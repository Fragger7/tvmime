package com.tvmime.tv.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.ui.player.TvVideoPlayer

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onSelectCategory: (CategoryEntity) -> Unit,
    channels: List<ChannelEntity>,
    selectedChannel: ChannelEntity?,
    playingChannel: ChannelEntity?,
    onPlayChannel: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onToggleLastChannel: (() -> Boolean)? = null,
    modifier: Modifier = Modifier
) {
    val bgMain = Color(DesignSystemTokens.Colors.Background)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)

    // If fullscreen, the player takes the whole screen
    if (isFullscreen) {
        TvVideoPlayer(
            channel = playingChannel,
            isFullscreen = true,
            onToggleFullscreen = onToggleFullscreen,
            onToggleFavorite = onToggleFavorite,
            onToggleLastChannel = onToggleLastChannel,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(bgMain)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Category Column (Left) ---
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
        ) {
            Text(
                text = "CATEGORIES (${categories.size})",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cardBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No categories", color = textSecondary, fontSize = 12.sp)
                }
            } else {
                TvLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        CategoryCard(
                            category = cat,
                            isSelected = cat.categoryId == selectedCategory?.categoryId,
                            onClick = { onSelectCategory(cat) }
                        )
                    }
                }
            }
        }

        // --- 2. Channel Browser & Preview Panel (Right) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Preview Header (Player Preview + Live Program Info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Player Preview Box
                TvVideoPlayer(
                    channel = playingChannel,
                    isFullscreen = false,
                    onToggleFullscreen = onToggleFullscreen,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                )

                // Currently Playing Channel Detail Card
                playingChannel?.let { ch ->
                    Surface(
                        onClick = onToggleFullscreen,
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = cardBg,
                            focusedContainerColor = Color(0xFF222230)
                        ),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(crimson, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "LIVE NOW",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Text(
                                        "Channel #${ch.num}",
                                        color = textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = ch.name,
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = selectedCategory?.categoryName ?: "General",
                                    color = crimson,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // Translucent EPG Program Timeline Strip
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xAA181822), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF262632), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(crimson, RoundedCornerShape(3.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text("NOW", color = Color.White, fontWeight = FontWeight.Black, fontSize = 8.sp)
                                    }

                                    Text(
                                        text = "Live Stream • ${ch.containerExtension.uppercase()} HD",
                                        color = textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = "60 FPS",
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Press OK to Fullscreen",
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )

                                Surface(
                                    onClick = { onToggleFavorite(ch) },
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = Color.Transparent,
                                        focusedContainerColor = crimson
                                    ),
                                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        androidx.compose.material3.Icon(
                                            imageVector = if (ch.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (ch.isFavorite) crimson else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } ?: Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(cardBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a channel to play", color = textSecondary, fontSize = 13.sp)
                }
            }

            // Channel List Section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CHANNELS (${channels.size})",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (channels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cardBg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (categories.isEmpty()) "Sync your portal to download channels" else "No channels in this category",
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    TvLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(channels) { channel ->
                            ChannelCard(
                                channel = channel,
                                isPlaying = channel.id == playingChannel?.id,
                                onClick = { onPlayChannel(channel) },
                                onToggleFavorite = { onToggleFavorite(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryCard(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val crimson = Color(DesignSystemTokens.Colors.Crimson)

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0x33E50914) else Color(0xFF14141C),
            focusedContainerColor = Color(0xFF222230)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, crimson)
            )
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(crimson, CircleShape)
                )
            }

            Text(
                text = category.categoryName,
                color = if (isSelected || isFocused) Color.White else Color(0xFF9CA3AF),
                fontSize = 12.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelCard(
    channel: ChannelEntity,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val crimson = Color(DesignSystemTokens.Colors.Crimson)

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isPlaying) Color(0x33E50914) else Color(0xFF161620),
            focusedContainerColor = Color(0xFF262636)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, crimson)
            )
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Channel Number Pill
            Box(
                modifier = Modifier
                    .background(Color(0xFF222230), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${channel.num}",
                    color = Color(0xFFD1D5DB),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Channel Name
            Text(
                text = channel.name,
                color = if (isPlaying || isFocused) Color.White else Color(0xFFE5E7EB),
                fontSize = 13.sp,
                fontWeight = if (isPlaying || isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Playing Pulse Badge
            if (isPlaying) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playing",
                        tint = crimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("PLAYING", color = crimson, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }

            // Favorite Icon
            if (channel.isFavorite) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = crimson,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
