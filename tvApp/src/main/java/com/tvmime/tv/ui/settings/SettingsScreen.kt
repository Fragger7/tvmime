package com.tvmime.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.tvmime.tv.viewmodel.TvMainViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TvMainViewModel,
    onLogoutSuccess: () -> Unit
) {
    var selectedCategory by remember { mutableIntStateOf(0) }

    Row(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        // Master: Navigation Rail
        LazyColumn(
            modifier = Modifier
                .width(236.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    onClick = { selectedCategory = 0 },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selectedCategory == 0) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.onBackground,
                        focusedContentColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Account & Sync", modifier = Modifier.padding(16.dp))
                }
            }
            item {
                Surface(
                    onClick = { selectedCategory = 1 },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selectedCategory == 1) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.onBackground,
                        focusedContentColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Updates (OTA)", modifier = Modifier.padding(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Detail: Content Pane
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedCategory == 0) {
                Column {
                    Text("Account Management", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        viewModel.logout {
                            onLogoutSuccess()
                        }
                    }) {
                        Text("Sign Out & Clear Cache")
                    }
                }
            } else if (selectedCategory == 1) {
                var otaStatus by remember { mutableStateOf("Ready to check.") }
                val scope = rememberCoroutineScope()
                
                Column {
                    Text("System Updates", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(otaStatus)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        otaStatus = "Checking GitHub Releases..."
                        scope.launch {
                            otaStatus = com.tvmime.sync.OtaManager.checkForUpdates()
                        }
                    }) {
                        Text("Check for Updates Now")
                    }
                }
            }
        }
    }
}
