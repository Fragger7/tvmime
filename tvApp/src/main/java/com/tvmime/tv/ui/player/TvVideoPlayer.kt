package com.tvmime.tv.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.player.Media3PlayerConfig
import com.tvmime.theme.DesignSystemTokens

@Composable
fun TvVideoPlayer(
    channel: ChannelEntity?,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val borderCol = Color(DesignSystemTokens.Colors.Border)

    var playbackError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        Media3PlayerConfig.buildPlayer(context).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val cause = error.cause
                val msg = when {
                    cause is HttpDataSource.InvalidResponseCodeException && (cause.responseCode == 456 || cause.responseCode == 884) -> {
                        "Stream Egress Disabled by Provider (HTTP ${cause.responseCode})"
                    }
                    cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 403 -> {
                        "Access Forbidden (HTTP 403) - Evasion Block"
                    }
                    cause is HttpDataSource.HttpDataSourceException -> {
                        "Connection dropped: ${cause.message ?: "Network error"}"
                    }
                    else -> error.localizedMessage ?: "Playback failure"
                }
                playbackError = msg
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // React to channel change
    LaunchedEffect(channel?.directSourceUrl) {
        playbackError = null
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

    Box(
        modifier = modifier
            .clip(if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(
                width = if (isFullscreen) 0.dp else 1.dp,
                color = if (isFullscreen) Color.Transparent else borderCol,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (channel != null && channel.directSourceUrl.isNotBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay: Buffering indicator
        if (isBuffering && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = crimson,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Buffering stream...", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Overlay: Error State
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD121217))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = crimson,
                        modifier = Modifier.size(40.dp)
                    )

                    Text(
                        text = playbackError ?: "Stream Unavailable",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Surface(
                        onClick = {
                            playbackError = null
                            channel?.let {
                                exoPlayer.setMediaItem(MediaItem.fromUri(it.directSourceUrl))
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            }
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = crimson,
                            focusedContainerColor = Color(0xFFFF1E27)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Retry Stream", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Overlay Header: Channel pill and Fullscreen toggle (in preview mode)
        if (!isFullscreen && channel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xAA070709), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CH ${channel.num} • ${channel.containerExtension.uppercase()}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    onClick = onToggleFullscreen,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0xAA070709),
                        focusedContainerColor = crimson
                    ),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
