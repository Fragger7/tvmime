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
    activePortals: List<PortalEntity>,
    allPortals: List<PortalEntity> = emptyList(),
    syncProgress: SyncProgress,
    onSyncCurrentPortal: () -> Unit,
    onRefreshCloudPortals: () -> Unit = {},
    onTogglePortal: (String, Boolean) -> Unit = { _, _ -> },
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
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    onClick = onRefreshCloudPortals,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0xFF2563EB),
                        focusedContainerColor = Color(0xFF3B82F6)
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Cloud Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Refresh Portals From Cloud",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

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
                            text = "Sync Active Portals Now",
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
                            text = "Load Demo Portal",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Portals List
            Text(
                text = "SAVED PORTALS (${allPortals.size})",
                color = crimson,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (allPortals.isEmpty() && activePortals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "No portals saved. Press 'Refresh Portals From Cloud' to load your portals from tvmime.vercel.app, or load demo portal.",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                val displayList = if (allPortals.isNotEmpty()) allPortals else activePortals
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (portal in displayList) {
                        val isCurrentActive = activePortals.any { it.id == portal.id }
                        Surface(
                            onClick = {
                                onTogglePortal(portal.id, !isCurrentActive)
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isCurrentActive) Color(0x33E50914) else cardBg,
                                focusedContainerColor = Color(0xFF2B2B3D)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = portal.name,
                                            color = textPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isCurrentActive) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${portal.serverUrl} • ${portal.username}",
                                        color = textSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = if (isCurrentActive) "Press OK to Deactivate" else "Press OK to Activate & Sync",
                                    color = if (isCurrentActive) crimson else textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
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
