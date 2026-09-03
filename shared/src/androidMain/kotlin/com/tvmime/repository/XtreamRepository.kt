package com.tvmime.repository

import com.tvmime.db.AppDatabase
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
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
        for ((idx, portal) in cloudPortals.withIndex()) {
            val entity = PortalEntity.fromDomain(portal)
            database.portalDao().insertOrUpdate(entity)
            if (idx == 0) {
                database.portalDao().setActivePortal(entity.id)
            }
            entities.add(entity)
        }

        Result.success(entities)
    }

    suspend fun registerPairingCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firebase = com.tvmime.sync.FirebaseSyncClient()
        firebase.registerTvPairingCode(code, System.currentTimeMillis())
    }

    suspend fun checkPairingAndSync(code: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val firebase = com.tvmime.sync.FirebaseSyncClient()
        val statusRes = firebase.checkTvPairingStatus(code)
        val status = statusRes.getOrElse { return@withContext Result.failure(it) }

        if (status.isAuthorized && !status.userId.isNullOrBlank()) {
            val portalsRes = firebase.getUserPortals(status.userId)
            val portals = portalsRes.getOrElse { return@withContext Result.failure(it) }
            for ((idx, cfg) in portals.withIndex()) {
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
                    lastSyncedAt = System.currentTimeMillis()
                )
                database.portalDao().insertOrUpdate(entity)
                if (idx == 0) {
                    database.portalDao().setActivePortal(entity.id)
                }
            }
            Result.success(true)
        } else {
            Result.success(false)
        }
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
        demo
    }
}
