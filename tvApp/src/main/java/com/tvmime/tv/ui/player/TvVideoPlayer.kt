package com.tvmime.tv.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val capabilities = remember { DeviceCapabilityDetector.detect(context) }
    val bufferProfile = if (capabilities.isLowRamDevice) BufferProfile.FAST_ZAP else BufferProfile.BALANCED

    var isBuffering by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        Media3PlayerConfig.buildPlayer(context, bufferProfile)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Handle channel changes smoothly
    LaunchedEffect(channel) {
        if (channel != null && channel.directSourceUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(channel.directSourceUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
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

        if (isBuffering && channel != null) {
            CircularProgressIndicator(
                color = Color(0xFFE50914),
                modifier = Modifier.align(Alignment.Center).size(48.dp)
            )
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
