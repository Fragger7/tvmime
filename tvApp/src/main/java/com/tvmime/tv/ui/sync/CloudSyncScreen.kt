package com.tvmime.tv.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.tvmime.db.entity.PortalEntity
import com.tvmime.repository.SyncProgress
import com.tvmime.theme.DesignSystemTokens
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    activePortal: PortalEntity?,
    syncProgress: SyncProgress,
    onSyncCurrentPortal: () -> Unit,
    onLoadDemoPortal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgMain = Color(DesignSystemTokens.Colors.Background)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)

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
                        .background(Color(0xFF10B981), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Cloud",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "CLOUD PORTAL SYNC",
                        color = textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Manage and sync IPTV provider credentials from tvmime.vercel.app",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Active Portal Card
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ACTIVE PORTAL",
                        color = crimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (activePortal != null) {
                        Text(
                            text = activePortal.name,
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Server: ${activePortal.serverUrl}",
                            color = textSecondary,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "Username: ${activePortal.username}",
                            color = textSecondary,
                            fontSize = 13.sp
                        )

                        val syncTime = if (activePortal.lastSyncedAt > 0) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(activePortal.lastSyncedAt))
                        } else {
                            "Never"
                        }
                        Text(
                            text = "Last Synced: $syncTime",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "No Active Portal Configured",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Load the demo portal below or link your account at tvmime.vercel.app",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Sync Progress Status Banner
            when (syncProgress) {
                is SyncProgress.Authenticating -> {
                    StatusBanner(
                        title = "Authenticating with IPTV Server...",
                        subtitle = "Connecting as ${syncProgress.portalName}",
                        color = Color(0xFF3B82F6),
                        isLoading = true
                    )
                }
                is SyncProgress.SyncingCategories -> {
                    StatusBanner(
                        title = "Syncing Categories...",
                        subtitle = "Fetching ${syncProgress.type.name} category groups",
                        color = Color(0xFF3B82F6),
                        isLoading = true
                    )
                }
                is SyncProgress.SyncingChannels -> {
                    StatusBanner(
                        title = "Streaming Catalog Channels...",
                        subtitle = "Ingested ${syncProgress.count} channels into local database",
                        color = Color(0xFF10B981),
                        isLoading = true
                    )
                }
                is SyncProgress.Success -> {
                    StatusBanner(
                        title = "Catalog Successfully Cached!",
                        subtitle = "All channels and categories are ready for 60fps playback",
                        color = Color(0xFF10B981),
                        isLoading = false
                    )
                }
                is SyncProgress.Error -> {
                    StatusBanner(
                        title = "Sync Failed",
                        subtitle = syncProgress.message,
                        color = crimson,
                        isLoading = false
                    )
                }
                is SyncProgress.Idle -> {}
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    onClick = onSyncCurrentPortal,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = crimson,
                        focusedContainerColor = Color(0xFFFF1E27)
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sync Active Portal Now",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Surface(
                    onClick = onLoadDemoPortal,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = cardBg,
                        focusedContainerColor = Color(0xFF262638)
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Demo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Load Demo Xtream Portal",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    title: String,
    subtitle: String,
    color: Color,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = color,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = if (color == Color(0xFF10B981)) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                androidx.compose.material3.Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                androidx.compose.material3.Text(
                    text = subtitle,
                    color = Color(0xFFD1D5DB),
                    fontSize = 12.sp
                )
            }
        }
    }
}
