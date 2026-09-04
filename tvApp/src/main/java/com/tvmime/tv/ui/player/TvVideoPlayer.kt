package com.tvmime.tv.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.player.BufferProfile
import com.tvmime.player.Media3PlayerConfig
import com.tvmime.tv.hardware.DeviceCapabilityDetector

@Composable
fun TvVideoPlayer(
    channel: ChannelEntity?,
    isFullscreen: Boolean = true,
    onToggleFullscreen: () -> Unit = {},
    onPlayerError: ((String) -> Unit)? = null,
    onAutoSkipNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val capabilities = remember { DeviceCapabilityDetector.detect(context) }
    val bufferProfile = if (capabilities.isLowRamDevice) BufferProfile.LOW_LATENCY else BufferProfile.STABILITY

    var isBuffering by remember { mutableStateOf(false) }
    var streamErrorMessage by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }

    val exoPlayer = remember {
        Media3PlayerConfig.buildPlayer(context, bufferProfile)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    streamErrorMessage = null
                    retryCount = 0
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                val errorDesc = error.localizedMessage ?: "Stream playback failed"
                val isHttpError = error.errorCodeName.contains("HTTP", ignoreCase = true) || errorDesc.contains("40", ignoreCase = true)

                if (retryCount == 0 && channel != null) {
                    // Try 1 auto-recovery: switch between .ts and .m3u8 if applicable
                    retryCount++
                    val currentUrl = channel.directSourceUrl
                    val alternateUrl = when {
                        currentUrl.endsWith(".ts") -> currentUrl.substringBeforeLast(".ts") + ".m3u8"
                        currentUrl.endsWith(".m3u8") -> currentUrl.substringBeforeLast(".m3u8") + ".ts"
                        else -> null
                    }
                    if (alternateUrl != null) {
                        streamErrorMessage = "Retrying format (${retryCount}/1)..."
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        exoPlayer.setMediaItem(MediaItem.fromUri(alternateUrl))
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                        return
                    }
                }

                streamErrorMessage = when {
                    errorDesc.contains("404") -> "Channel Offline or Not Found (HTTP 404)"
                    errorDesc.contains("456") || errorDesc.contains("884") -> "Stream Limit Reached (HTTP 456/884)"
                    errorDesc.contains("403") -> "Stream Access Forbidden (HTTP 403)"
                    else -> "Playback Error: ${error.errorCodeName}"
                }
                onPlayerError?.invoke(streamErrorMessage ?: errorDesc)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    // Prevent IPTV max_connections lockout
                    exoPlayer.stop()
                }
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    if (channel != null) {
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle channel changes smoothly with strict socket release
    LaunchedEffect(channel) {
        streamErrorMessage = null
        retryCount = 0
        // Immediately stop previous stream to close HTTP socket (vital for max_connections: 1 IPTV portals)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        if (channel != null && channel.directSourceUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(channel.directSourceUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We use our own HUD/OSD
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && channel != null && streamErrorMessage == null) {
            CircularProgressIndicator(
                color = Color(0xFFE50914),
                modifier = Modifier.align(Alignment.Center).size(48.dp)
            )
        }

        if (streamErrorMessage != null && channel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xEB14141E), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = streamErrorMessage ?: "Stream Playback Error",
                        color = Color(0xFFFF5252),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Press [OK] to open controls or [UP/DOWN] to switch channels",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (channel == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Select a channel to begin", color = Color.Gray)
            }
        }
    }
}
