package com.sipun.sonora.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SonoraColors = lightColorScheme(
    primary = SonoraRed,
    onPrimary = SonoraOnPrimary,
    primaryContainer = SonoraRed.copy(alpha = 0.12f),
    onPrimaryContainer = SonoraRed,
    background = SonoraBackground,
    onBackground = SonoraOnBackground,
    surface = SonoraSurface,
    onSurface = SonoraOnSurface,
    surfaceVariant = SonoraSurfaceVariant,
    onSurfaceVariant = SonoraOnSurfaceVariant,
    outline = SonoraOutline,
)

@Composable
fun SonoraTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SonoraColors,
        typography = SonoraTypography,
        content = content,
    )
}
