package com.tvmime.tv.ui.player

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.HttpDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.db.entity.PortalEntity
import com.tvmime.player.BufferProfile
import com.tvmime.player.Media3PlayerConfig
import com.tvmime.sync.FirebaseSyncClient
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.hardware.DeviceCapabilityDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

private enum class AspectRatioMode(val label: String, val mode: Int) {
    FIT("16:9 Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Stretch Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

private enum class PlaybackOverlayModal {
    AUDIO_TRACKS,
    SUBTITLES
}

private data class TrackOption(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvVideoPlayer(
    channel: ChannelEntity?,
    activePortal: PortalEntity? = null,
    activeConnections: Int = 1,
    maxConnections: Int = 1,
    showClockOverlay: Boolean = true,
    autoHideOsdSeconds: Int = 5,
    enableLastChannelZap: Boolean = true,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onToggleFavorite: ((ChannelEntity) -> Unit)? = null,
    onToggleLastChannel: (() -> Boolean)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val crimsonBright = Color(DesignSystemTokens.Colors.CrimsonBright)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val borderCol = Color(DesignSystemTokens.Colors.Border)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)

    val capabilities = remember { DeviceCapabilityDetector.detect(context) }
    val bufferProfile = if (capabilities.isLowRamDevice) BufferProfile.FAST_ZAP else BufferProfile.BALANCED

    // Player States
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var errorCode by remember { mutableStateOf("STREAM_ERROR") }
    var reportStatus by remember { mutableStateOf<String?>(null) }
    var isReporting by remember { mutableStateOf(false) }

    // OSD & Controls State
    var isOsdVisible by remember { mutableStateOf(true) }
    var activeModal by remember { mutableStateOf<PlaybackOverlayModal?>(null) }
    var currentAspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var showStatsHud by remember { mutableStateOf(false) }
    var showLastChannelToast by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(showLastChannelToast) {
        if (showLastChannelToast) {
            delay(2500)
            showLastChannelToast = false
        }
    }

    // Telemetry stats
    var videoResolution by remember { mutableStateOf("Auto") }
    var videoFps by remember { mutableStateOf("--") }
    var videoCodec by remember { mutableStateOf("Hardware Decoder") }
    var audioCodec by remember { mutableStateOf("Audio DSP") }
    var streamBitrate by remember { mutableStateOf("--") }
    var bufferedSeconds by remember { mutableLongStateOf(0L) }

    // Remote Host/IP Parsing
    val streamHost = remember(channel?.directSourceUrl) {
        try {
            val uri = URI(channel?.directSourceUrl ?: "")
            val host = uri.host ?: ""
            val port = if (uri.port > 0) ":${uri.port}" else ""
            if (host.isNotBlank()) "$host$port" else "Direct Stream"
        } catch (e: Exception) {
            "Direct Stream"
        }
    }

    // Tracks
    var audioTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }

    val exoPlayer = remember {
        Media3PlayerConfig.buildPlayer(context, bufferProfile).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Keep player stats refreshed
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            val vFormat = exoPlayer.videoFormat
            if (vFormat != null) {
                videoResolution = if (vFormat.width > 0 && vFormat.height > 0) "${vFormat.width}x${vFormat.height}" else "Auto"
                videoFps = if (vFormat.frameRate > 0) "${vFormat.frameRate.toInt()} fps" else "--"
                videoCodec = vFormat.sampleMimeType?.substringAfterLast("/")?.uppercase() ?: "HEVC/H.264"
                streamBitrate = if (vFormat.bitrate > 0) {
                    val mbps = vFormat.bitrate / 1_000_000f
                    if (mbps >= 1f) String.format(Locale.US, "%.1f Mbps", mbps) else "${vFormat.bitrate / 1000} kbps"
                } else "--"
            }
            val aFormat = exoPlayer.audioFormat
            if (aFormat != null) {
                val channels = if (aFormat.channelCount == 6) "5.1 Surround" else "${aFormat.channelCount}ch"
                val mime = aFormat.sampleMimeType?.substringAfterLast("/")?.uppercase() ?: "AAC"
                audioCodec = "$mime • $channels"
            }
            bufferedSeconds = ((exoPlayer.bufferedPosition - exoPlayer.currentPosition).coerceAtLeast(0) / 1000)
            delay(1000)
        }
    }

    // Auto-hide OSD after configured timeout in fullscreen
    LaunchedEffect(isOsdVisible, lastInteractionTime, activeModal, autoHideOsdSeconds) {
        if (isFullscreen && isOsdVisible && activeModal == null && autoHideOsdSeconds > 0) {
            delay(autoHideOsdSeconds * 1000L)
            isOsdVisible = false
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

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audios = mutableListOf<TrackOption>()
                val subs = mutableListOf<TrackOption>()

                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (t in 0 until group.length) {
                            val fmt = group.getTrackFormat(t)
                            val lang = fmt.language?.uppercase() ?: "AUDIO"
                            val channels = if (fmt.channelCount == 6) "5.1 AC3" else if (fmt.channelCount == 2) "Stereo" else "${fmt.channelCount}ch"
                            val mime = fmt.sampleMimeType?.substringAfterLast("/")?.uppercase() ?: ""
                            audios.add(
                                TrackOption(
                                    group = group,
                                    trackIndex = t,
                                    label = "$lang ($channels $mime)",
                                    isSelected = group.isTrackSelected(t)
                                )
                            )
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (t in 0 until group.length) {
                            val fmt = group.getTrackFormat(t)
                            val lang = fmt.language?.uppercase() ?: "SUBTITLE"
                            subs.add(
                                TrackOption(
                                    group = group,
                                    trackIndex = t,
                                    label = lang,
                                    isSelected = group.isTrackSelected(t)
                                )
                            )
                        }
                    }
                }
                audioTracks = audios
                subtitleTracks = subs
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

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    var currentTimeString by remember { mutableStateOf(timeFormat.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = timeFormat.format(Date())
            delay(10000L)
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
            .focusable(isFullscreen)
            .onKeyEvent { keyEvent ->
                if (!isFullscreen) return@onKeyEvent false
                if (keyEvent.type == KeyEventType.KeyDown) {
                    lastInteractionTime = System.currentTimeMillis()
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!isOsdVisible) {
                                isOsdVisible = true
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (enableLastChannelZap && onToggleLastChannel != null) {
                                val toggled = onToggleLastChannel()
                                if (toggled) {
                                    showLastChannelToast = true
                                }
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (activeModal != null) {
                                activeModal = null
                                true
                            } else if (isOsdVisible) {
                                isOsdVisible = false
                                true
                            } else {
                                onToggleFullscreen()
                                true
                            }
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // --- 1. ExoPlayer Video Surface ---
        if (channel != null && channel.directSourceUrl.isNotBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = currentAspectMode.mode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.resizeMode = currentAspectMode.mode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 2. Buffering Spinner ---
        if (isBuffering && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = crimson,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Buffering stream...", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // --- 3. Telemetry Stats HUD Overlay ---
        if (showStatsHud && isFullscreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEE0A0A12))
                    .border(1.dp, Color(0xFF2E2E40), RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                val maxBufferSec = if (capabilities.isLowRamDevice) 15f else 30f
                val bufferPercent = ((bufferedSeconds.toFloat() / maxBufferSec) * 100).toInt().coerceIn(0, 100)

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "LIVE STREAM TELEMETRY",
                            color = crimsonBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Text("Resolution: $videoResolution ($videoFps)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Data Transfer Rate: $streamBitrate", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Stream Host / IP: $streamHost", color = textSecondary, fontSize = 11.sp)
                    Text(
                        text = "Buffer Depth: ${bufferedSeconds}s / ${maxBufferSec.toInt()}s ($bufferPercent% cached)",
                        color = if (bufferedSeconds > 2) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Video Decoder: $videoCodec", color = textSecondary, fontSize = 11.sp)
                    Text("Audio Track: $audioCodec", color = textSecondary, fontSize = 11.sp)
                    Text("Aspect Scaling: ${currentAspectMode.label}", color = textSecondary, fontSize = 11.sp)
                }
            }
        }

        // --- 4. Dedicated Clock Overlay (Top-Right Screen Corner, Persistent if enabled) ---
        if (showClockOverlay && isFullscreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x990A0A10))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = currentTimeString,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // --- 5. Last Channel Zap Floating Toast ---
        AnimatedVisibility(
            visible = showLastChannelToast && isFullscreen,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xEE0C0C14))
                    .border(1.dp, crimson, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Last Channel",
                        tint = crimsonBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "LAST CHANNEL: CH ${channel?.num ?: 0} • ${channel?.name ?: ""}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- 6. Fullscreen OSD: Top Bar & Bottom Bar ---
        if (isFullscreen) {
            AnimatedVisibility(
                visible = isOsdVisible,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                // Top Information Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xDD070709))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Channel Number, Channel Name, Live Resolution
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(crimson)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CH ${channel?.num ?: 0}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = channel?.name ?: "No Channel Playing",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Live Resolution Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1F1F2C))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = videoResolution,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Right: Connection / Playlist Name & Active/Max Connections Badge
                    val activeCons = activeConnections
                    val maxCons = maxConnections
                    val isExceeded = activeCons > maxCons

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1E28))
                            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isExceeded) Color(0xFFEF4444) else Color(0xFF10B981))
                            )

                            Text(
                                text = activePortal?.name ?: "TVMime Public Demo",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "•",
                                color = textSecondary,
                                fontSize = 11.sp
                            )

                            Text(
                                text = "$activeCons/$maxCons Cons",
                                color = if (isExceeded) Color(0xFFEF4444) else crimsonBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isOsdVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // Bottom Quick Actions Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xEE0A0A10))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Pause Toggle
                        Surface(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isPlaying) Color(0xFF1E1E2C) else crimson,
                                focusedContainerColor = crimsonBright
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(if (isPlaying) "Pause" else "Play", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Aspect Ratio Cycle
                        Surface(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                currentAspectMode = when (currentAspectMode) {
                                    AspectRatioMode.FIT -> AspectRatioMode.FILL
                                    AspectRatioMode.FILL -> AspectRatioMode.ZOOM
                                    AspectRatioMode.ZOOM -> AspectRatioMode.FIT
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color(0xFF1E1E2C),
                                focusedContainerColor = crimsonBright
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AspectRatio, contentDescription = "Aspect", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(currentAspectMode.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Audio Track Selector
                        Surface(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                activeModal = if (activeModal == PlaybackOverlayModal.AUDIO_TRACKS) null else PlaybackOverlayModal.AUDIO_TRACKS
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (activeModal == PlaybackOverlayModal.AUDIO_TRACKS) crimson else Color(0xFF1E1E2C),
                                focusedContainerColor = crimsonBright
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Audiotrack, contentDescription = "Audio", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Audio (${audioTracks.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Subtitles Selector
                        Surface(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                activeModal = if (activeModal == PlaybackOverlayModal.SUBTITLES) null else PlaybackOverlayModal.SUBTITLES
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (activeModal == PlaybackOverlayModal.SUBTITLES) crimson else Color(0xFF1E1E2C),
                                focusedContainerColor = crimsonBright
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Subtitles", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Stats HUD Toggle
                        Surface(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                showStatsHud = !showStatsHud
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (showStatsHud) Color(0xFF10B981) else Color(0xFF1E1E2C),
                                focusedContainerColor = crimsonBright
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = "Stats", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("HUD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite Toggle
                        if (channel != null && onToggleFavorite != null) {
                            Surface(
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    onToggleFavorite(channel)
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color(0xFF1E1E2C),
                                    focusedContainerColor = crimsonBright
                                ),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Icon(
                                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (channel.isFavorite) crimsonBright else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Exit Fullscreen / Back to Guide
                        Surface(
                            onClick = onToggleFullscreen,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color(0xFF1E1E2C),
                                focusedContainerColor = crimson
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Guide", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- 7. Audio / Subtitles Modal Dialog Sheet ---
            if (activeModal != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(bottom = 80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF12121A))
                            .border(1.dp, crimson, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (activeModal == PlaybackOverlayModal.AUDIO_TRACKS) "SELECT AUDIO TRACK" else "SELECT SUBTITLES",
                                    color = crimsonBright,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )

                                Surface(
                                    onClick = { activeModal = null },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = Color(0xFF222230),
                                        focusedContainerColor = crimson
                                    ),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            if (activeModal == PlaybackOverlayModal.AUDIO_TRACKS) {
                                if (audioTracks.isEmpty()) {
                                    Text("Single default audio stream detected", color = textSecondary, fontSize = 12.sp)
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 200.dp)) {
                                        items(audioTracks) { track ->
                                            Surface(
                                                onClick = {
                                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                        .buildUpon()
                                                        .setOverrideForType(TrackSelectionOverride(track.group.mediaTrackGroup, listOf(track.trackIndex)))
                                                        .build()
                                                    activeModal = null
                                                },
                                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                                                colors = ClickableSurfaceDefaults.colors(
                                                    containerColor = if (track.isSelected) Color(0xFF261214) else Color(0xFF1E1E28),
                                                    focusedContainerColor = crimson
                                                ),
                                                modifier = Modifier.fillMaxWidth().height(36.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(track.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    if (track.isSelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = crimsonBright, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Subtitle options
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 200.dp)) {
                                    item {
                                        Surface(
                                            onClick = {
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                                    .build()
                                                activeModal = null
                                            },
                                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                                            colors = ClickableSurfaceDefaults.colors(
                                                containerColor = Color(0xFF1E1E28),
                                                focusedContainerColor = crimson
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Off (Disable Subtitles)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    items(subtitleTracks) { sub ->
                                        Surface(
                                            onClick = {
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                    .setOverrideForType(TrackSelectionOverride(sub.group.mediaTrackGroup, listOf(sub.trackIndex)))
                                                    .build()
                                                activeModal = null
                                            },
                                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                                            colors = ClickableSurfaceDefaults.colors(
                                                containerColor = if (sub.isSelected) Color(0xFF261214) else Color(0xFF1E1E28),
                                                focusedContainerColor = crimson
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(sub.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                if (sub.isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = crimsonBright, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 8. Error State Overlay with 1-Click Firestore Reporting ---
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
                    Icon(
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
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Retry Stream", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

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
                                Icon(Icons.Default.BugReport, contentDescription = "Report", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Text("Report Stream Issue", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- 9. Non-fullscreen Preview Header (when embedded in screen) ---
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
                        text = "CH ${channel.num} • ${channel.containerExtension.uppercase()} • $videoResolution",
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
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
