package com.tvmime.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.tvmime.tv.viewmodel.TvMainViewModel

@Composable
fun PlayerRoute(
    streamUrl: String,
    viewModel: TvMainViewModel,
    modifier: Modifier = Modifier
) {
    val engineState by viewModel.activeEngineState.collectAsState()

    // When the route opens, ensure we are playing this stream in fullscreen
    LaunchedEffect(streamUrl) {
        viewModel.fullscreenChannel(
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

    PlayerOverlay(
        streamUrl = streamUrl,
        engineState = engineState,
        modifier = modifier
    )
}
