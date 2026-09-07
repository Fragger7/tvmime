package com.tvmime.tv.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.tvmime.tv.viewmodel.TvMainViewModel

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerRoute(
    streamUrl: String,
    viewModel: TvMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engineState by viewModel.activeEngineState.collectAsState()

    // When the route opens, ensure we are playing this stream
    LaunchedEffect(streamUrl) {
        viewModel.playChannel(
            com.tvmime.db.entity.ChannelEntity(
                id = streamUrl, 
                portalId = "", 
                streamId = 0, 
                name = "Live Stream", 
                type = "LIVE", 
                categoryId = "", 
                directSourceUrl = streamUrl
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.engineControllerInstance.teardownAll()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                // This block runs on recomposition (e.g. when engineState changes from IDLE to BUFFERING)
                val exo = viewModel.engineControllerInstance.livePreviewEngine.exoPlayer
                if (playerView.player != exo) {
                    playerView.player = exo
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // HUD Overlay
        PlayerOverlay(
            streamUrl = streamUrl,
            engineState = engineState,
            modifier = Modifier.fillMaxSize()
        )
    }
}
