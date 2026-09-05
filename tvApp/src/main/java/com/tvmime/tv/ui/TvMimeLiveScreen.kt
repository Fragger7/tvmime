package com.tvmime.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvmime.tv.ui.guide.GuideGrid
import com.tvmime.tv.ui.guide.GuideTimelineChannel
import com.tvmime.tv.ui.guide.GuideTimelineProgramme
import com.tvmime.tv.ui.player.LiveProgrammeInfoOverlay
import com.tvmime.tv.viewmodel.TvMainViewModel
import java.util.TimeZone

@Composable
fun TvMimeLiveScreen(viewModel: TvMainViewModel) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val epgPrograms by viewModel.epgPrograms.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    
    val guideChannels = remember(channels, epgPrograms) {
        channels.map { channel ->
            val channelEpg = epgPrograms.filter { it.epgChannelId == channel.epgChannelId }
            GuideTimelineChannel(
                sourceId = channel.portalId,
                sourceName = "Portal",
                sourcePriority = 0,
                id = channel.id,
                name = channel.name,
                groupTitle = channel.categoryId,
                logoUrl = channel.streamIcon,
                playlistOrder = 0,
                programmes = channelEpg.map { epg ->
                    GuideTimelineProgramme(
                        id = epg.id,
                        title = epg.title,
                        subtitle = null,
                        description = epg.description,
                        categories = emptyList(),
                        startEpochMillis = epg.startEpoch * 1000,
                        stopEpochMillis = epg.endEpoch * 1000
                    )
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (overlayState == com.tvmime.tv.viewmodel.TvOverlayState.GUIDE) {
            val listState = rememberLazyListState()
            val focusRequester = remember { FocusRequester() }
            
            GuideGrid(
                channels = guideChannels,
                windowStart = System.currentTimeMillis() - 3600000,
                windowEnd = System.currentTimeMillis() + 7200000,
                now = System.currentTimeMillis(),
                timeZoneId = TimeZone.getDefault().id,
                selection = null,
                listState = listState,
                initialFocusIndex = 0,
                firstFocusRequester = focusRequester,
                returnFocusRequester = focusRequester,
                onOpenGroupRail = { },
                pagedFocusRequester = focusRequester,
                pagedFocusAtEnd = false,
                onSelection = { _, _ -> },
                onPlay = { _, _ -> },
                onPageForward = { },
                onPageBack = { },
                onTransportKey = { false },
                modifier = Modifier.fillMaxSize()
            )
        } else if (overlayState == com.tvmime.tv.viewmodel.TvOverlayState.HUD) {
            LiveProgrammeInfoOverlay(
                channel = guideChannels.firstOrNull(),
                streamName = "Live Stream",
                nowEpochMillis = System.currentTimeMillis(),
                timeZoneId = TimeZone.getDefault().id,
                metadata = null,
                aspectModeLabel = "FIT",
                audioTrackLabel = "Default",
                subtitleTrackLabel = "None",
                statsVisible = false,
                onBack = { viewModel.setOverlayState(com.tvmime.tv.viewmodel.TvOverlayState.HIDDEN) },
                onCycleAspectMode = { },
                onCycleAudioTrack = { },
                onCycleSubtitleTrack = { },
                onOpenChannelBrowser = { },
                onToggleStats = { },
                onOpenQuickActions = { },
                externalPlayerBusy = false,
                onOpenExternal = { },
                visibilityKey = 0L,
                enabled = true
            )
        }
    }
}
