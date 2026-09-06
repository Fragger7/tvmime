package com.tvmime.tv.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineController @Inject constructor(
    private val livePreviewEngine: LivePreviewEngine,
    private val mainPlayer: MainPlayer
) {
    companion object {
        const val SURFACE_HANDOFF_MS = 508L // Solves 0x80001000 MediaCodec claim failure
    }

    private val _activeEngine = MutableStateFlow<TvMimeVideoEngine>(livePreviewEngine)
    val activeEngine = _activeEngine.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    fun startLivePreview(url: String) {
        if (mainPlayer.hasActiveStream()) return
        
        _activeEngine.value = livePreviewEngine
        livePreviewEngine.play(url)
    }

    fun handoffToMainPlayer(url: String) {
        scope.launch {
            // 1. Teardown the ExoPlayer connection
            livePreviewEngine.stop()
            
            // 2. Hardware Decoder OS Release Quirk
            delay(SURFACE_HANDOFF_MS)

            // 3. Boot libmpv (MainPlayer)
            _activeEngine.value = mainPlayer
            mainPlayer.play(url)
        }
    }

    fun teardownAll() {
        livePreviewEngine.release()
        mainPlayer.release()
    }
}
