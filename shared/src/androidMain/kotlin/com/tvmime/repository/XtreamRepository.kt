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

    val activePortal: Flow<PortalEntity?> = database.portalDao().getActivePortal()
    val allPortals: Flow<List<PortalEntity>> = database.portalDao().getAllPortals()

    fun getCategories(portalId: String, type: StreamType = StreamType.LIVE): Flow<List<CategoryEntity>> {
        return database.categoryDao().getCategories(portalId, type.name)
    }

    fun getChannelsByCategory(portalId: String, categoryId: String): Flow<List<ChannelEntity>> {
        return database.channelDao().getChannelsByCategory(portalId, categoryId)
    }

    fun getAllChannelsByType(portalId: String, type: StreamType = StreamType.LIVE): Flow<List<ChannelEntity>> {
        return database.channelDao().getAllChannelsByType(portalId, type.name)
    }

    suspend fun getFirstChannel(portalId: String): ChannelEntity? {
        return database.channelDao().getFirstChannel(portalId)
    }

    fun getFavorites(portalId: String): Flow<List<ChannelEntity>> {
        return database.channelDao().getFavorites(portalId)
    }

    fun searchChannels(portalId: String, query: String): Flow<List<ChannelEntity>> {
        return database.channelDao().searchChannels(portalId, query)
    }

    fun getRecentlyWatched(portalId: String, limit: Int = 20): Flow<List<ChannelEntity>> {
        return database.channelDao().getRecentlyWatched(portalId, limit)
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
            database.portalDao().setActivePortal(entity.id)
        }
    }

    suspend fun setActivePortal(portalId: String) {
        database.portalDao().setActivePortal(portalId)
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
     * Executes zero-OOM sync of categories and channels directly into SQLite/Room.
     */
    suspend fun syncActivePortal(): Result<Unit> = withContext(Dispatchers.IO) {
        val portal = database.portalDao().getActivePortalSync()
            ?: return@withContext Result.failure(IllegalStateException("No active portal selected"))

        syncPortal(portal)
    }

    suspend fun syncPortal(portal: PortalEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val portalConfig = portal.toDomain()
        _syncProgress.value = SyncProgress.Authenticating(portal.name)

        // 1. Authenticate with provider
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
            database.portalDao().setActivePortal(activePortal.id)
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
                    lastSyncedAt = null
                )
                database.portalDao().insertOrUpdate(entity)
            }
            val activePortal = portals.firstOrNull { it.isActive } ?: portals.firstOrNull()
            if (activePortal != null) {
                database.portalDao().setActivePortal(activePortal.id)
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
                lastSyncedAt = null
            )
            database.portalDao().insertOrUpdate(entity)
            entities.add(entity)
        }
        val activePortal = portals.firstOrNull { it.isActive } ?: portals.firstOrNull()
        if (activePortal != null) {
            database.portalDao().setActivePortal(activePortal.id)
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
        database.portalDao().setActivePortal(demo.id)

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
