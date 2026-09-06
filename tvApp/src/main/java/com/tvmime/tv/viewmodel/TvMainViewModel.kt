package com.tvmime.tv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvmime.db.AppDatabase
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.tv.player.EngineController
import com.tvmime.tv.sync.SyncManagerM3uImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvMainViewModel @Inject constructor(
    private val database: AppDatabase,
    private val engineController: EngineController,
    private val syncManager: SyncManagerM3uImporter,
    private val firebaseSyncManager: com.tvmime.tv.sync.FirebaseSyncManager
) : ViewModel() {

    private val activePortalId = "mock_portal_123" // Hardcoded for this UI sprint

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

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
            // Fake categories for the UI
            database.categoryDao().insertCategories(listOf(
                CategoryEntity("${activePortalId}_LIVE_all", activePortalId, "all", "All Channels", "LIVE", 0),
                CategoryEntity("${activePortalId}_LIVE_news", activePortalId, "news", "News", "LIVE", 1)
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

    // 4. Firebase Cloud Sync Listener
    fun startFirebaseSyncListener(sessionCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            firebaseSyncManager.listenForCredentials(sessionCode)
                .catch { e -> e.printStackTrace() }
                .collect { credentials ->
                    // 1. Credentials received from the Cloud!
                    // 2. Trigger the StreamVault Mass Ingestion Engine
                    // (If it was Xtream, we would construct the API URL here. For this demo, we assume raw M3U)
                    triggerMockSync(credentials.portalUrl)
                    
                    // 3. Tell the UI to navigate to Live TV
                    onSuccess()
                }
        }
    }
}
