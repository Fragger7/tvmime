package com.tvmime.tv.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.viewmodel.TvNavDestination

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvNavigationDrawer(
    currentDestination: TvNavDestination,
    onDestinationSelected: (TvNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgDrawer = Color.Transparent
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val crimsonBright = Color(DesignSystemTokens.Colors.CrimsonBright)
    val textMuted = Color(DesignSystemTokens.Colors.TextMuted)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)

    var isDrawerFocused by remember { mutableStateOf(false) }
    val drawerWidth by animateDpAsState(
        targetValue = if (isDrawerFocused) 220.dp else 76.dp,
        label = "drawerWidth"
    )

    Column(
        modifier = modifier
            .width(drawerWidth)
            .fillMaxHeight()
            .background(bgDrawer)
            .padding(vertical = 16.dp, horizontal = 10.dp)
            .onFocusChanged { isDrawerFocused = it.hasFocus },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // TVMime Brand Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(crimson),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "TV",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }

                if (isDrawerFocused) {
                    Column {
                        androidx.compose.material3.Text(
                            text = "TVMIME",
                            color = textPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.5.sp
                        )
                        androidx.compose.material3.Text(
                            text = "IPTV PLAYER",
                            color = crimsonBright,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Nav Items
            NavItemRow(
                icon = Icons.Default.LiveTv,
                label = TvNavDestination.LIVE_TV.label,
                isSelected = currentDestination == TvNavDestination.LIVE_TV,
                isExpanded = isDrawerFocused,
                onClick = { onDestinationSelected(TvNavDestination.LIVE_TV) }
            )

            NavItemRow(
                icon = Icons.Default.Dvr,
                label = TvNavDestination.TV_GUIDE.label,
                isSelected = currentDestination == TvNavDestination.TV_GUIDE,
                isExpanded = isDrawerFocused,
                onClick = { onDestinationSelected(TvNavDestination.TV_GUIDE) }
            )

            NavItemRow(
                icon = Icons.Default.Movie,
                label = TvNavDestination.MOVIES.label,
                isSelected = currentDestination == TvNavDestination.MOVIES,
                isExpanded = isDrawerFocused,
                onClick = { onDestinationSelected(TvNavDestination.MOVIES) }
            )

            NavItemRow(
                icon = Icons.Default.Tv,
                label = TvNavDestination.SERIES.label,
                isSelected = currentDestination == TvNavDestination.SERIES,
                isExpanded = isDrawerFocused,
                onClick = { onDestinationSelected(TvNavDestination.SERIES) }
            )

            NavItemRow(
                icon = Icons.Default.Favorite,
                label = TvNavDestination.FAVORITES.label,
                isSelected = currentDestination == TvNavDestination.FAVORITES,
                isExpanded = isDrawerFocused,
                onClick = { onDestinationSelected(TvNavDestination.FAVORITES) }
            )
        }

        // Bottom Utilities (Cloud Sync, Settings)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            NavItemRow(
                icon = Icons.Default.CloudSync,
                label = TvNavDestination.CLOUD_SYNC.label,
                isSelected = currentDestination == TvNavDestination.CLOUD_SYNC,
                isExpanded = isDrawerFocused,
                onClick = { onDestinationSelected(TvNavDestination.CLOUD_SYNC) }
            )

            NavItemRow(
                icon = Icons.Default.Settings,
                label = TvNavDestination.SETTINGS.label,
                isSelected = currentDestination == TvNavDestination.SETTINGS,
                isExpanded = isDrawerFocused,
                onClick = { onDestinationSelected(TvNavDestination.SETTINGS) }
            )

            NavItemRow(
                icon = Icons.Default.Info,
                label = TvNavDestination.ABOUT.label,
                isSelected = currentDestination == TvNavDestination.ABOUT,
                isExpanded = isDrawerFocused,
                onClick = { onDestinationSelected(TvNavDestination.ABOUT) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavItemRow(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val cardColor = Color(DesignSystemTokens.Colors.Card)
    val focusedBorderColor = Color(DesignSystemTokens.Colors.BorderFocused)

    val containerColor = when {
        isFocused -> Color(0xFF232332)
        isSelected -> Color(0x33E50914)
        else -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = Color(0xFF262638)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active Indicator indicator bar
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(crimson, RoundedCornerShape(2.dp))
                )
            }

            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected || isFocused) Color.White else Color(0xFF9CA3AF),
                modifier = Modifier.size(22.dp)
            )

            if (isExpanded) {
                Text(
                    text = label,
                    color = if (isSelected || isFocused) Color.White else Color(0xFFD1D5DB),
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }
    }
}
