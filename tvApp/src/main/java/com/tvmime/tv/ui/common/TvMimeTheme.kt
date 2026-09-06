package com.tvmime.tv.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val DeepBlack = Color(0xFF0D0D0D)
val CrimsonRed = Color(0xFFDC143C)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceTranslucent = Color(0x99000000)

@OptIn(ExperimentalTvMaterial3Api::class)
private val TvMimeColorScheme = darkColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    background = DeepBlack,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceTranslucent
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMimeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvMimeColorScheme,
        content = content
    )
}
