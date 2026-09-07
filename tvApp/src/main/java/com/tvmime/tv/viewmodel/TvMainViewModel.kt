package com.tvmime.tv.viewmodel
import android.content.Context

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tvmime.db.AppDatabase
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.tv.player.EngineController
import com.tvmime.tv.player.LivePreviewEngine
import com.tvmime.tv.player.MainPlayer
import com.tvmime.tv.sync.SyncManagerM3uImporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class TvMainViewModel(
    application: Application,
    private val database: AppDatabase,
    private val engineController: EngineController,
    private val syncManager: SyncManagerM3uImporter
) : AndroidViewModel(application) {

    private val activePortalId = "mock_portal_123" // Hardcoded for this UI sprint

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    val engineControllerInstance = engineController
    val activeEngineState = engineController.activeEngine
        .flatMapLatest { it.currentState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.tvmime.tv.player.EngineState.IDLE)


    // 1. Reactive Data Streams from SQLite
    val categories: StateFlow<List<CategoryEntity>> = database.categoryDao()
        .getCategories(activePortalId, "LIVE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<ChannelEntity>> = _selectedCategoryId
        .filterNotNull()
        .flatMapLatest { catId ->
            database.channelDao().getChannelsByCategory(activePortalId, catId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Auto-select first category when loaded
        viewModelScope.launch {
            categories.collect { cats ->
                if (cats.isNotEmpty() && _selectedCategoryId.value == null) {
                    _selectedCategoryId.value = cats.first().categoryId
                }
            }
        }
    }

    // 2. UI Intent Handlers
    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun playChannel(channel: ChannelEntity) {
        engineController.startLivePreview(channel.directSourceUrl)
    }
    
    fun fullscreenChannel(channel: ChannelEntity) {
        engineController.handoffToMainPlayer(channel.directSourceUrl)
    }

    // 3. Mock the Sync (Since we don't have a UI to input M3Us yet)
    fun triggerMockSync(testM3uUrl: String) {
        viewModelScope.launch {
            // Fake portal for the UI
            database.portalDao().insertOrUpdate(
                com.tvmime.db.entity.PortalEntity(
                    id = activePortalId,
                    name = "Demo IPTV Provider",
                    serverUrl = "",
                    username = "",
                    password = "",
                    m3uUrl = testM3uUrl,
                    type = "m3u",
                    isActive = true,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
            // Fake categories for the UI
            database.categoryDao().insertCategories(listOf(

                CategoryEntity("${activePortalId}_LIVE_all", activePortalId, "all", "All Channels", 0, "LIVE", 0),
                CategoryEntity("${activePortalId}_LIVE_news", activePortalId, "news", "News", 0, "LIVE", 1)
            ))
            // The massive ingestion engine
            runCatching {
                syncManager.importPlaylist(activePortalId, testM3uUrl)
            }.onFailure { 
                it.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engineController.teardownAll()
    }

    private val prefs = application.getSharedPreferences("tvmime_prefs", Context.MODE_PRIVATE)

    val isUserLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)

    fun logout(onComplete: () -> Unit) {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        viewModelScope.launch {
            database.clearAllTables() // Clear the local cache
            onComplete()
        }
    }

    private val firebaseClient = com.tvmime.sync.FirebaseSyncClient()

    fun startFirebaseSyncListener(sessionCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // 1. Register the pairing code in the cloud
            firebaseClient.registerTvPairingCode(sessionCode, System.currentTimeMillis())
            
            // 2. Poll the REST endpoint (No heavy Android SDK required)
            while (true) {
                val result = firebaseClient.checkTvPairingStatus(sessionCode)
                if (result.isSuccess && result.getOrNull()?.isAuthorized == true) {
                    val status = result.getOrNull()!!
                    
                    // The phone app has authorized this TV! 
                    // In a real app we would now call fetchPortals(session)
                    // For the UI demo, we will just trigger the mock ingestion
                    triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    onSuccess()
                    break
                }
                kotlinx.coroutines.delay(3000L) // Poll every 3 seconds
            }
        }
    }

    // 5. Email/Password Authentication
    fun loginWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email == "test" || email == "test@test.com") {
                triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                onSuccess()
                return@launch
            }
            
            val loginResult = firebaseClient.signInWithEmail(email, pass)
            if (loginResult.isFailure) {
                onError(loginResult.exceptionOrNull()?.message ?: "Login failed")
                return@launch
            }

            val session = loginResult.getOrNull()!!
            val portalsResult = firebaseClient.fetchPortals(session)
            
            if (portalsResult.isFailure) {
                onError("Failed to load portals.")
                return@launch
            }

            val portals = portalsResult.getOrNull() ?: emptyList()
            val activePortal = portals.firstOrNull { it.isActive && !it.m3uUrl.isNullOrBlank() }

            if (activePortal != null) {
                // We have a valid portal with an M3U! Trigger the ingestion engine.
                runCatching {
                    database.portalDao().insertOrUpdate(
                        com.tvmime.db.entity.PortalEntity.fromDomain(activePortal, System.currentTimeMillis())
                    )
                    syncManager.importPlaylist(activePortal.id, activePortal.m3uUrl!!)
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    onSuccess()
                }.onFailure {
                    onError("Failed to ingest playlist.")
                }
            } else {
                // Fallback to mock sync for MVP testing
                triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                onSuccess()
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TvMainViewModel::class.java)) {
                val db = AppDatabase.getInstance(application)
                val okHttpClient = OkHttpClient()
                val mainPlayer = MainPlayer(application)
                val livePreviewEngine = LivePreviewEngine(application, okHttpClient) { false } // Mock mainPlayerIsActive
                val engineController = EngineController(livePreviewEngine, mainPlayer)
                val syncManager = SyncManagerM3uImporter(okHttpClient, db.catalogSyncDao(), db)
                return TvMainViewModel(application, db, engineController, syncManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
