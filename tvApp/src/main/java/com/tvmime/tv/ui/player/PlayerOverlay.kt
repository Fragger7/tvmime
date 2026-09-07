package com.tvmime.tv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.tvmime.tv.player.EngineState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerOverlay(
    streamUrl: String,
    engineState: EngineState,
    modifier: Modifier = Modifier
) {
    var isControlsVisible by remember { mutableStateOf(true) }

    // Auto-hide controls when playing
    LaunchedEffect(engineState) {
        if (engineState == EngineState.PLAYING) {
            kotlinx.coroutines.delay(3000)
            isControlsVisible = false
        } else {
            isControlsVisible = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 48.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {},
                        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Red),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp))
                    ) {
                        Text("LIVE", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    
                    Text(
                        text = engineState.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (engineState == EngineState.PLAYING) Color.Green else Color.Yellow
                    )
                }

                // Bottom Bar
                Surface(
                    onClick = { isControlsVisible = !isControlsVisible },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0xFF0C1624).copy(alpha = 0.92f),
                        focusedContainerColor = Color(0xFF0C1624),
                        focusedContentColor = Color.White
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier.padding(48.dp)
                    ) {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = streamUrl.substringAfterLast("/"),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
