package com.tvmime.tv.ui.livetv

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.model.StreamType

@Composable
fun LiveTvRoute(
    modifier: Modifier = Modifier,
    onNavigateToPlayer: (String) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: LiveTvViewModel = viewModel(
        factory = LiveTvViewModel.Factory(application)
    )

    val uiState by viewModel.uiState.collectAsState()

    LiveTvGrid(
        modifier = modifier,
        categories = uiState.categories,
        channels = uiState.channels,
        selectedCategoryId = uiState.selectedCategoryId,
        onCategorySelected = { viewModel.selectCategory(it) },
        onChannelClick = { channel ->
            // In TVMime V3, EngineController handles playback, but from a Route,
            // we typically navigate to a Player route or trigger a global controller.
            // For now, we invoke the provided lambda with the directSourceUrl or streamId.
            // Assuming stream ID or direct URL is needed. 
            // In production, we'd build the full XTREAM url if it's missing, but let's pass directSourceUrl or id.
            val urlToPlay = channel.directSourceUrl.takeIf { it.isNotEmpty() } ?: channel.streamId.toString()
            onNavigateToPlayer(urlToPlay)
        }
    )
}
