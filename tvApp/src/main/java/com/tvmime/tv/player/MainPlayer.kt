package com.tvmime.tv.player

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// import dev.jdtech.mpv.MPVLib

class MainPlayer(
    private val context: Context
) : TvMimeVideoEngine {

    override val engineType = EngineType.MPV_MAIN
    private val _currentState = MutableStateFlow(EngineState.IDLE)
    override val currentState: StateFlow<EngineState> = _currentState.asStateFlow()
    
    private var isPlaying = false

    override fun initialize() {
        // MPVLib.create(context)
        // MPVLib.setOptionString("hwdec", "mediacodec-copy")
        // MPVLib.setOptionString("vo", "gpu")
    }

    override fun play(url: String) {
        if (!isPlaying) initialize()
        isPlaying = true
        _currentState.value = EngineState.BUFFERING
        // MPVLib.command(arrayOf("loadfile", url))
        // Listeners for MPV Events would update state to PLAYING here
    }

    override fun pause() {
        // MPVLib.setPropertyBoolean("pause", true)
    }

    override fun stop() {
        // MPVLib.command(arrayOf("stop"))
        isPlaying = false
        _currentState.value = EngineState.IDLE
    }

    override fun release() {
        // MPVLib.destroy()
        isPlaying = false
        _currentState.value = EngineState.IDLE
    }

    override fun hasActiveStream(): Boolean = isPlaying
}
