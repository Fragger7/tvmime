package com.tvmime.tv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.ui.live.LiveTvScreen
import com.tvmime.tv.ui.navigation.TvNavigationDrawer
import com.tvmime.tv.ui.player.TvVideoPlayer
import com.tvmime.tv.ui.settings.SettingsScreen
import com.tvmime.tv.viewmodel.TvMainViewModel
import com.tvmime.tv.viewmodel.TvNavDestination
import com.tvmime.tv.viewmodel.TvOverlayState
import com.tvmime.tv.ui.hud.TvHudOverlay
import kotlinx.coroutines.delay

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
    val bgColor = Color.Black
    val activePortal by viewModel.activePortal.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val playingChannel by viewModel.playingChannel.collectAsStateWithLifecycle()
    val epgPrograms by viewModel.epgPrograms.collectAsStateWithLifecycle()
    val showClockOverlay by viewModel.showClockOverlay.collectAsStateWithLifecycle()
    val autoHideOsdSeconds by viewModel.autoHideOsdSeconds.collectAsStateWithLifecycle()
    val enableLastChannelZap by viewModel.enableLastChannelZap.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()

    val rootFocusRequester = remember { FocusRequester() }
    val channelListFocusRequester = remember { FocusRequester() }
    val mainMenuFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val guideFocusRequester = remember { FocusRequester() }

    // First-Run Wizard
    var dismissedOnboarding by remember { mutableStateOf(false) }
    if (activePortal == null && !dismissedOnboarding) {
        com.tvmime.tv.ui.onboarding.OnboardingScreen(
            viewModel = viewModel,
            onComplete = { 
                 dismissedOnboarding = true 
                 viewModel.syncCurrentPortal()
                 viewModel.setOverlayState(TvOverlayState.CHANNEL_LIST)
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Handle Back Button based on Overlay State
    BackHandler(enabled = overlayState != TvOverlayState.HIDDEN) {
        if (overlayState == TvOverlayState.MAIN_MENU) {
            viewModel.setOverlayState(TvOverlayState.CHANNEL_LIST)
        } else {
            viewModel.setOverlayState(TvOverlayState.HIDDEN)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (overlayState == TvOverlayState.HIDDEN) {
                                viewModel.setOverlayState(TvOverlayState.HUD)
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (overlayState == TvOverlayState.HIDDEN) {
                                viewModel.setOverlayState(TvOverlayState.CHANNEL_LIST)
                                return@onKeyEvent true
                            } else if (overlayState == TvOverlayState.CHANNEL_LIST) {
                                viewModel.setOverlayState(TvOverlayState.MAIN_MENU)
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (overlayState == TvOverlayState.HIDDEN) {
                                viewModel.zapNext()
                                viewModel.setOverlayState(TvOverlayState.HUD)
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (overlayState == TvOverlayState.HIDDEN) {
                                viewModel.zapPrevious()
                                viewModel.setOverlayState(TvOverlayState.HUD)
                                return@onKeyEvent true
                            }
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (overlayState == TvOverlayState.HIDDEN && enableLastChannelZap) {
                                viewModel.toggleLastChannel()
                                viewModel.setOverlayState(TvOverlayState.HUD)
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_MENU -> {
                            if (overlayState == TvOverlayState.HIDDEN) {
                                viewModel.setOverlayState(TvOverlayState.SETTINGS)
                                return@onKeyEvent true
                            }
                        }
                    }
                }
                false
            }
            .focusRequester(rootFocusRequester)
            .focusable()
    ) {
        // Base Layer: Perpetual Video Player
        TvVideoPlayer(
            channel = playingChannel,
            isFullscreen = true,
            onToggleFullscreen = { },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay: HUD (Bottom Info Banner)
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.HUD,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(),
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
        ) {
            TvHudOverlay(
                channel = playingChannel,
                epgPrograms = epgPrograms,
                onDismiss = { viewModel.setOverlayState(TvOverlayState.HIDDEN) }
            )
        }

        // Overlay: MAIN MENU (Left Slide-out pane)
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.MAIN_MENU,
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(),
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterStart)
        ) {
            TvNavigationDrawer(
                currentDestination = currentDestination,
                onDestinationSelected = { dest ->
                    viewModel.navigateTo(dest)
                    when (dest) {
                        TvNavDestination.LIVE_TV, TvNavDestination.FAVORITES -> viewModel.setOverlayState(TvOverlayState.CHANNEL_LIST)
                        TvNavDestination.TV_GUIDE -> viewModel.setOverlayState(TvOverlayState.GUIDE)
                        TvNavDestination.MOVIES, TvNavDestination.SERIES -> viewModel.setOverlayState(TvOverlayState.VOD)
                        TvNavDestination.CLOUD_SYNC -> viewModel.setOverlayState(TvOverlayState.CLOUD_SYNC)
                        TvNavDestination.SETTINGS -> viewModel.setOverlayState(TvOverlayState.SETTINGS)
                        else -> viewModel.setOverlayState(TvOverlayState.HIDDEN)
                    }
                },
                modifier = Modifier.background(Color(0xD90A0A10)).focusRequester(mainMenuFocusRequester)
            )
        }

        // Overlay: Channel List (Left Side Glass Pane)
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.CHANNEL_LIST,
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(),
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.55f)
                    .background(Color(0xD90A0A10))
                    .focusProperties {
                        // Prevent focus from escaping to the right
                    }
            ) {
                LiveTvScreen(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onHideCategory = { viewModel.toggleCategoryVisibility(it, true) },
                    channels = channels,
                    selectedChannel = selectedChannel,
                    playingChannel = playingChannel,
                    onPlayChannel = { 
                        viewModel.playChannel(it)
                        viewModel.setOverlayState(TvOverlayState.HIDDEN)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    isFullscreen = false,
                    onToggleFullscreen = { viewModel.setOverlayState(TvOverlayState.HIDDEN) },
                    onToggleLastChannel = { viewModel.toggleLastChannel() },
                    activePortal = activePortal,
                    showClockOverlay = showClockOverlay,
                    autoHideOsdSeconds = autoHideOsdSeconds,
                    enableLastChannelZap = enableLastChannelZap,
                    modifier = Modifier.fillMaxSize().focusRequester(channelListFocusRequester)
                )
            }
        }

        // Overlay: TV Guide
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.GUIDE,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE605050A))
            ) {
                com.tvmime.tv.ui.guide.TvGuideScreen(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onHideCategory = { viewModel.toggleCategoryVisibility(it, true) },
                    channels = channels,
                    selectedChannel = selectedChannel,
                    playingChannel = playingChannel,
                    onPlayChannel = { 
                        viewModel.playChannel(it)
                        viewModel.setOverlayState(TvOverlayState.HIDDEN)
                    },
                    onToggleFullscreen = { viewModel.setOverlayState(TvOverlayState.HIDDEN) },
                    epgPrograms = epgPrograms,
                    modifier = Modifier.fillMaxSize().focusRequester(guideFocusRequester)
                )
            }
        }

        // Overlay: VOD
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.VOD,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xE605050A))) {
                com.tvmime.tv.ui.vod.VodScreen(
                    title = if (currentDestination == TvNavDestination.MOVIES) "Movies" else "TV Series",
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    items = channels,
                    onPlayItem = { item ->
                        viewModel.playChannel(item)
                        viewModel.setOverlayState(TvOverlayState.HIDDEN)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    modifier = Modifier.fillMaxSize() // Usually VOD has its own focus internally
                )
            }
        }
        
        // Overlay: Cloud Sync
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.CLOUD_SYNC,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xE605050A))) {
                CloudSyncScreen(
                    activePortal = activePortal,
                    syncProgress = syncProgress,
                    onSyncCurrentPortal = { viewModel.syncCurrentPortal() },
                    onLoadDemoPortal = { viewModel.addDemoPortal() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Overlay: Settings
        AnimatedVisibility(
            visible = overlayState == TvOverlayState.SETTINGS,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(),
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .background(Color(0xFA05050A))
            ) {
                SettingsScreen(
                    channelCount = channels.size,
                    categoryCount = categories.size,
                    showClockOverlay = showClockOverlay,
                    autoHideOsdSeconds = autoHideOsdSeconds,
                    enableLastChannelZap = enableLastChannelZap,
                    onToggleClockOverlay = { viewModel.setShowClockOverlay(!showClockOverlay) },
                    onChangeAutoHideOsdSeconds = { viewModel.setAutoHideOsdSeconds(it) },
                    onToggleLastChannelZap = { viewModel.setEnableLastChannelZap(!enableLastChannelZap) },
                    modifier = Modifier.fillMaxSize().focusRequester(settingsFocusRequester)
                )
            }
        }
    }

    LaunchedEffect(overlayState) {
        delay(100) // Small delay to let AnimatedVisibility mount the nodes
        try {
            when (overlayState) {
                TvOverlayState.HIDDEN -> rootFocusRequester.requestFocus()
                TvOverlayState.CHANNEL_LIST -> channelListFocusRequester.requestFocus()
                TvOverlayState.MAIN_MENU -> mainMenuFocusRequester.requestFocus()
                TvOverlayState.SETTINGS -> settingsFocusRequester.requestFocus()
                TvOverlayState.GUIDE -> guideFocusRequester.requestFocus()
                else -> {}
            }
        } catch (e: Exception) {
            // Ignore if not mountable yet
        }
    }
}
