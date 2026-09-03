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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TvNavDestination(val label: String) {
    LIVE_TV("Live TV"),
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

    // Active Portal & Sync Status
    val activePortal: StateFlow<PortalEntity?> = repository.activePortal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val syncProgress: StateFlow<SyncProgress> = repository.syncProgress

    // Navigation & Screen State
    private val _currentDestination = MutableStateFlow(TvNavDestination.LIVE_TV)
    val currentDestination: StateFlow<TvNavDestination> = _currentDestination.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    // Selection & Playback State
    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory: StateFlow<CategoryEntity?> = _selectedCategory.asStateFlow()

    private val _selectedChannel = MutableStateFlow<ChannelEntity?>(null)
    val selectedChannel: StateFlow<ChannelEntity?> = _selectedChannel.asStateFlow()

    private val _playingChannel = MutableStateFlow<ChannelEntity?>(null)
    val playingChannel: StateFlow<ChannelEntity?> = _playingChannel.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    // Categories Flow for active portal and destination
    val categories: StateFlow<List<CategoryEntity>> = combine(activePortal, currentDestination) { portal, dest ->
        portal to dest
    }.flatMapLatest { (portal, dest) ->
        if (portal == null) {
            flowOf(emptyList())
        } else {
            val type = when (dest) {
                TvNavDestination.LIVE_TV, TvNavDestination.FAVORITES -> StreamType.LIVE
                TvNavDestination.MOVIES -> StreamType.MOVIE
                TvNavDestination.SERIES -> StreamType.SERIES
                else -> StreamType.LIVE
            }
            repository.getCategories(portal.id, type)
        }
    }.onEach { catList ->
        if (_selectedCategory.value == null || catList.none { it.categoryId == _selectedCategory.value?.categoryId }) {
            _selectedCategory.value = catList.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Channels Flow for active portal & selected category
    val channels: StateFlow<List<ChannelEntity>> = combine(
        activePortal,
        currentDestination,
        selectedCategory,
        searchQuery
    ) { portal, dest, category, query ->
        Quadruple(portal, dest, category, query)
    }.flatMapLatest { (portal, dest, category, query) ->
        if (portal == null) return@flatMapLatest flowOf(emptyList())

        if (query.isNotBlank()) {
            return@flatMapLatest repository.searchChannels(portal.id, query)
        }

        when (dest) {
            TvNavDestination.FAVORITES -> repository.getFavorites(portal.id)
            else -> {
                if (category != null) {
                    repository.getChannelsByCategory(portal.id, category.categoryId)
                } else {
                    flowOf(emptyList())
                }
            }
        }
    }.onEach { chList ->
        if (_playingChannel.value == null && chList.isNotEmpty()) {
            _playingChannel.value = chList.firstOrNull()
            _selectedChannel.value = chList.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(dest: TvNavDestination) {
        _currentDestination.value = dest
        _searchQuery.value = ""
        _selectedCategory.value = null
    }

    fun selectCategory(cat: CategoryEntity) {
        _selectedCategory.value = cat
    }

    fun selectChannel(ch: ChannelEntity) {
        _selectedChannel.value = ch
    }

    fun playChannel(ch: ChannelEntity) {
        _playingChannel.value = ch
        _selectedChannel.value = ch
        _playerError.value = null
        viewModelScope.launch {
            repository.recordWatch(ch.id)
        }
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
            repository.syncActivePortal()
        }
    }

    fun syncFromCloud(email: String, pass: String) {
        viewModelScope.launch {
            repository.syncFromCloud(email, pass)
            repository.syncActivePortal()
        }
    }

    fun addDemoPortal() {
        viewModelScope.launch {
            val demo = PortalConfig(
                name = "Demo Xtream Portal",
                serverUrl = "http://line.liveiptv.pro:80",
                username = "demo",
                password = "demo"
            )
            repository.savePortal(demo, setActive = true)
            repository.syncActivePortal()
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
