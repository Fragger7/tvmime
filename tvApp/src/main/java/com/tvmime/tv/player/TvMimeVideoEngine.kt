package com.tvmime.tv.player

import kotlinx.coroutines.flow.StateFlow

enum class EngineState { IDLE, BUFFERING, PLAYING, ERROR }
enum class EngineType { EXO_PREVIEW, MPV_MAIN }

interface TvMimeVideoEngine {
    val engineType: EngineType
    val currentState: StateFlow<EngineState>
    
    fun initialize()
    fun play(url: String)
    fun pause()
    fun stop()
    fun release()
    fun hasActiveStream(): Boolean
}
