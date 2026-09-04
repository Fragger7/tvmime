package com.tvmime.tv.ui.settings

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.*
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.update.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    channelCount: Int,
    categoryCount: Int,
    showClockOverlay: Boolean = true,
    autoHideOsdSeconds: Int = 5,
    enableLastChannelZap: Boolean = true,
    onToggleClockOverlay: () -> Unit = {},
    onChangeAutoHideOsdSeconds: (Int) -> Unit = {},
    onToggleLastChannelZap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val capabilities = remember { com.tvmime.tv.hardware.DeviceCapabilityDetector.detect(context) }

    val (versionName, versionCode) = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pInfo.versionName ?: "1.1.0"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            Pair(vName, vCode)
        } catch (e: Exception) {
            Pair("1.1.0", 2L)
        }
    }

    val bgMain = Color(DesignSystemTokens.Colors.Background)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val crimsonBright = Color(DesignSystemTokens.Colors.CrimsonBright)
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
        TvLazyColumn(
            modifier = Modifier.fillMaxWidth(0.88f),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item {
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
                        Icon(
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
                            text = "Playback preferences, stream telemetry HUD, hardware diagnostics & updates",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // System Status Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            InfoItem(label = "App Version", value = "v$versionName (Build $versionCode)")
                            InfoItem(label = "Cached Channels", value = "$channelCount channels")
                            InfoItem(label = "Cached Categories", value = "$categoryCount categories")
                            InfoItem(label = "Hardware Decoder", value = "Media3 (Preferred)")
                        }
                    }
                }
            }

            // Player & Overlay Preferences Card (Interactive TV Controls)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PLAYER & OVERLAY PREFERENCES",
                                color = crimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "D-Pad Navigable",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Setting 1: Clock Overlay Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = "Top-Right Clock Overlay",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Displays independent real-time clock pill in the upper-right corner during fullscreen playback",
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Surface(
                                onClick = onToggleClockOverlay,
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (showClockOverlay) Color(0xFF065F46) else Color(0xFF262636),
                                    focusedContainerColor = crimsonBright
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showClockOverlay) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (showClockOverlay) "ENABLED" else "DISABLED",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Setting 2: OSD Auto-Hide Duration
                        val nextTimeout = when (autoHideOsdSeconds) {
                            3 -> 5
                            5 -> 10
                            10 -> 0
                            else -> 3
                        }
                        val timeoutLabel = when (autoHideOsdSeconds) {
                            0 -> "ALWAYS ON"
                            else -> "$autoHideOsdSeconds SECONDS"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = "OSD Controls Auto-Hide Timeout",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Inactivity delay before player overlay, channel information, and bottom controls dismiss",
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Surface(
                                onClick = { onChangeAutoHideOsdSeconds(nextTimeout) },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color(0xFF1E1E2C),
                                    focusedContainerColor = crimsonBright
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = timeoutLabel,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Setting 3: Last Channel Zap Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = "D-Pad Right Last Channel Zap",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Press D-Pad Right during playback to switch back and forth between previous and current channel",
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Surface(
                                onClick = onToggleLastChannelZap,
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (enableLastChannelZap) Color(0xFF065F46) else Color(0xFF262636),
                                    focusedContainerColor = crimsonBright
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (enableLastChannelZap) "ENABLED" else "DISABLED",
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

            // Hardware & Performance Intelligence Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HARDWARE & PERFORMANCE INTELLIGENCE",
                                color = crimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Auto-Tuned for ${capabilities.model}",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoItem(label = "Device Model", value = capabilities.model)
                            InfoItem(label = "System RAM", value = "${capabilities.totalRamMb} MB")
                            InfoItem(label = "VPU Capability", value = capabilities.recommendedDecoderMode)
                            InfoItem(label = "Active Buffer", value = capabilities.recommendedBufferProfile)
                        }

                        Text(
                            text = "Optimization note: ${capabilities.recommendedBufferReason}",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // OTA Update Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    val activity = context as? Activity
                                    val updateInfo = UpdateManager.checkForUpdate(context)
                                    if (updateInfo.hasUpdate) {
                                        if (activity != null) {
                                            updateStatus = "Update found (${updateInfo.latestVersionName})! Downloading APK..."
                                            UpdateManager.downloadAndInstall(activity, updateInfo.downloadUrl)
                                        } else {
                                            updateStatus = "Update available (${updateInfo.latestVersionName})"
                                        }
                                    } else {
                                        updateStatus = "TVMime is currently up to date (v$versionName)"
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
                                Icon(
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
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
