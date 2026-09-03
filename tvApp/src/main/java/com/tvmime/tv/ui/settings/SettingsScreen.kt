package com.tvmime.tv.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.update.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    channelCount: Int,
    categoryCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val bgMain = Color(DesignSystemTokens.Colors.Background)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)

    var updateStatus by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgMain)
            .padding(32.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "SETTINGS & SYSTEM INFO",
                        color = textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "TVMime App Diagnostics & In-Place OTA Updates",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Diagnostic Info Card
            Surface(
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = cardBg,
                    focusedContainerColor = Color(0xFF222230)
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SYSTEM STATUS",
                        color = crimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(label = "App Version", value = "v1.0.0 (Build 1)")
                        InfoItem(label = "Cached Channels", value = "$channelCount channels")
                        InfoItem(label = "Cached Categories", value = "$categoryCount categories")
                        InfoItem(label = "Hardware Decoder", value = "Media3 (Preferred)")
                    }
                }
            }

            // OTA Update Card
            Surface(
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = cardBg,
                    focusedContainerColor = Color(0xFF222230)
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "OTA CONTINUOUS DELIVERY",
                        color = crimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Download and apply signed updates directly from GitHub Releases without losing settings.",
                        color = textSecondary,
                        fontSize = 12.sp
                    )

                    if (updateStatus != null) {
                        Text(
                            text = updateStatus ?: "",
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        onClick = {
                            isCheckingUpdate = true
                            updateStatus = "Checking GitHub release server..."
                            coroutineScope.launch {
                                val manager = UpdateManager(context)
                                val available = manager.checkUpdateAvailable()
                                if (available) {
                                    updateStatus = "Update found! Downloading APK..."
                                    manager.downloadAndInstall()
                                } else {
                                    updateStatus = "TVMime is currently up to date (v1.0.0)"
                                }
                                isCheckingUpdate = false
                            }
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = crimson,
                            focusedContainerColor = Color(0xFFFF1E27)
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isCheckingUpdate) "Checking Server..." else "Check for Updates Now",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        androidx.compose.material3.Text(
            text = label,
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp
        )
        androidx.compose.material3.Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
