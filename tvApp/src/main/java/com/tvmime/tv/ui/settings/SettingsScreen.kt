package com.tvmime.tv.ui.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.*
import com.tvmime.theme.DesignSystemTokens

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
    val bgMain = Color.Transparent
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val crimsonBright = Color(DesignSystemTokens.Colors.CrimsonBright)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)

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
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF262636), RoundedCornerShape(10.dp)),
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
                            text = "Settings",
                            color = textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure playback, interface, and application preferences",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // UI & Player Preferences
            item {
                SettingsSection(title = "Appearance & Overlay", cardBg = cardBg, crimson = crimson) {
                    SettingToggleRow(
                        title = "Top-Right Clock Overlay",
                        subtitle = "Show clock during fullscreen playback",
                        icon = Icons.Default.Schedule,
                        isChecked = showClockOverlay,
                        onClick = onToggleClockOverlay,
                        crimsonBright = crimsonBright
                    )
                    
                    val timeoutLabel = if (autoHideOsdSeconds == 0) "ALWAYS ON" else "$autoHideOsdSeconds SEC"
                    SettingActionRow(
                        title = "OSD Auto-Hide Timeout",
                        subtitle = "Delay before playback controls are dismissed",
                        icon = Icons.Default.Timer,
                        actionLabel = timeoutLabel,
                        onClick = { 
                            val next = when(autoHideOsdSeconds) { 3 -> 5; 5 -> 10; 10 -> 0; else -> 3 }
                            onChangeAutoHideOsdSeconds(next)
                        },
                        crimsonBright = crimsonBright
                    )
                }
            }
            
            // Remote & Navigation
            item {
                SettingsSection(title = "Remote Control", cardBg = cardBg, crimson = crimson) {
                    SettingToggleRow(
                        title = "Last Channel Zap (D-Pad Right)",
                        subtitle = "Toggle quickly between current and previous channel",
                        icon = Icons.Default.SwapHoriz,
                        isChecked = enableLastChannelZap,
                        onClick = onToggleLastChannelZap,
                        crimsonBright = crimsonBright
                    )
                }
            }

            // Sync Status
            item {
                SettingsSection(title = "Account & Library Status", cardBg = cardBg, crimson = crimson) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoStatItem(label = "Live Channels", value = "$channelCount")
                        InfoStatItem(label = "Categories", value = "$categoryCount")
                        InfoStatItem(label = "EPG Source", value = "Xtream API")
                        InfoStatItem(label = "Hardware Decoder", value = "Auto (Media3)")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    cardBg: Color,
    crimson: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = title.uppercase(),
                color = crimson,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            content()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onClick: () -> Unit,
    crimsonBright: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF9CA3AF), fontSize = 11.sp)
        }
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (isChecked) Color(0xFF065F46) else Color(0xFF262636),
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
                    imageVector = if (isChecked) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isChecked) "ON" else "OFF",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String,
    onClick: () -> Unit,
    crimsonBright: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF9CA3AF), fontSize = 11.sp)
        }
        Surface(
            onClick = onClick,
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = actionLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InfoStatItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color(0xFF9CA3AF), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
