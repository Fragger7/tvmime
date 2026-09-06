package com.tvmime.tv.ui.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tvmime.tv.viewmodel.TvMainViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    viewModel: TvMainViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryId.collectAsStateWithLifecycle()

    // A mock button to trigger the massive ingestion engine for testing
    // In production, this happens silently in the background
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        Row(modifier = Modifier.fillMaxSize()) {
            // 1. Category Rail (Left Side)
            TvLazyColumn(
                modifier = Modifier.width(250.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category.categoryId == selectedCategory
                    Surface(
                        onClick = { viewModel.selectCategory(category.categoryId) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                            Text(text = category.categoryName, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // 2. Channel List (Right Side)
            TvLazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (channels.isEmpty()) {
                    item {
                        Surface(
                            onClick = { 
                                // Mock a small test M3U to prove the engine works
                                viewModel.triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                            },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Click to trigger V3 Mass Ingestion Engine (Test M3U)")
                            }
                        }
                    }
                }
                
                items(channels) { channel ->
                    Surface(
                        onClick = { viewModel.playChannel(channel) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${channel.num}", modifier = Modifier.width(50.dp))
                            Text(text = channel.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
