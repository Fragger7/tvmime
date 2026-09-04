package com.tvmime.tv.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvmime.db.entity.ChannelEntity
import com.tvmime.db.entity.EpgProgramEntity
import com.tvmime.theme.DesignSystemTokens
import kotlinx.coroutines.delay

@Composable
fun TvHudOverlay(
    channel: ChannelEntity?,
    epgPrograms: List<EpgProgramEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    
    // Auto-hide HUD after 5 seconds
    LaunchedEffect(channel) {
        delay(5000)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Color(0xD905050A)) // Translucent dark glass
            .padding(horizontal = 40.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        if (channel == null) {
            Text("No Channel Playing", color = Color.White, fontSize = 18.sp)
            return@Box
        }

        // Find current program
        val now = System.currentTimeMillis()
        val currentProgram = epgPrograms.find { 
            it.epgChannelId == channel.epgChannelId && now in it.startEpoch..it.endEpoch 
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Channel Number Box
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF1E1E2C), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${channel.num}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                // Channel Name
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (currentProgram != null) {
                    Text(
                        text = currentProgram.title,
                        color = Color(0xFFD1D5DB),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Progress Bar
                    val duration = currentProgram.endEpoch - currentProgram.startEpoch
                    val elapsed = now - currentProgram.startEpoch
                    val progress = (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF374151), RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier.fillMaxWidth(progress).height(4.dp).background(crimson, RoundedCornerShape(2.dp)))
                    }
                } else {
                    Text(
                        text = "No EPG Data Available",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Stream Stats
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Badge("FHD 1080p")
                Badge("60 FPS")
                Badge("AAC 2.0")
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF262636), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = Color(0xFFD1D5DB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
