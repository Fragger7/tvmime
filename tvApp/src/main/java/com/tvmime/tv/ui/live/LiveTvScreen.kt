package com.tvmime.tv.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tvmime.db.entity.PortalEntity
import com.tvmime.theme.DesignSystemTokens

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onSelectCategory: (CategoryEntity) -> Unit,
    onHideCategory: (CategoryEntity) -> Unit = {},
    channels: List<ChannelEntity>,
    selectedChannel: ChannelEntity?,
    playingChannel: ChannelEntity?,
    onPlayChannel: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    isFullscreen: Boolean, // Kept for signature compatibility but ignored
    onToggleFullscreen: () -> Unit,
    onToggleLastChannel: (() -> Boolean)? = null,
    activePortal: PortalEntity? = null,
    showClockOverlay: Boolean = true,
    autoHideOsdSeconds: Int = 5,
    enableLastChannelZap: Boolean = true,
    onSyncPortal: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
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
                Surface(
                    onClick = { onSyncPortal?.invoke() },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = cardBg,
                        focusedContainerColor = Color(0xFF222232)
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = crimson, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No Categories", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Click to sync", color = textSecondary, fontSize = 10.sp)
                    }
                }
            } else {
                TvLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        CategoryCard(
                            category = cat,
                            isSelected = cat.categoryId == selectedCategory?.categoryId,
                            onClick = { onSelectCategory(cat) },
                            onLongClick = { onHideCategory(cat) }
                        )
                    }
                }
            }
        }

        // --- 2. Channel Column (Right) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Text(
                text = "CHANNELS (${channels.size})",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            if (channels.isEmpty()) {
                Surface(
                    onClick = { onSyncPortal?.invoke() },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = cardBg,
                        focusedContainerColor = Color(0xFF222232)
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = crimson, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No channels loaded yet", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Click to sync active portal channels", color = textSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                TvLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(channels, key = { it.id }) { channel ->
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryCard(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0xFF262638) else Color(0x80181822),
            focusedContainerColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.categoryName,
                color = if (isFocused) Color.Black else (if (isSelected) Color.White else Color(0xFFD1D5DB)),
                fontSize = 14.sp,
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
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit
) {
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isPlaying) Color(0xFF262638) else Color(0x80181822),
            focusedContainerColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Channel Number Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isFocused) Color(0xFFE5E7EB) else Color(0xFF1E1E2C),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${channel.num}",
                        color = if (isFocused) Color.Black else Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Channel Name
                Text(
                    text = channel.name,
                    color = if (isFocused) Color.Black else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Icons Right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(crimson, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Playing",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
