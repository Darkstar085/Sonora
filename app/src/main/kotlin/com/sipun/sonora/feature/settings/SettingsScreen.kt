package com.sipun.sonora.feature.settings

import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sipun.sonora.BuildConfig
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.AppTheme
import com.sipun.sonora.data.preferences.DynamicPalette
import com.sipun.sonora.data.preferences.ThemePreferences
import com.sipun.sonora.ui.theme.WallpaperPalette
import com.sipun.sonora.ui.theme.wallpaperPalette

@Composable
fun SettingsScreen(onCheckForUpdates: () -> Unit = {}) {
    val context = LocalContext.current
    val themePreferences = remember(context) {
        ThemePreferences.from(context.applicationContext)
    }
    val selectedTheme by themePreferences.theme.collectAsStateWithLifecycle()
    val pureBlack by themePreferences.pureBlack.collectAsStateWithLifecycle()
    val dynamicColor by themePreferences.dynamicColor.collectAsStateWithLifecycle()
    val selectedPalette by themePreferences.dynamicPalette.collectAsStateWithLifecycle()
    val customColor by themePreferences.customColor.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var wallpaperSeed by remember(context) { mutableStateOf(readWallpaperSeed(context)) }

    DisposableEffect(context, dynamicColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 || !dynamicColor) {
            onDispose { }
        } else {
            val manager = WallpaperManager.getInstance(context)
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and WallpaperManager.FLAG_SYSTEM) != 0 && colors != null) {
                    wallpaperSeed = Color(colors.primaryColor.toArgb())
                }
            }
            val handler = Handler(Looper.getMainLooper())
            manager.addOnColorsChangedListener(listener, handler)
            onDispose { manager.removeOnColorsChangedListener(listener) }
        }
    }

    val darkTheme = pureBlack || when (selectedTheme) {
        AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val themeLabel = when (selectedTheme) {
        AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
        AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
        AppTheme.SYSTEM -> stringResource(R.string.settings_theme_device_default)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(top = 20.dp, bottom = 18.dp)) {
            Text(
                stringResource(R.string.settings_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                stringResource(R.string.settings_tagline),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        SettingsSection(stringResource(R.string.settings_appearance)) {
            if (!pureBlack) {
                SettingsRow(
                    icon = Icons.Default.LightMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = themeLabel,
                    onClick = { showThemeDialog = true },
                    trailing = {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                themeLabel,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    },
                )
            }
            SettingsRow(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_detail),
                trailing = {
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = themePreferences::setDynamicColor,
                    )
                },
            )
            if (dynamicColor) {
                PaletteSelector(
                    seed = wallpaperSeed,
                    darkTheme = darkTheme,
                    selected = selectedPalette,
                    onSelect = themePreferences::setDynamicPalette,
                )
            }

            SettingsRow(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.settings_pure_black),
                subtitle = stringResource(R.string.settings_pure_black_detail),
                trailing = {
                    Switch(
                        checked = pureBlack,
                        onCheckedChange = themePreferences::setPureBlack,
                    )
                },
            )

            if (!dynamicColor) {
                CustomColorSelector(
                    color = Color(customColor),
                    onColorChange = { themePreferences.setCustomColor(it) },
                )
            }

        }

        SettingsSection(stringResource(R.string.settings_updates)) {
            SettingsRow(
                icon = Icons.Default.SystemUpdate,
                title = stringResource(R.string.settings_check_updates),
                subtitle = stringResource(R.string.settings_check_updates_detail),
                onClick = onCheckForUpdates,
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
            )
        }

        SettingsSection(stringResource(R.string.settings_about)) {
            SettingsRow(
                Icons.Default.MusicNote,
                stringResource(R.string.app_name),
                stringResource(R.string.settings_local_player, BuildConfig.VERSION_NAME)
            )
        }
    }

    if (!pureBlack && showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    stringResource(R.string.settings_theme),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column {
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_light),
                        selected = selectedTheme == AppTheme.LIGHT,
                        onClick = {
                            themePreferences.setTheme(AppTheme.LIGHT)
                            showThemeDialog = false
                        },
                    )
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_dark),
                        selected = selectedTheme == AppTheme.DARK,
                        onClick = {
                            themePreferences.setTheme(AppTheme.DARK)
                            showThemeDialog = false
                        },
                    )
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_device_default),
                        selected = selectedTheme == AppTheme.SYSTEM,
                        onClick = {
                            themePreferences.setTheme(AppTheme.SYSTEM)
                            showThemeDialog = false
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomColorSelector(
    color: Color,
    onColorChange: (Int) -> Unit,
) {
    val presetColors = listOf(
        Color(0xFFE92B2B),
        Color(0xFFE57373),
        Color(0xFF42A5F5),
        Color(0xFF26A69A),
        Color(0xFF66BB6A),
        Color(0xFF9CCC65),
        Color(0xFFFFB74D),
        Color(0xFFFF7043),
        Color(0xFFAB47BC),
        Color(0xFF7E57C2),
        Color(0xFF5C6BC0),
        Color(0xFF26C6DA),
        Color(0xFF26A69A),
        Color(0xFF8BC34A),
        Color(0xFFD4E157),
        Color(0xFFFFCA28),
        Color(0xFFFFA726),
    )
    val hsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }
    var hue by remember(color) { mutableStateOf(hsv[0]) }

    val hueColors = remember {
        (0..360 step 30).map { Color.hsv(it.toFloat(), 0.78f, 0.92f) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.settings_custom_color),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.settings_custom_color_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            presetColors.forEach { preset ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(preset, RoundedCornerShape(50))
                        .clickable {
                            val presetHsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(preset.toArgb(), presetHsv)
                            hue = presetHsv[0]
                            onColorChange(preset.toArgb())
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (color.toArgb() == preset.toArgb()) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(hueColors)),
            )

            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    val hsvColor = floatArrayOf(it, 0.78f, 0.92f)
                    onColorChange(android.graphics.Color.HSVToColor(hsvColor))
                },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(34.dp)
                            .background(
                                Color.White,
                                RoundedCornerShape(50),
                            ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PaletteSelector(
    seed: Color,
    darkTheme: Boolean,
    selected: DynamicPalette,
    onSelect: (DynamicPalette) -> Unit,
) {
    val palettes = DynamicPalette.entries.take(4)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.settings_palette_style),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            palettes.forEach { palette ->
                PaletteCard(
                    palette = palette,
                    colors = wallpaperPalette(seed, palette, darkTheme),
                    selected = palette == selected,
                    onClick = { onSelect(palette) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: DynamicPalette,
    colors: WallpaperPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paletteColors = listOf(
        colors.primary,
        colors.secondary,
        colors.tertiary,
        colors.container,
    )

    Card(
        modifier = modifier
            .height(132.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 1.dp,
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            },
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(6.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(15.dp)),
            ) {
                paletteColors.forEachIndexed { index, color ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(color),
                    ) {
                        if (index < paletteColors.lastIndex) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)),
                            )
                        }
                    }
                }
            }

            if (selected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .size(28.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 2.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun readWallpaperSeed(context: android.content.Context): Color =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        androidx.compose.material3.dynamicLightColorScheme(context).primary
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?.primaryColor
            ?.let { Color(it.toArgb()) }
            ?: Color(0xFFE92B2B)
    } else {
        Color(0xFFE92B2B)
    }

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(
            Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        trailing?.invoke()
    }
}
