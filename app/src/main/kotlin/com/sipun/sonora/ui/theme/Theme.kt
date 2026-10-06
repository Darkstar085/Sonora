package com.sipun.sonora.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sipun.sonora.data.preferences.AppTheme
import com.sipun.sonora.data.preferences.ThemePreferences

private val SonoraLightColors = lightColorScheme(
    primary = SonoraRed,
    onPrimary = Color.White,
    primaryContainer = SonoraRed.copy(alpha = 0.12f),
    onPrimaryContainer = SonoraRed,
    background = Color(0xFFF6F7FA),
    onBackground = Color(0xFF202124),
    surface = Color.White,
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFF0F1F5),
    onSurfaceVariant = Color(0xFF777A83),
    outline = Color(0xFFE0E1E6),
)

private val SonoraDarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141313),
    onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF1C1B1B),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    outline = Color(0xFFA08C89),
)

private val SonoraPureBlackColors = darkColorScheme(
    primary = SonoraRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5F0000),
    onPrimaryContainer = Color(0xFFFFDAD6),
    background = Color.Black,
    onBackground = Color(0xFFF5F5F5),
    surface = Color.Black,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF3A3A3A),
)

@Composable
fun SonoraTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val preferences = androidx.compose.runtime.remember(context) {
        ThemePreferences.from(context.applicationContext)
    }
    val selectedTheme by preferences.theme.collectAsStateWithLifecycle()
    val pureBlack by preferences.pureBlack.collectAsStateWithLifecycle()
    val darkTheme = pureBlack || when (selectedTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = when {
        pureBlack -> SonoraPureBlackColors
        darkTheme -> SonoraDarkColors
        else -> SonoraLightColors
    }

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SonoraTypography,
        content = content,
    )
}
