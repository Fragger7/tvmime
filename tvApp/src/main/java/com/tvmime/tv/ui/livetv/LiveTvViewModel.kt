package com.tvmime.tv.ui.livetv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tvmime.db.AppDatabase
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class LiveTvUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val channels: List<ChannelEntity> = emptyList(),
    val selectedCategoryId: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val portalDao = db.portalDao()
    private val categoryDao = db.categoryDao()
    private val channelDao = db.channelDao()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)

    private val activePortalIds = portalDao.getActivePortals().map { portals ->
        portals.map { it.id }
    }

    val uiState: StateFlow<LiveTvUiState> = activePortalIds.flatMapLatest { portalIds ->
        if (portalIds.isEmpty()) {
            flowOf(LiveTvUiState())
        } else {
            combine(
                categoryDao.getCategoriesForPortals(portalIds, "LIVE"),
                _selectedCategoryId
            ) { categories, selectedId ->
                val safeSelectedId = selectedId ?: categories.firstOrNull()?.categoryId
                Pair(categories, safeSelectedId)
            }.flatMapLatest { (categories, categoryId) ->
                if (categoryId != null) {
                    channelDao.getChannelsByCategoryForPortals(portalIds, categoryId).map { channels ->
                        LiveTvUiState(
                            categories = categories,
                            channels = channels,
                            selectedCategoryId = categoryId
                        )
                    }
                } else {
                    flowOf(LiveTvUiState(categories = categories))
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveTvUiState()
    )

    fun selectCategory(category: CategoryEntity) {
        _selectedCategoryId.value = category.categoryId
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LiveTvViewModel::class.java)) {
                return LiveTvViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
