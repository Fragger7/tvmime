package com.tvmime.tv.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

@Composable
fun StatelessAppShell(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C1624), Color.Black)
                )
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // SIDE NAVIGATION RAIL
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(180.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "TVMime",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                )

                StatelessRailButton(
                    label = "Live TV",
                    isSelected = currentRoute.startsWith("PLAYER") || currentRoute == "LIVETV",
                    onClick = { onNavigate("LIVETV") }
                )
                StatelessRailButton(
                    label = "Movies",
                    isSelected = currentRoute == "MOVIES",
                    onClick = { onNavigate("MOVIES") }
                )
                StatelessRailButton(
                    label = "Series",
                    isSelected = currentRoute == "SERIES",
                    onClick = { onNavigate("SERIES") }
                )
                Spacer(modifier = Modifier.weight(1f))
                StatelessRailButton(
                    label = "Settings",
                    isSelected = currentRoute == "SETTINGS",
                    onClick = { onNavigate("SETTINGS") }
                )
            }

            // MAIN CONTENT AREA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatelessRailButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.1f else 1.0f)

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.onBackground,
            focusedContentColor = MaterialTheme.colorScheme.background,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        ),
        shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
