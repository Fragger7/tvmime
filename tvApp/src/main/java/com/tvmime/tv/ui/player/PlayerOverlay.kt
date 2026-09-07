package com.tvmime.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvmime.tv.player.EngineState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerOverlay(
    streamUrl: String,
    engineState: EngineState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Now Playing: $streamUrl",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            
            Text(
                text = engineState.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (engineState == EngineState.PLAYING) Color.Green else Color.Yellow
            )
        }
    }
}
