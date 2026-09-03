package com.tvmime.tv.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvmime.theme.DesignSystemTokens

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val bgMain = Color(DesignSystemTokens.Colors.Background)
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
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(crimson),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TV",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TVMIME",
                            color = textPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0x33E50914), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0x66E50914), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v1.0.0 (Build 1)",
                                color = crimsonBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "High-Performance, Zero-OOM IPTV Streaming Player for Android TV & Fire TV",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Creator & Project Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF262634), RoundedCornerShape(14.dp))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "PROJECT METADATA",
                        color = crimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetaCard(
                            icon = Icons.Default.Person,
                            label = "Creator & Lead Architect",
                            value = "Faraz Ahmad",
                            accentColor = crimsonBright
                        )

                        MetaCard(
                            icon = Icons.Default.Language,
                            label = "Cloud Admin Portal",
                            value = "https://tvmime.vercel.app",
                            accentColor = Color(0xFF10B981)
                        )

                        MetaCard(
                            icon = Icons.Default.Download,
                            label = "Direct Firestick Downloader",
                            value = "tvmime.vercel.app/tv.apk",
                            accentColor = Color(0xFF3B82F6)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetaCard(
                            icon = Icons.Default.Code,
                            label = "GitHub Repository",
                            value = "github.com/Fragger7/tvmime",
                            accentColor = Color(0xFFA855F7)
                        )

                        MetaCard(
                            icon = Icons.Default.Speed,
                            label = "Memory & Rendering",
                            value = "60 FPS • Zero-OOM Engine",
                            accentColor = Color(0xFFF59E0B)
                        )

                        MetaCard(
                            icon = Icons.Default.Shield,
                            label = "Network Evasion",
                            value = "IPTVSmartersPro/1.1.1 Spoof",
                            accentColor = Color(0xFF06B6D4)
                        )
                    }
                }
            }

            // Architecture Highlights Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF262634), RoundedCornerShape(14.dp))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "TECHNICAL ARCHITECTURE",
                        color = crimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "• Kotlin Multiplatform (KMP): Shared networking, catalog parsing, and database logic future-proofed for Apple TV (tvOS).\n" +
                               "• Low-Level Token Stream Parser: Bypasses JSON object deserialization into RAM, streaming 100,000+ IPTV channels directly into SQLite.\n" +
                               "• AndroidX Media3 (ExoPlayer): Hardware video decoder preference with automatic recovery for HTTP 456/884 stream drops.\n" +
                               "• Continuous Delivery: Automated compilation with immutable keystore signing for in-place OTA upgrades.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaCard(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier.width(240.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = Color(0xFF9CA3AF),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
