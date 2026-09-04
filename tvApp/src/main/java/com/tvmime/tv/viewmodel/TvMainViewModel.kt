package com.tvmime.tv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvmime.db.AppDatabase
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.db.entity.PortalEntity
import com.tvmime.model.PortalConfig
import com.tvmime.model.StreamType
import com.tvmime.repository.SyncProgress
import com.tvmime.repository.XtreamRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.tvmime.db.entity.EpgProgramEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TvNavDestination(val label: String) {
    LIVE_TV("Live TV"),
    TV_GUIDE("TV Guide"),
    MOVIES("Movies"),
    SERIES("TV Series"),
    FAVORITES("Favorites"),
    CLOUD_SYNC("Cloud Sync"),
    SETTINGS("Settings & Updates"),
    ABOUT("About TVMime")
}

@OptIn(ExperimentalCoroutinesApi::class)
class TvMainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = XtreamRepository(database)
    val preferences = com.tvmime.tv.settings.TvPreferencesManager.getInstance(application)

    val showClockOverlay: StateFlow<Boolean> = preferences.showClockOverlay
    val autoHideOsdSeconds: StateFlow<Int> = preferences.autoHideOsdSeconds
    val enableLastChannelZap: StateFlow<Boolean> = preferences.enableLastChannelZap
    val defaultAspectMode: StateFlow<String> = preferences.defaultAspectMode

    fun setShowClockOverlay(enabled: Boolean) {
        preferences.setShowClockOverlay(enabled)
    }

    fun setAutoHideOsdSeconds(sec: Int) {
        preferences.setAutoHideOsdSeconds(sec)
    }

    fun setEnableLastChannelZap(enabled: Boolean) {
        preferences.setEnableLastChannelZap(enabled)
    }

    fun setDefaultAspectMode(mode: String) {
        preferences.setDefaultAspectMode(mode)
    }

    // Active Portals & Sync Status
    val activePortals: StateFlow<List<PortalEntity>> = repository.activePortals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPortals: StateFlow<List<PortalEntity>> = repository.allPortals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncProgress: StateFlow<SyncProgress> = repository.syncProgress

    val epgPrograms: StateFlow<List<EpgProgramEntity>> = activePortals.flatMapLatest { portals ->
        if (portals.isEmpty()) flowOf(emptyList())
        else {
            // Combine flows for all active portals
            val flows = portals.map { portal ->
                repository.getEpgProgramsInWindow(
                    portal.id,
                    System.currentTimeMillis() - 2 * 3600 * 1000L,
                    System.currentTimeMillis() + 6 * 3600 * 1000L
                )
            }
            combine(flows) { arrays -> arrays.flatMap { it.toList() } }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation & Screen State
    private val _currentDestination = MutableStateFlow(TvNavDestination.LIVE_TV)
    val currentDestination: StateFlow<TvNavDestination> = _currentDestination.asStateFlow()

    private val _overlayState = MutableStateFlow(TvOverlayState.HIDDEN)
    val overlayState: StateFlow<TvOverlayState> = _overlayState.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()


    // Selection & Playback State
    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory: StateFlow<CategoryEntity?> = _selectedCategory.asStateFlow()

    private val _selectedChannel = MutableStateFlow<ChannelEntity?>(null)
    val selectedChannel: StateFlow<ChannelEntity?> = _selectedChannel.asStateFlow()

    private val _playingChannel = MutableStateFlow<ChannelEntity?>(null)
    val playingChannel: StateFlow<ChannelEntity?> = _playingChannel.asStateFlow()

    private val _previousChannel = MutableStateFlow<ChannelEntity?>(null)
    val previousChannel: StateFlow<ChannelEntity?> = _previousChannel.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    // Categories Flow for active portals and destination
    val categories: StateFlow<List<CategoryEntity>> = combine(activePortals, currentDestination, preferences.hiddenCategories) { portals, dest, hidden ->
        Triple(portals, dest, hidden)
    }.flatMapLatest { (portals, dest, hidden) ->
        if (portals.isEmpty()) {
            flowOf(emptyList())
        } else {
            val type = when (dest) {
                TvNavDestination.LIVE_TV, TvNavDestination.FAVORITES -> StreamType.LIVE
                TvNavDestination.MOVIES -> StreamType.MOVIE
                TvNavDestination.SERIES -> StreamType.SERIES
                else -> StreamType.LIVE
            }
            val portalIds = portals.map { it.id }
            repository.getCategories(portalIds, type).map { list -> list.filter { !hidden.contains(it.categoryId) } }
        }
    }.onEach { catList ->
        if (_selectedCategory.value == null || catList.none { it.categoryId == _selectedCategory.value?.categoryId }) {
            _selectedCategory.value = catList.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<ChannelEntity>> = combine(
        activePortals,
        currentDestination,
        selectedCategory,
        searchQuery
    ) { portals, dest, category, query ->
        Quadruple(portals, dest, category, query)
    }.flatMapLatest { (portals, dest, category, query) ->
        if (portals.isEmpty()) return@flatMapLatest flowOf(emptyList())

        val portalIds = portals.map { it.id }

        if (query.isNotBlank()) {
            return@flatMapLatest repository.searchChannels(portalIds, query)
        }

        when (dest) {
            TvNavDestination.FAVORITES -> repository.getFavorites(portalIds)
            else -> {
                if (category != null) {
                    repository.getChannelsByCategory(portalIds, category.categoryId)
                } else {
                    val type = when (dest) {
                        TvNavDestination.MOVIES -> StreamType.MOVIE
                        TvNavDestination.SERIES -> StreamType.SERIES
                        else -> StreamType.LIVE
                    }
                    repository.getAllChannelsByType(portalIds, type)
                }
            }
        }
    }.onEach { chList ->
        if (_playingChannel.value == null && chList.isNotEmpty()) {
            _playingChannel.value = chList.firstOrNull()
            _selectedChannel.value = chList.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun autoPlayFirstChannelIfIdle() {
        if (_playingChannel.value == null) {
            val portals = activePortals.value
            if (portals.isEmpty()) return
            val portalIds = portals.map { it.id }
            val firstCh = repository.getFirstChannel(portalIds)
            if (firstCh != null) {
                _playingChannel.value = firstCh
                _selectedChannel.value = firstCh
            }
        }
    }

    fun navigateTo(dest: TvNavDestination) {
        _currentDestination.value = dest
        _searchQuery.value = ""
        _selectedCategory.value = null
    }

    fun setOverlayState(state: TvOverlayState) {
        _overlayState.value = state
    }

    fun selectCategory(cat: CategoryEntity?) {
        _selectedCategory.value = cat
    }

    fun selectChannel(ch: ChannelEntity) {
        _selectedChannel.value = ch
    }

    fun playChannel(ch: ChannelEntity) {
        val current = _playingChannel.value
        if (current != null && current.id != ch.id) {
            _previousChannel.value = current
        }
        _playingChannel.value = ch
        _selectedChannel.value = ch
        _playerError.value = null
        viewModelScope.launch {
            repository.recordWatch(ch.id)
        }
    }

    fun zapNext() {
        val currentChannels = channels.value
        if (currentChannels.isEmpty()) return
        val currentChannel = _playingChannel.value
        if (currentChannel == null) {
            playChannel(currentChannels.first())
            return
        }
        val currentIndex = currentChannels.indexOfFirst { it.id == currentChannel.id }
        if (currentIndex != -1 && currentIndex < currentChannels.size - 1) {
            playChannel(currentChannels[currentIndex + 1])
        } else {
            playChannel(currentChannels.first())
        }
    }

    fun zapPrevious() {
        val currentChannels = channels.value
        if (currentChannels.isEmpty()) return
        val currentChannel = _playingChannel.value
        if (currentChannel == null) {
            playChannel(currentChannels.last())
            return
        }
        val currentIndex = currentChannels.indexOfFirst { it.id == currentChannel.id }
        if (currentIndex > 0) {
            playChannel(currentChannels[currentIndex - 1])
        } else {
            playChannel(currentChannels.last())
        }
    }

    fun toggleLastChannel(): Boolean {
        val prev = _previousChannel.value
        val current = _playingChannel.value
        if (prev != null && prev.id != current?.id) {
            playChannel(prev)
            return true
        }
        return false
    }

    fun toggleCategoryVisibility(category: CategoryEntity, hide: Boolean) {
        preferences.toggleCategoryVisibility(category.categoryId, hide)
    }

    fun toggleFavorite(ch: ChannelEntity) {
        viewModelScope.launch {
            repository.setFavorite(ch.id, !ch.isFavorite)
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun setPlayerError(msg: String?) {
        _playerError.value = msg
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun syncCurrentPortal() {
        viewModelScope.launch {
            repository.syncActivePortals()
            autoPlayFirstChannelIfIdle()
        }
    }

    fun toggleActivePortal(portalId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.setPortalActiveStatus(portalId, isActive)
            if (isActive) {
                repository.syncActivePortals()
                autoPlayFirstChannelIfIdle()
            }
        }
    }

    fun refreshPortalsFromCloud() {
        viewModelScope.launch {
            val userId = preferences.cloudUserId.value ?: "r0b3lfflA9RbQaGZJFCE4mdTspy1"
            val res = repository.syncPortalsByUserId(userId)
            if (res.isSuccess) {
                repository.syncActivePortals()
                autoPlayFirstChannelIfIdle()
            }
        }
    }

    suspend fun syncFromCloud(email: String, pass: String): Result<Unit> {
        val authResult = repository.syncFromCloud(email, pass)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Authentication failed"))
        }
        val syncResult = repository.syncActivePortals()
        autoPlayFirstChannelIfIdle()
        if (syncResult.isFailure) {
            return Result.failure(syncResult.exceptionOrNull() ?: Exception("Sync failed"))
        }
        return Result.success(Unit)
    }

    fun addDemoPortal() {
        viewModelScope.launch {
            repository.addDemoPortal()
            autoPlayFirstChannelIfIdle()
        }
    }

    suspend fun registerPairingCode(code: String): Result<Unit> {
        return repository.registerPairingCode(code)
    }

    suspend fun checkPairingAndSync(code: String): Result<Boolean> {
        val res = repository.checkPairingAndSync(code)
        if (res.isFailure) return Result.failure(res.exceptionOrNull() ?: Exception("Pairing check failed"))
        val pair = res.getOrNull()
        if (pair?.first == true) {
            if (!pair.second.isNullOrBlank()) {
                preferences.setCloudUser(pair.second, null)
            }
            repository.syncActivePortals()
            return Result.success(true)
        }
        return Result.success(false)
    }

    suspend fun reportStreamIssue(
        channelName: String,
        channelNum: Int?,
        errorCode: String,
        errorMessage: String,
        deviceSpecs: String
    ): Result<Unit> {
        return repository.reportStreamIssue(channelName, channelNum, errorCode, errorMessage, deviceSpecs)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
