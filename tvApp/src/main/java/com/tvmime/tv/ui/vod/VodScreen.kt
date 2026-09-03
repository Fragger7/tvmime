package com.tvmime.tv.ui.vod

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.theme.DesignSystemTokens

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VodScreen(
    title: String,
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onSelectCategory: (CategoryEntity) -> Unit,
    items: List<ChannelEntity>,
    onPlayItem: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgMain = Color(DesignSystemTokens.Colors.Background)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)

    // Hero Billboard item (either first item or featured)
    val heroItem = remember(items) { items.firstOrNull() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgMain)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Hero Spotlight Billboard ---
        if (heroItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1C0A0D),
                                Color(0xFF14141E),
                                Color(0xFF0C0C12)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF261820), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.65f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(crimson, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FEATURED ${title.uppercase()}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "4K ULTRA HD • 5.1 AUDIO",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = heroItem.name,
                            color = textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Stream on-demand with ultra-low latency hardware acceleration.",
                            color = textSecondary,
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            onClick = { onPlayItem(heroItem) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = crimson,
                                focusedContainerColor = Color(0xFFFF1E27)
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Watch Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Surface(
                            onClick = { onToggleFavorite(heroItem) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = cardBg,
                                focusedContainerColor = Color(0xFF262638)
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    if (heroItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (heroItem.isFavorite) crimson else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Favorite", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Category Selector Chips ---
        if (categories.isNotEmpty()) {
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = cat.categoryId == selectedCategory?.categoryId
                    Surface(
                        onClick = { onSelectCategory(cat) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) crimson else Color(0xFF181824),
                            focusedContainerColor = Color(0xFF26263A)
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = androidx.compose.foundation.BorderStroke(2.dp, crimson)
                            )
                        ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.categoryName,
                                color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Movie Poster Grid ---
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No titles available. Sync portal to download ${title.lowercase()} catalog.",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(minSize = 140.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    VodPosterCard(
                        item = item,
                        onClick = { onPlayItem(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VodPosterCard(
    item: ChannelEntity,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val cardBg = Color(DesignSystemTokens.Colors.Card)

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = cardBg,
            focusedContainerColor = Color(0xFF222232)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, crimson)
            )
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        modifier = Modifier
            .width(140.dp)
            .height(210.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Poster Art Placeholder (Sleek dark gradient with cinema icon)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF221418),
                                Color(0xFF14141E)
                            )
                        )
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isFocused) crimson else Color(0x66FFFFFF),
                    modifier = Modifier.size(32.dp)
                )

                // HD Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("HD", color = Color(0xFFE5E7EB), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Title & Container Type
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.name,
                    color = if (isFocused) Color.White else Color(0xFFD1D5DB),
                    fontSize = 11.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.containerExtension.uppercase(),
                    color = Color(0xFF9CA3AF),
                    fontSize = 9.sp
                )
            }
        }
    }
}
