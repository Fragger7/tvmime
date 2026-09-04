package com.tvmime.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.tvmime.network.XtreamClient

enum class BufferProfile(
    val label: String,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
) {
    LOW_LATENCY(
        label = "Fast Zap (Low Latency)",
        minBufferMs = 5000,
        maxBufferMs = 15000,
        bufferForPlaybackMs = 1000,
        bufferForPlaybackAfterRebufferMs = 2000
    ),
    STABILITY(
        label = "Stability Mode (Deep Buffer)",
        minBufferMs = 60000,
        maxBufferMs = 120000,
        bufferForPlaybackMs = 5000,
        bufferForPlaybackAfterRebufferMs = 10000
    )
}

/**
 * Media3 / ExoPlayer hardware-accelerated IPTV configuration.
 * V1.0 Premium Upgrades: Auto Frame Rate (AFR), Audio Passthrough, and Fast Zap buffers.
 */
object Media3PlayerConfig {

    fun createHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent(XtreamClient.EVASION_USER_AGENT)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
    }

    @OptIn(UnstableApi::class)
    fun buildLoadControl(profile: BufferProfile = BufferProfile.LOW_LATENCY): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                profile.minBufferMs,
                profile.maxBufferMs,
                profile.bufferForPlaybackMs,
                profile.bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    @OptIn(UnstableApi::class)
    fun buildPlayer(
        context: Context,
        bufferProfile: BufferProfile = BufferProfile.LOW_LATENCY,
        enableAfr: Boolean = true
    ): ExoPlayer {
        // 1. Hardware Video Decoder Prioritization
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        // 2. Track Selector: Prefer Audio Passthrough (AC3/DTS to Soundbar)
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioMimeTypes(
                        "audio/ac3", 
                        "audio/eac3", 
                        "audio/vnd.dts"
                    )
            )
        }

        val httpDataSourceFactory = createHttpDataSourceFactory()
        val upstreamDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(upstreamDataSourceFactory)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(buildLoadControl(bufferProfile))
            .setTrackSelector(trackSelector)
            .build()

        // 3. Audio Attributes (Crucial for TV / HDMI ARC Passthrough)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        player.setAudioAttributes(audioAttributes, true)

        // 4. Auto Frame Rate (AFR) & Black Screen Minimizer
        if (enableAfr) {
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            player.videoChangeFrameRateStrategy = C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
        } else {
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }

        return player
    }
}
