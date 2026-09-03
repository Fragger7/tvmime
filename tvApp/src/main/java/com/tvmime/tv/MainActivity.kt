package com.tvmime.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.ui.live.LiveTvScreen
import com.tvmime.tv.ui.navigation.TvNavigationDrawer
import com.tvmime.tv.ui.settings.SettingsScreen
import com.tvmime.tv.ui.sync.CloudSyncScreen
import com.tvmime.tv.viewmodel.TvMainViewModel
import com.tvmime.tv.viewmodel.TvNavDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TVMimeTvApp()
        }
    }
}

@Composable
fun TVMimeTvApp(viewModel: TvMainViewModel = viewModel()) {
    val bgColor = Color(DesignSystemTokens.Colors.Background)

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val activePortal by viewModel.activePortal.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val playingChannel by viewModel.playingChannel.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()

    // Handle Back Button in Fullscreen
    BackHandler(enabled = isFullscreen) {
        viewModel.setFullscreen(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (isFullscreen) {
            LiveTvScreen(
                categories = categories,
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.selectCategory(it) },
                channels = channels,
                selectedChannel = selectedChannel,
                playingChannel = playingChannel,
                onPlayChannel = { viewModel.playChannel(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                isFullscreen = true,
                onToggleFullscreen = { viewModel.toggleFullscreen() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Collapsible Left-Side Navigation Drawer
                TvNavigationDrawer(
                    currentDestination = currentDestination,
                    onDestinationSelected = { dest ->
                        viewModel.navigateTo(dest)
                    }
                )

                // Screen Content Area
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentDestination) {
                        TvNavDestination.LIVE_TV,
                        TvNavDestination.FAVORITES -> {
                            LiveTvScreen(
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                channels = channels,
                                selectedChannel = selectedChannel,
                                playingChannel = playingChannel,
                                onPlayChannel = { viewModel.playChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                isFullscreen = false,
                                onToggleFullscreen = { viewModel.toggleFullscreen() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        TvNavDestination.MOVIES,
                        TvNavDestination.SERIES -> {
                            com.tvmime.tv.ui.vod.VodScreen(
                                title = if (currentDestination == TvNavDestination.MOVIES) "Movies" else "TV Series",
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                items = channels,
                                onPlayItem = { item ->
                                    viewModel.playChannel(item)
                                    viewModel.setFullscreen(true)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        TvNavDestination.CLOUD_SYNC -> {
                            CloudSyncScreen(
                                activePortal = activePortal,
                                syncProgress = syncProgress,
                                onSyncCurrentPortal = { viewModel.syncCurrentPortal() },
                                onLoadDemoPortal = { viewModel.addDemoPortal() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        TvNavDestination.SETTINGS -> {
                            SettingsScreen(
                                channelCount = channels.size,
                                categoryCount = categories.size,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
