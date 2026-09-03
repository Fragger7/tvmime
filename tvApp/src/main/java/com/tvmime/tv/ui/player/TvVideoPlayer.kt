package com.tvmime.tv.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
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
import androidx.media3.datasource.HttpDataSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.player.BufferProfile
import com.tvmime.player.Media3PlayerConfig
import com.tvmime.sync.FirebaseSyncClient
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.hardware.DeviceCapabilityDetector
import kotlinx.coroutines.launch

@Composable
fun TvVideoPlayer(
    channel: ChannelEntity?,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val borderCol = Color(DesignSystemTokens.Colors.Border)

    val capabilities = remember { DeviceCapabilityDetector.detect(context) }
    val bufferProfile = if (capabilities.isLowRamDevice) BufferProfile.FAST_ZAP else BufferProfile.BALANCED

    var playbackError by remember { mutableStateOf<String?>(null) }
    var errorCode by remember { mutableStateOf<String>("STREAM_ERROR") }
    var isBuffering by remember { mutableStateOf(false) }
    var reportStatus by remember { mutableStateOf<String?>(null) }
    var isReporting by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        Media3PlayerConfig.buildPlayer(context, bufferProfile).apply {
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
                    reportStatus = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val cause = error.cause
                when {
                    cause is HttpDataSource.InvalidResponseCodeException && (cause.responseCode == 456 || cause.responseCode == 884) -> {
                        errorCode = "HTTP_${cause.responseCode}_LIMIT_EXCEEDED"
                        playbackError = "Stream Egress Disabled (HTTP ${cause.responseCode}): Max connection limit reached on provider account."
                    }
                    cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 403 -> {
                        errorCode = "HTTP_403_FORBIDDEN"
                        playbackError = "Access Forbidden (HTTP 403): Provider subscription expired or anti-bot IP block."
                    }
                    cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 404 -> {
                        errorCode = "HTTP_404_NOT_FOUND"
                        playbackError = "Stream Offline (HTTP 404): Channel satellite downlink is temporarily down."
                    }
                    cause is HttpDataSource.HttpDataSourceException -> {
                        errorCode = "NETWORK_TIMEOUT"
                        playbackError = "Network connection dropped or timed out: ${cause.message ?: "Connection reset"}"
                    }
                    else -> {
                        errorCode = error.errorCodeName
                        playbackError = error.localizedMessage ?: "Hardware decoder / playback failure."
                    }
                }
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
        reportStatus = null
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

        // Overlay: Human-Friendly Error State with Reporting
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE0A0A10))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = crimson,
                        modifier = Modifier.size(44.dp)
                    )

                    Text(
                        text = "STREAM PLAYBACK ISSUE",
                        color = crimson,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = playbackError ?: "Stream Unavailable",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    if (reportStatus != null) {
                        Text(
                            text = reportStatus ?: "",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        // Retry Button
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
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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

                        // Report Issue Button
                        Surface(
                            onClick = {
                                if (isReporting) return@Surface
                                isReporting = true
                                reportStatus = "Logging issue to admin console..."
                                coroutineScope.launch {
                                    val client = FirebaseSyncClient()
                                    val res = client.reportStreamIssue(
                                        channelName = channel?.name ?: "Unknown Channel",
                                        channelNum = channel?.num,
                                        errorCode = errorCode,
                                        errorMessage = playbackError ?: "Stream failed",
                                        deviceSpecs = "${capabilities.model} • ${capabilities.recommendedBufferProfile}",
                                        timestamp = System.currentTimeMillis()
                                    )
                                    if (res.isSuccess) {
                                        reportStatus = "✓ Issue reported to TVMime Admin! Engineering notified."
                                    } else {
                                        reportStatus = "Failed to send report. Please check network."
                                    }
                                    isReporting = false
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color(0xFF1F1F2C),
                                focusedContainerColor = Color(0xFF2E2E40)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Default.BugReport,
                                    contentDescription = "Report",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Report Stream Issue", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
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
                        text = "CH ${channel.num} • ${channel.containerExtension.uppercase()} • ${bufferProfile.label}",
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
