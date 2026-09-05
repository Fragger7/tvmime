package com.tvmime.tv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvmime.tv.ui.common.TvMimeTheme
import com.tvmime.tv.ui.TvMimeLiveScreen
import com.tvmime.tv.ui.settings.SettingsScreen
import com.tvmime.tv.ui.sync.CloudSyncScreen
import com.tvmime.tv.viewmodel.TvMainViewModel
import com.tvmime.tv.viewmodel.TvNavDestination
import com.tvmime.tv.viewmodel.TvOverlayState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvMimeTheme {
                TVMimeTvApp()
            }
        }
    }
}

@Composable
fun TVMimeTvApp(viewModel: TvMainViewModel = viewModel()) {
    val bgColor = Color.Black
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val activePortals by viewModel.activePortals.collectAsStateWithLifecycle()
    val allPortals by viewModel.allPortals.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val showClockOverlay by viewModel.showClockOverlay.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        
        // Z-Index 0/1: Live TV Engine (Player & Overlays)
        if (currentDestination == TvNavDestination.LIVE_TV) {
            TvMimeLiveScreen(viewModel)
        }

        // Overlay: Cloud Sync
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.CLOUD_SYNC,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xE605050A))) {
                CloudSyncScreen(
                    activePortals = activePortals ?: emptyList(),
                    allPortals = allPortals,
                    syncProgress = syncProgress,
                    onSyncCurrentPortal = { viewModel.syncCurrentPortal() },
                    onRefreshCloudPortals = { viewModel.refreshPortalsFromCloud() },
                    onTogglePortal = { portalId, isActive -> viewModel.toggleActivePortal(portalId, isActive) },
                    onLoadDemoPortal = { viewModel.addDemoPortal() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Overlay: Settings
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.SETTINGS,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xE605050A))) {
                SettingsScreen(
                    channelCount = viewModel.channels.value.size, categoryCount = viewModel.categories.value.size,
                    showClockOverlay = showClockOverlay,
                    onToggleClockOverlay = { viewModel.setShowClockOverlay(!showClockOverlay) }
                )
            }
        }
    }
}
