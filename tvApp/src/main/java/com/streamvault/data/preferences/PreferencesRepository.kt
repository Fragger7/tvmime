package com.streamvault.data.preferences

import kotlinx.coroutines.flow.Flow
import com.streamvault.domain.model.*

interface PreferencesRepository {
    val playerControlsTimeoutSeconds: Flow<Int>
    val playerDiagnosticsTimeoutSeconds: Flow<Int>
    val playerEthernetMaxVideoHeight: Flow<Int>
    val playerExternalPlaybackMode: Flow<ExternalPlaybackMode>
    val playerFastRetryOnTransientFailures: Flow<Boolean>
    val playerLiveOverlayTimeoutSeconds: Flow<Int>
    val playerLiveTranslationEnabled: Flow<Boolean>
    val playerLiveTranslationEndpoint: Flow<String>
    val playerMediaSessionEnabled: Flow<Boolean>
    val playerMuted: Flow<Boolean>
    val playerNoticeTimeoutSeconds: Flow<Int>
    val playerPlaybackBufferMode: Flow<PlaybackBufferMode>
    val playerPlaybackSpeed: Flow<Float>
    val playerSubtitleBackgroundColor: Flow<Int>
    val playerSubtitleTextColor: Flow<Int>
    val playerSubtitleTextScale: Flow<Float>
    val playerSurfaceMode: Flow<PlayerSurfaceMode>
    val playerTimeshiftBackend: Flow<TimeshiftBackendPreference>
    val playerTimeshiftDepthMinutes: Flow<Int>
    val playerTimeshiftEnabled: Flow<Boolean>
    val playerVideoDecoderMode: Flow<DecoderMode>
    val playerVodHttpProtocolMode: Flow<VodHttpProtocolMode>
    val playerWifiMaxVideoHeight: Flow<Int>
    val preventStandbyDuringPlayback: Flow<Boolean>
    val preferredAudioLanguage: Flow<String>
    val remoteShortcutPreferences: Flow<RemoteShortcutPreferences>
    val vodVariantObservations: Flow<List<VodVariantObservation>>
    val zapAutoRevert: Flow<Boolean>
    
    val appTopLevelDestinations: Flow<List<AppTopLevelDestination>>
    val appLandingDestination: Flow<AppLandingDestination>
    val showFavoritesCategory: Flow<Boolean>
    val allowCleartextNetworkTraffic: Flow<Boolean>
    
    fun getAspectRatioForChannel(channelId: Long): Flow<Float?>
    fun getHiddenCategoryIds(providerId: Long, type: ContentType): Flow<Set<String>>
    fun getPinnedCategoryIds(providerId: Long, type: ContentType): Flow<Set<Long>>
    fun getLastLiveCategoryId(providerId: Long): Flow<String?>
    fun observeAudioVideoOffsetForChannel(channelId: Long): Flow<Long?>
    suspend fun clearAudioVideoOffsetForChannel(channelId: Long)
    suspend fun recordLiveVariantObservation(rawChannelId: String, observedQuality: LiveChannelObservedQuality)
    suspend fun recordVodVariantObservation(rawItemId: String, observation: VodVariantObservation)
    suspend fun setAspectRatioForChannel(channelId: Long, ratio: Float?)
    suspend fun setAudioVideoOffsetForChannel(channelId: Long, offsetMs: Long)
    suspend fun setPlayerAudioVideoOffsetMs(offsetMs: Long)
    suspend fun setPlayerFastRetryOnTransientFailures(enabled: Boolean)
    suspend fun setPlayerMuted(muted: Boolean)
    suspend fun setPlayerPlaybackSpeed(speed: Float)
    suspend fun setPreferredLiveVariant(providerId: Long, logicalGroupId: Long, rawChannelId: String)
    suspend fun setLastSplitCatalogType(providerId: Long, type: String)
    fun getHiddenChannelIds(providerId: Long): Flow<Set<Long>>
}
