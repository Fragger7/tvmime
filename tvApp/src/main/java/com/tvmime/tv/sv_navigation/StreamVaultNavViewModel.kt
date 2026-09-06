package com.tvmime.tv.sv_navigation

import androidx.lifecycle.ViewModel
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class StreamVaultNavViewModel @Inject constructor(
    val providerRepository: ProviderRepository,
    val favoriteRepository: FavoriteRepository,
    val playbackHistoryRepository: PlaybackHistoryRepository,
    val channelRepository: ChannelRepository,
    val combinedM3uRepository: CombinedM3uRepository,
    val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val externalNavigationRequestFlow: StateFlow<ExternalNavigationRequest?> = MutableStateFlow(null)
    fun clearExternalNavigationRequest() {}
}
