package com.tvmime.tv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient

class LivePreviewEngine(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val mainPlayerIsActive: () -> Boolean
) : TvMimeVideoEngine {

    override val engineType = EngineType.EXO_PREVIEW
    private val _currentState = MutableStateFlow(EngineState.IDLE)
    override val currentState: StateFlow<EngineState> = _currentState.asStateFlow()

    var exoPlayer: ExoPlayer? = null
        private set

    override fun initialize() {
        // Fast Zap 500ms LoadControl Profile
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(500, 50000, 500, 500)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _currentState.value = when (state) {
                            Player.STATE_BUFFERING -> EngineState.BUFFERING
                            Player.STATE_READY -> EngineState.PLAYING
                            Player.STATE_ENDED -> EngineState.IDLE
                            else -> EngineState.IDLE
                        }
                    }
                })
            }
    }

    override fun play(url: String) {
        if (mainPlayerIsActive()) return // Do not lock out single-session streams
        if (exoPlayer == null) initialize()
        
        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    override fun pause() {
        exoPlayer?.playWhenReady = false
    }

    override fun stop() {
        exoPlayer?.stop()
        _currentState.value = EngineState.IDLE
    }

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
        _currentState.value = EngineState.IDLE
    }

    override fun hasActiveStream(): Boolean = exoPlayer?.isPlaying == true
}
