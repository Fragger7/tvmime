package com.tvmime.tv.sv_ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.tvmime.tv.sv_ui.design.AppColors
import com.tvmime.tv.sv_ui.design.AppShapes
import com.tvmime.tv.sv_ui.design.LocalAppShapes
import com.tvmime.tv.sv_ui.design.LocalAppSpacing
import com.tvmime.tv.sv_ui.design.rememberAppTypography

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Brand,
    onPrimary = OnPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceElevated,
    onSurfaceVariant = AppColors.TextSecondary,
    background = AppColors.CanvasElevated,
    onBackground = AppColors.TextPrimary,
    error = AppColors.Live,
    onError = OnPrimary
)

@Composable
fun StreamVaultTheme(content: @Composable () -> Unit) {
    val typography = rememberAppTypography()
    CompositionLocalProvider(
        LocalAppSpacing provides com.tvmime.tv.sv_ui.design.AppSpacing(),
        LocalAppShapes provides AppShapes()
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = typography,
            content = content
        )
    }
}
