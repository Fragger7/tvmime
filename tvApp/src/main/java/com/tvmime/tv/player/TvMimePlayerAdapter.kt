package com.tvmime.tv.player

import com.streamvault.player.*
import com.streamvault.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class TvMimePlayerAdapter(
    private val engineController: EngineController
) : PlayerEngine {
    override val playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val isPlaying = MutableStateFlow(false)
    override val currentPosition = MutableStateFlow(0L)
    override val duration = MutableStateFlow(0L)
    override val videoFormat = MutableStateFlow(VideoFormat(0, 0))
    override val error: Flow<PlayerError?> = emptyFlow()
    override val retryStatus = MutableStateFlow<PlayerRetryStatus?>(null)
    override val playerStats = MutableStateFlow(PlayerStats())

    override val availableAudioTracks = MutableStateFlow(emptyList<PlayerTrack>())
    override val availableSubtitleTracks = MutableStateFlow(emptyList<PlayerTrack>())
    override val availableVideoTracks = MutableStateFlow(emptyList<PlayerTrack>())
    override val playbackSpeed = MutableStateFlow(1f)
    override val audioVideoOffsetMs = MutableStateFlow(0)
    override val audioVideoSyncEnabled = MutableStateFlow(false)
    override val timeshiftState = MutableStateFlow(com.streamvault.player.timeshift.LiveTimeshiftState())
    override val renderSurfaceType = MutableStateFlow(PlayerRenderSurfaceType.AUTO)

    override val mediaTitle = MutableStateFlow<String?>(null)

    private var currentStreamInfo: StreamInfo? = null

    override fun prepare(streamInfo: StreamInfo) {
        currentStreamInfo = streamInfo
        playbackState.value = PlaybackState.BUFFERING
        // Boot via TVMime dual engine controller!
        engineController.startLivePreview(streamInfo.url)
    }

    override fun renewStreamUrl(streamInfo: StreamInfo) {
        currentStreamInfo = streamInfo
        engineController.handoffToMainPlayer(streamInfo.url)
    }

    override fun play() {
        isPlaying.value = true
        playbackState.value = PlaybackState.READY
    }

    override fun pause() {
        isPlaying.value = false
    }

    override fun stop() {
        isPlaying.value = false
        playbackState.value = PlaybackState.IDLE
        engineController.teardownAll()
    }

    override fun seekTo(positionMs: Long) {}
    
    // Other PlayerEngine interface methods (stubs to satisfy compiler)
    override val audioFocusDenied = MutableStateFlow(false)
    override val isMuted = MutableStateFlow(false)
    
    override fun setMediaSessionEnabled(enabled: Boolean) {}
    override fun setFastRetryOnTransientFailures(enabled: Boolean) {}
    override fun setAudioVideoOffsetMs(offsetMs: Int) {}
    override fun setScrubbingMode(enabled: Boolean) {}
    override fun setSubtitleStyle(style: com.streamvault.player.PlayerSubtitleStyle) {}
    override fun setDecoderModes(audioMode: DecoderMode, videoMode: DecoderMode) {}
    override fun setPlaybackBufferMode(mode: PlaybackBufferMode) {}
    override fun setAudioOutputPreference(preference: AudioOutputPreference) {}
    override fun setCompatibilityMemoryEnabled(enabled: Boolean) {}
    override fun setSurfaceMode(mode: PlayerSurfaceMode) {}
    override fun setVodHttpProtocolMode(mode: VodHttpProtocolMode) {}
    override fun selectTrack(trackId: String, type: @androidx.media3.common.C.TrackType Int) {}
    override fun setVolume(volume: Float) {}
    override fun clearCache() {}
    override fun release() { stop() }
    override fun renderTo(surfaceView: android.view.SurfaceView) {}
    override fun renderTo(textureView: android.view.TextureView) {}
}
