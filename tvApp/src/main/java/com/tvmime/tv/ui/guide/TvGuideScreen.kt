package com.tvmime.tv.ui.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dvr
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.db.entity.EpgProgramEntity
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.ui.player.TvVideoPlayer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvGuideScreen(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onSelectCategory: (CategoryEntity?) -> Unit,
    channels: List<ChannelEntity>,
    selectedChannel: ChannelEntity?,
    playingChannel: ChannelEntity?,
    onPlayChannel: (ChannelEntity) -> Unit,
    onPlayCatchup: (ChannelEntity, EpgProgramEntity) -> Unit,
    onToggleFullscreen: () -> Unit,
    epgPrograms: List<EpgProgramEntity>,
    onHideCategory: (CategoryEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgMain = Color.Transparent
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val crimsonBright = Color(DesignSystemTokens.Colors.CrimsonBright)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)
    val borderCol = Color(DesignSystemTokens.Colors.Border)

    // Current Time Reference
    val currentTimeMillis = remember { System.currentTimeMillis() }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE, MMMM d • h:mm a", Locale.getDefault()) }

    // Generate 30-min Time Window Slots (past 30 mins to 3 hours ahead)
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = currentTimeMillis } }
    val startMinute = (calendar.get(Calendar.MINUTE) / 30) * 30
    calendar.set(Calendar.MINUTE, startMinute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val windowBaseTime = calendar.timeInMillis

    val timeSlots = remember(windowBaseTime) {
        (0..5).map { index ->
            val slotTime = windowBaseTime + index * 30 * 60 * 1000L
            timeFormat.format(Date(slotTime))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgMain)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Top Bar: Live Clock, Categories & Mini Preview Player ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Header & Current Clock
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33E50914))
                            .border(1.dp, Color(0x66E50914), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EPG GRID",
                            color = crimsonBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = fullDateFormat.format(Date(currentTimeMillis)),
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Electronic Program Guide",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                // Category Selector Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    item {
                        Surface(
                            onClick = { onSelectCategory(null) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (selectedCategory == null) crimson else cardBg,
                                focusedContainerColor = crimsonBright
                            ),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(
                                    text = "All Channels (${channels.size})",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    items(categories, key = { it.id }) { cat ->
                        val isSelected = cat.categoryId == selectedCategory?.categoryId
                        Surface(
                            onClick = { onSelectCategory(cat) },
                            onLongClick = { onHideCategory(cat) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) crimson else cardBg,
                                focusedContainerColor = crimsonBright
                            ),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(
                                    text = cat.categoryName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Right: Mini Preview Player Box
            Box(
                modifier = Modifier
                    .width(196.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, crimson, RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                // Just a transparent hole to let the background player show through
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Transparent)
                )
            }
        }

        // --- 2. Horizontal Time Axis Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF14141E), RoundedCornerShape(8.dp))
                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Column Header
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = "CHANNELS",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Time Slots Header
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                timeSlots.forEach { slot ->
                    Text(
                        text = slot,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- 3. Vertical Channels & Program Matrix ---
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No channels found in this category", color = textSecondary, fontSize = 13.sp)
            }
        } else {
            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    val channelPrograms = remember(channel.epgChannelId, epgPrograms) {
                        epgPrograms.filter { it.epgChannelId == channel.epgChannelId }
                    }

                    val isPlaying = channel.id == playingChannel?.id

                    ChannelGuideRow(
                        channel = channel,
                        programs = channelPrograms,
                        isPlaying = isPlaying,
                        currentTimeMillis = currentTimeMillis,
                        timeFormat = timeFormat,
                        onChannelClick = {
                            if (isPlaying) {
                                onToggleFullscreen()
                            } else {
                                onPlayChannel(channel)
                            }
                        },
                        onPlayCatchup = { program ->
                            onPlayCatchup(channel, program)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelGuideRow(
    channel: ChannelEntity,
    programs: List<EpgProgramEntity>,
    isPlaying: Boolean,
    currentTimeMillis: Long,
    timeFormat: SimpleDateFormat,
    onChannelClick: () -> Unit,
    onPlayCatchup: (EpgProgramEntity) -> Unit
) {
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val crimsonBright = Color(DesignSystemTokens.Colors.CrimsonBright)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)
    val borderCol = Color(DesignSystemTokens.Colors.Border)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Channel Info Surface (Left) ---
        Surface(
            onClick = onChannelClick,
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (isPlaying) Color(0xFF261214) else cardBg,
                focusedContainerColor = crimson
            ),
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .border(
                    width = 1.dp,
                    color = if (isPlaying) crimson else borderCol,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Channel Number Pill
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPlaying) crimson else Color(0xFF222230)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${channel.num}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Channel Name & Live Indicator
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isPlaying) {
                        Text(
                            text = "PLAYING NOW",
                            color = crimsonBright,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                if (channel.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = crimsonBright,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // --- Program Blocks Timeline (Right) ---
        if (programs.isEmpty()) {
            // Default Placeholder Program Block if no EPG sync
            Surface(
                onClick = onChannelClick,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xFF14141E),
                    focusedContainerColor = Color(0xFF20202F)
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3310B981))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE BROADCAST",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${channel.name} 24/7 Live Stream",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Press OK to Watch",
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            // Horizontal sequence of EPG program cards
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                programs.forEach { program ->
                    val isLiveNow = currentTimeMillis in program.startEpoch..program.endEpoch
                    val isPast = currentTimeMillis > program.endEpoch
                    val canCatchup = isPast && channel.hasArchive

                    Surface(
                        onClick = { 
                            if (canCatchup) {
                                onPlayCatchup(program)
                            } else if (isLiveNow) {
                                onChannelClick()
                            }
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isLiveNow) Color(0xFF1B1B26) else Color(0xFF12121A),
                            focusedContainerColor = if (isLiveNow) Color(0xFF2A2A3C) else Color(0xFF1E1E28)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = 1.dp,
                                color = if (isLiveNow) crimson else if (canCatchup) Color(0xFFE50914).copy(alpha=0.3f) else borderCol,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = program.title,
                                    color = if (canCatchup) Color.White else textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isLiveNow) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(crimson)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ON NOW",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                } else if (canCatchup) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Catch-Up",
                                        tint = crimsonBright,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val startTime = timeFormat.format(Date(program.startEpoch))
                                val endTime = timeFormat.format(Date(program.endEpoch))

                                Text(
                                    text = "$startTime - $endTime",
                                    color = textSecondary,
                                    fontSize = 10.sp
                                )

                                if (isLiveNow) {
                                    val totalDuration = (program.endEpoch - program.startEpoch).coerceAtLeast(1)
                                    val elapsed = (currentTimeMillis - program.startEpoch).coerceAtLeast(0)
                                    val progress = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = crimsonBright,
                                        trackColor = Color(0xFF2E2E40),
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
