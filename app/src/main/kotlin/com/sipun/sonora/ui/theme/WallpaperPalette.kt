package com.sipun.sonora.ui.theme

import androidx.compose.ui.graphics.Color
import com.sipun.sonora.data.preferences.DynamicPalette
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class WallpaperPalette(val primary: Color, val secondary: Color, val tertiary: Color, val container: Color)

fun wallpaperPalette(seed: Color, style: DynamicPalette, darkTheme: Boolean): WallpaperPalette {
    val hsl = rgbToHsl(seed.red, seed.green, seed.blue)
    val hue = if (style == DynamicPalette.EXPRESSIVE) (hsl[0] + 45f) % 360f else hsl[0]
    val saturation = when (style) {
        DynamicPalette.TONAL_SPOT -> 0.42f
        DynamicPalette.VIBRANT -> 0.78f
        DynamicPalette.EXPRESSIVE -> 0.62f
        DynamicPalette.NEUTRAL -> 0.18f
        DynamicPalette.MONOCHROME -> 0f
    }
    val primaryLightness = if (darkTheme) 0.72f else 0.42f
    return WallpaperPalette(
        hslColor(hue, saturation, primaryLightness),
        hslColor((hue + if (style == DynamicPalette.EXPRESSIVE) 60f else 20f) % 360f, saturation * 0.78f, if (darkTheme) 0.66f else 0.48f),
        hslColor((hue + if (style == DynamicPalette.EXPRESSIVE) 120f else 35f) % 360f, saturation * 0.68f, if (darkTheme) 0.64f else 0.52f),
        hslColor(hue, saturation * 0.28f, if (darkTheme) 0.24f else 0.90f),
    )
}

private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f % 2f) - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

private fun rgbToHsl(red: Float, green: Float, blue: Float): FloatArray {
    val max = max(red, max(green, blue))
    val min = min(red, min(green, blue))
    val lightness = (max + min) / 2f
    if (max == min) return floatArrayOf(0f, 0f, lightness)
    val delta = max - min
    val saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val hue = when (max) {
        red -> ((green - blue) / delta + if (green < blue) 6f else 0f) * 60f
        green -> ((blue - red) / delta + 2f) * 60f
        else -> ((red - green) / delta + 4f) * 60f
    }
    return floatArrayOf(hue, saturation, lightness)
}
