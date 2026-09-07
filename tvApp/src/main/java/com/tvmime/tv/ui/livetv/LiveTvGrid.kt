package com.tvmime.tv.ui.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.tvmime.db.entity.CategoryEntity
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.tv.ui.common.TvMimeTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvGrid(
    categories: List<CategoryEntity>,
    channels: List<ChannelEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (CategoryEntity) -> Unit,
    onChannelClick: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            
    ) {
        // Left Column: Categories
        TvLazyColumn(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(vertical = 24.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp, top = 16.dp)
                )
            }
            
            items(categories, key = { it.id }) { category ->
                val isSelected = category.categoryId == selectedCategoryId
                Surface(
                    onClick = { onCategorySelected(category) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.onBackground,
                        focusedContentColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = category.categoryName,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Center Grid: Channels
        Box(modifier = Modifier.fillMaxSize().padding(end = 24.dp)) {
            if (channels.isEmpty()) {
                Text(
                    text = "No channels in this category.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(4),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(channels, key = { it.id }) { channel ->
                        Surface(
                            onClick = { onChannelClick(channel) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                focusedContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f) // Wide cards
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Channel Name and Number
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${channel.num}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                        color = LocalContentColor.current.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
