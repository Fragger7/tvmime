package com.tvmime.repository

import com.tvmime.db.AppDatabase
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.db.entity.EpgProgramEntity
import com.tvmime.db.entity.PortalEntity
import com.tvmime.model.Category
import com.tvmime.model.Channel
import com.tvmime.model.PortalConfig
import com.tvmime.model.StreamType
import com.tvmime.network.AuthResult
import com.tvmime.network.XtreamClient
import com.tvmime.parser.StreamingCatalogParser
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface SyncProgress {
    object Idle : SyncProgress
    data class Authenticating(val portalName: String) : SyncProgress
    data class SyncingCategories(val type: StreamType) : SyncProgress
    data class SyncingChannels(val type: StreamType, val count: Int) : SyncProgress
    data class Success(val lastSyncedAt: Long) : SyncProgress
    data class Error(val message: String) : SyncProgress
}

class XtreamRepository(
    private val database: AppDatabase,
    private val xtreamClient: XtreamClient = XtreamClient()
) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    val activePortals: Flow<List<PortalEntity>> = database.portalDao().getActivePortals()
    val allPortals: Flow<List<PortalEntity>> = database.portalDao().getAllPortals()

    fun getCategories(portalIds: List<String>, type: StreamType = StreamType.LIVE): Flow<List<CategoryEntity>> {
        return database.categoryDao().getCategoriesForPortals(portalIds, type.name)
    }

    fun getChannelsByCategory(portalIds: List<String>, categoryId: String): Flow<List<ChannelEntity>> {
        return database.channelDao().getChannelsByCategoryForPortals(portalIds, categoryId)
    }

    fun getAllChannelsByType(portalIds: List<String>, type: StreamType = StreamType.LIVE): Flow<List<ChannelEntity>> {
        return database.channelDao().getAllChannelsByTypeForPortals(portalIds, type.name)
    }

    suspend fun getFirstChannel(portalIds: List<String>): ChannelEntity? {
        if (portalIds.isEmpty()) return null
        return database.channelDao().getFirstChannelForPortals(portalIds)
    }

    fun getFavorites(portalIds: List<String>): Flow<List<ChannelEntity>> {
        return database.channelDao().getFavoritesForPortals(portalIds)
    }

    fun searchChannels(portalIds: List<String>, query: String): Flow<List<ChannelEntity>> {
        return database.channelDao().searchChannelsForPortals(portalIds, query)
    }

    fun getRecentlyWatched(portalIds: List<String>, limit: Int = 20): Flow<List<ChannelEntity>> {
        return database.channelDao().getRecentlyWatchedForPortals(portalIds, limit)
    }

    suspend fun setFavorite(channelId: String, isFavorite: Boolean) {
        database.channelDao().setFavorite(channelId, isFavorite)
    }

    suspend fun recordWatch(channelId: String) {
        database.channelDao().updateLastWatched(channelId, System.currentTimeMillis())
    }

    suspend fun savePortal(portal: PortalConfig, setActive: Boolean = true) {
        val entity = PortalEntity.fromDomain(portal)
        database.portalDao().insertOrUpdate(entity)
        if (setActive) {
            database.portalDao().setPortalActiveStatus(entity.id, true)
        }
    }

    suspend fun setPortalActiveStatus(portalId: String, isActive: Boolean) {
        database.portalDao().setPortalActiveStatus(portalId, isActive)
    }

    suspend fun deletePortal(portalId: String) {
        val portal = database.portalDao().getPortalById(portalId)
        if (portal != null) {
            database.portalDao().deletePortal(portal)
            database.categoryDao().deleteAllForPortal(portalId)
            database.channelDao().deleteAllForPortal(portalId)
            database.epgDao().deleteAllForPortal(portalId)
        }
    }

    /**
     * Executes zero-OOM sync of categories and channels directly into SQLite/Room for all active portals.
     */
    suspend fun syncActivePortals(): Result<Unit> = withContext(Dispatchers.IO) {
        val portals = database.portalDao().getActivePortalsSync()
        if (portals.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No active portals selected"))
        }

        var lastException: Exception? = null
        for (portal in portals) {
            val res = syncPortal(portal)
            if (res.isFailure) {
                lastException = res.exceptionOrNull() as? Exception
            }
        }
        
        if (lastException != null && portals.size == 1) {
            return@withContext Result.failure(lastException)
        }
        Result.success(Unit)
    }

    suspend fun syncPortal(portal: PortalEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val portalConfig = portal.toDomain()
        _syncProgress.value = SyncProgress.Authenticating(portal.name)

        if (portalConfig.type == "m3u" && !portalConfig.m3uUrl.isNullOrBlank()) {
            // M3U Fallback Pipeline
            try {
                _syncProgress.value = SyncProgress.SyncingChannels(StreamType.LIVE, 0)
                syncM3uPlaylist(portal)
                val timestamp = System.currentTimeMillis()
                database.portalDao().updateLastSynced(portal.id, timestamp)
                _syncProgress.value = SyncProgress.Success(timestamp)
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                Napier.e("M3U Sync failed for portal ${portal.name}", e)
                _syncProgress.value = SyncProgress.Error(e.message ?: "M3U Sync failed")
                return@withContext Result.failure(e)
            }
        }

        // 1. Authenticate with provider (Xtream Codes)
        val auth = xtreamClient.authenticate(portalConfig)
        if (auth is AuthResult.Error) {
            val err = "Authentication failed: ${auth.message}"
            _syncProgress.value = SyncProgress.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        try {
            // Types to sync
            val typesToSync = mutableListOf<StreamType>()
            if (portal.syncLive) typesToSync.add(StreamType.LIVE)
            if (portal.syncMovies) typesToSync.add(StreamType.MOVIE)
            if (portal.syncSeries) typesToSync.add(StreamType.SERIES)

            for (type in typesToSync) {
                // 2. Sync Categories
                _syncProgress.value = SyncProgress.SyncingCategories(type)
                syncCategories(portal, type)

                // 3. Sync Channels (Zero-OOM streaming)
                syncChannels(portal, type)
            }

            // 4. Sync EPG if Live TV was synced
            if (portal.syncLive) {
                syncEpg(portal)
            }

            val timestamp = System.currentTimeMillis()
            database.portalDao().updateLastSynced(portal.id, timestamp)
            _syncProgress.value = SyncProgress.Success(timestamp)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Sync failed for portal ${portal.name}", e)
            _syncProgress.value = SyncProgress.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }

    private suspend fun syncCategories(portal: PortalEntity, type: StreamType) {
        val url = xtreamClient.buildCategoriesApiUrl(portal.toDomain(), type)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", XtreamClient.EVASION_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch categories: HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty categories response")

            val categories = mutableListOf<CategoryEntity>()
            var sortIndex = 0
            StreamingCatalogParser.parseCategoriesStream(body.byteStream(), type).collect { cat ->
                categories.add(CategoryEntity.fromDomain(cat, portal.id, sortIndex++))
                if (categories.size >= 200) {
                    database.categoryDao().insertCategories(categories)
                    categories.clear()
                }
            }
            if (categories.isNotEmpty()) {
                database.categoryDao().insertCategories(categories)
            }
        }
    }

    private suspend fun syncChannels(portal: PortalEntity, type: StreamType) {
        val url = xtreamClient.buildCatalogApiUrl(portal.toDomain(), type)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", XtreamClient.EVASION_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch $type streams: HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty $type streams response")

            val batch = mutableListOf<ChannelEntity>()
            var totalProcessed = 0

            StreamingCatalogParser.parseChannelsStream(
                inputStream = body.byteStream(),
                portalServerUrl = portal.serverUrl,
                portalUser = portal.username,
                portalPass = portal.password,
                streamType = type
            ).collect { channel ->
                batch.add(ChannelEntity.fromDomain(channel, portal.id))
                totalProcessed++

                if (batch.size >= 500) {
                    database.channelDao().insertChannelsBatch(batch)
                    batch.clear()
                    _syncProgress.value = SyncProgress.SyncingChannels(type, totalProcessed)
                }
            }

            if (batch.isNotEmpty()) {
                database.channelDao().insertChannelsBatch(batch)
                _syncProgress.value = SyncProgress.SyncingChannels(type, totalProcessed)
            }
        }
    }

    private suspend fun syncEpg(portal: PortalEntity) {
        // Only fetch get_short_epg for the whole server if streamId is not specified
        // Some servers support get_short_epg without stream_id to dump everything.
        val url = "${portal.serverUrl.removeSuffix("/")}/player_api.php?username=${portal.username}&password=${portal.password}&action=get_short_epg"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", XtreamClient.EVASION_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Napier.w("EPG sync returned HTTP ${response.code} for portal ${portal.name}. Skipping EPG.")
                    return
                }
                val body = response.body ?: return

                val batch = mutableListOf<EpgProgramEntity>()
                StreamingCatalogParser.parseEpgStream(
                    inputStream = body.byteStream(),
                    portalId = portal.id,
                    timeShiftHours = portal.timeShiftHours
                ).collect { prog ->
                    batch.add(EpgProgramEntity.fromDomain(prog, portal.id))

                    if (batch.size >= 1000) {
                        database.epgDao().insertProgramsBatch(batch)
                        batch.clear()
                    }
                }

                if (batch.isNotEmpty()) {
                    database.epgDao().insertProgramsBatch(batch)
                }
            }
        } catch (e: Exception) {
            Napier.w("EPG sync failed for portal ${portal.name}", e)
        }
    }

    private suspend fun syncM3uPlaylist(portal: PortalEntity) {
        val url = portal.m3uUrl ?: return
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", XtreamClient.EVASION_USER_AGENT)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch M3U: HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty M3U response")

            val categoriesMap = mutableMapOf<String, CategoryEntity>()
            val channelsBatch = mutableListOf<ChannelEntity>()
            var totalProcessed = 0
            var sortIndex = 0

            com.tvmime.parser.M3uParser.parseM3uStream(body.byteStream()).collect { channel ->
                val categoryName = channel.categoryId.ifBlank { "Uncategorized" }
                val catId = "${portal.id}_m3u_${categoryName.hashCode()}"
                
                if (!categoriesMap.containsKey(catId)) {
                    val catEntity = CategoryEntity(
                        id = catId,
                        portalId = portal.id,
                        categoryId = catId,
                        categoryName = categoryName,
                        parentId = 0,
                        type = channel.type.name,
                        sortOrder = sortIndex++
                    )
                    categoriesMap[catId] = catEntity
                }

                // Associate channel with generated Category ID
                val finalChannel = channel.copy(categoryId = catId)
                channelsBatch.add(ChannelEntity.fromDomain(finalChannel, portal.id))
                totalProcessed++

                if (channelsBatch.size >= 500) {
                    database.categoryDao().insertCategories(categoriesMap.values.toList())
                    database.channelDao().insertChannelsBatch(channelsBatch)
                    channelsBatch.clear()
                    _syncProgress.value = SyncProgress.SyncingChannels(StreamType.LIVE, totalProcessed)
                }
            }

            if (categoriesMap.isNotEmpty()) {
                database.categoryDao().insertCategories(categoriesMap.values.toList())
            }
            if (channelsBatch.isNotEmpty()) {
                database.channelDao().insertChannelsBatch(channelsBatch)
                _syncProgress.value = SyncProgress.SyncingChannels(StreamType.LIVE, totalProcessed)
            }
        }
    }

    /**
     * Authenticates with Firebase Cloud and pulls all user portals directly into Room DB.
     */
    suspend fun syncFromCloud(email: String, pass: String): Result<List<PortalEntity>> = withContext(Dispatchers.IO) {
        val firebase = com.tvmime.sync.FirebaseSyncClient()
        val sessionResult = firebase.signInWithEmail(email, pass)
        val session = sessionResult.getOrElse { return@withContext Result.failure(it) }

        val portalsResult = firebase.fetchPortals(session)
        val cloudPortals = portalsResult.getOrElse { return@withContext Result.failure(it) }

        val entities = mutableListOf<PortalEntity>()
        for (portal in cloudPortals) {
            val entity = PortalEntity.fromDomain(portal)
            database.portalDao().insertOrUpdate(entity)
            entities.add(entity)
        }

        val activePortal = cloudPortals.firstOrNull { it.isActive } ?: cloudPortals.firstOrNull()
        if (activePortal != null) {
            database.portalDao().setPortalActiveStatus(activePortal.id, true)
        }

        Result.success(entities)
    }

    suspend fun registerPairingCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firebase = com.tvmime.sync.FirebaseSyncClient()
        firebase.registerTvPairingCode(code, System.currentTimeMillis())
    }

    suspend fun checkPairingAndSync(code: String): Result<Pair<Boolean, String?>> = withContext(Dispatchers.IO) {
        val firebase = com.tvmime.sync.FirebaseSyncClient()
        val statusRes = firebase.checkTvPairingStatus(code)
        if (statusRes.isFailure) return@withContext Result.failure(statusRes.exceptionOrNull() ?: Exception("Pairing check failed"))
        val status = statusRes.getOrNull() ?: return@withContext Result.success(Pair(false, null))

        if (status.isAuthorized && !status.userId.isNullOrBlank()) {
            val portalsRes = firebase.fetchPortalsByUserId(status.userId)
            if (portalsRes.isFailure) return@withContext Result.failure(portalsRes.exceptionOrNull() ?: Exception("Failed to fetch portals"))
            val portals = portalsRes.getOrNull() ?: emptyList()
            for (cfg in portals) {
                val entity = PortalEntity(
                    id = cfg.id,
                    name = cfg.name,
                    serverUrl = cfg.serverUrl,
                    username = cfg.username,
                    password = cfg.password,
                    isActive = cfg.isActive,
                    syncLive = cfg.syncLive,
                    syncMovies = cfg.syncMovies,
                    syncSeries = cfg.syncSeries,
                    lastSyncedAt = 0L
                )
                database.portalDao().insertOrUpdate(entity)
            }
            val activePortal = portals.firstOrNull { it.isActive } ?: portals.firstOrNull()
            if (activePortal != null) {
                database.portalDao().setPortalActiveStatus(activePortal.id, true)
            }
            Result.success(Pair(true, status.userId))
        } else {
            Result.success(Pair(false, null))
        }
    }

    suspend fun syncPortalsByUserId(userId: String): Result<List<PortalEntity>> = withContext(Dispatchers.IO) {
        val firebase = com.tvmime.sync.FirebaseSyncClient()
        val portalsRes = firebase.fetchPortalsByUserId(userId)
        if (portalsRes.isFailure) return@withContext Result.failure(portalsRes.exceptionOrNull() ?: Exception("Failed to fetch portals"))
        val portals = portalsRes.getOrNull() ?: emptyList()
        val entities = mutableListOf<PortalEntity>()
        for (cfg in portals) {
            val entity = PortalEntity(
                id = cfg.id,
                name = cfg.name,
                serverUrl = cfg.serverUrl,
                username = cfg.username,
                password = cfg.password,
                isActive = cfg.isActive,
                syncLive = cfg.syncLive,
                syncMovies = cfg.syncMovies,
                syncSeries = cfg.syncSeries,
                lastSyncedAt = 0L
            )
            database.portalDao().insertOrUpdate(entity)
            entities.add(entity)
        }
        val activePortal = portals.firstOrNull { it.isActive } ?: portals.firstOrNull()
        if (activePortal != null) {
            database.portalDao().setPortalActiveStatus(activePortal.id, true)
        }
        Result.success(entities)
    }

    suspend fun reportStreamIssue(
        channelName: String,
        channelNum: Int?,
        errorCode: String,
        errorMessage: String,
        deviceSpecs: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val firebase = com.tvmime.sync.FirebaseSyncClient()
        firebase.reportStreamIssue(
            channelName = channelName,
            channelNum = channelNum,
            errorCode = errorCode,
            errorMessage = errorMessage,
            deviceSpecs = deviceSpecs,
            timestamp = System.currentTimeMillis()
        )
    }

    fun getEpgProgramsInWindow(portalId: String, windowStart: Long, windowEnd: Long): Flow<List<EpgProgramEntity>> =
        database.epgDao().getProgramsInWindow(portalId, windowStart, windowEnd)

    suspend fun addDemoPortal(): PortalEntity = withContext(Dispatchers.IO) {
        val demo = PortalEntity(
            id = "demo_portal",
            name = "TVMime Public Demo",
            serverUrl = "http://demo.tvmime.local:8080",
            username = "demo",
            password = "demo",
            isActive = true,
            lastSyncedAt = System.currentTimeMillis()
        )
        database.portalDao().insertOrUpdate(demo)
        database.portalDao().setPortalActiveStatus(demo.id, true)

        val demoCat = com.tvmime.db.entity.CategoryEntity(
            id = "demo_portal_LIVE_demo_cat",
            portalId = demo.id,
            categoryId = "demo_cat",
            categoryName = "Demo Showcase",
            parentId = 0,
            type = "LIVE",
            sortOrder = 1
        )
        database.categoryDao().insertCategories(listOf(demoCat))

        val demoChannels = listOf(
            ChannelEntity(
                id = "demo_portal_LIVE_1",
                portalId = demo.id,
                streamId = 1,
                num = 1,
                name = "Big Buck Bunny (HLS 60fps)",
                type = "LIVE",
                epgChannelId = "demo_epg_1",
                categoryId = "demo_cat",
                containerExtension = "m3u8",
                directSourceUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            ),
            ChannelEntity(
                id = "demo_portal_LIVE_2",
                portalId = demo.id,
                streamId = 2,
                num = 2,
                name = "Sintel (HLS)",
                type = "LIVE",
                epgChannelId = "demo_epg_2",
                categoryId = "demo_cat",
                containerExtension = "m3u8",
                directSourceUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
            ),
            ChannelEntity(
                id = "demo_portal_LIVE_3",
                portalId = demo.id,
                streamId = 3,
                num = 3,
                name = "Tears of Steel (4K HLS)",
                type = "LIVE",
                epgChannelId = "demo_epg_3",
                categoryId = "demo_cat",
                containerExtension = "m3u8",
                directSourceUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
            )
        )
        database.channelDao().insertChannelsBatch(demoChannels)

        val now = System.currentTimeMillis()
        val demoPrograms = listOf(
            EpgProgramEntity(
                id = "${demo.id}_demo_epg_1_p1",
                portalId = demo.id,
                epgChannelId = "demo_epg_1",
                title = "Big Buck Bunny: The 60fps Cut",
                description = "Open-source animated short film following a giant bunny fighting forest bullies in 4K 60fps.",
                startEpoch = now - 45 * 60 * 1000L,
                endEpoch = now + 45 * 60 * 1000L
            ),
            EpgProgramEntity(
                id = "${demo.id}_demo_epg_1_p2",
                portalId = demo.id,
                epgChannelId = "demo_epg_1",
                title = "Blender Open VFX Showcase",
                description = "Deep dive into 3D rendering, lighting, and spatial audio pipelines.",
                startEpoch = now + 45 * 60 * 1000L,
                endEpoch = now + 165 * 60 * 1000L
            ),
            EpgProgramEntity(
                id = "${demo.id}_demo_epg_2_p1",
                portalId = demo.id,
                epgChannelId = "demo_epg_2",
                title = "Sintel: Quest for the Dragon",
                description = "A lonely young woman searches for a wounded baby dragon she once nursed back to health.",
                startEpoch = now - 30 * 60 * 1000L,
                endEpoch = now + 60 * 60 * 1000L
            ),
            EpgProgramEntity(
                id = "${demo.id}_demo_epg_2_p2",
                portalId = demo.id,
                epgChannelId = "demo_epg_2",
                title = "Cosmos Laundromat (First Cycle)",
                description = "On a desolate island, a suicidal sheep named Franck is visited by a mysterious salesman.",
                startEpoch = now + 60 * 60 * 1000L,
                endEpoch = now + 180 * 60 * 1000L
            ),
            EpgProgramEntity(
                id = "${demo.id}_demo_epg_3_p1",
                portalId = demo.id,
                epgChannelId = "demo_epg_3",
                title = "Tears of Steel: Sci-Fi Premiere",
                description = "A dystopian future in Amsterdam where scientists battle against robotic invaders in Dolby Surround.",
                startEpoch = now - 60 * 60 * 1000L,
                endEpoch = now + 30 * 60 * 1000L
            ),
            EpgProgramEntity(
                id = "${demo.id}_demo_epg_3_p2",
                portalId = demo.id,
                epgChannelId = "demo_epg_3",
                title = "Caminandes: Llama Chronicles",
                description = "Hilarious animated adventures of Koro the llama attempting to cross a desert highway.",
                startEpoch = now + 30 * 60 * 1000L,
                endEpoch = now + 150 * 60 * 1000L
            )
        )
        database.epgDao().insertProgramsBatch(demoPrograms)

        demo
    }
}
