package com.sipun.sonora.feature.settings

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.sipun.sonora.BuildConfig
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.AppTheme
import com.sipun.sonora.data.preferences.DynamicPalette
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.data.preferences.ThemePreferences
import com.sipun.sonora.ui.theme.WallpaperPalette
import com.sipun.sonora.ui.theme.wallpaperPalette

@Composable
fun SettingsScreen(
    onCheckForUpdates: () -> Unit = {},
) {
    val context = LocalContext.current
    val themePreferences = remember(context) {
        ThemePreferences.from(context.applicationContext)
    }
    val playbackPreferences = remember(context) {
        SonoraPreferences(context.applicationContext)
    }
    val selectedTheme by themePreferences.theme.collectAsStateWithLifecycle()
    val pureBlack by themePreferences.pureBlack.collectAsStateWithLifecycle()
    val dynamicColor by themePreferences.dynamicColor.collectAsStateWithLifecycle()
    val selectedPalette by themePreferences.dynamicPalette.collectAsStateWithLifecycle()
    val customColor by themePreferences.customColor.collectAsStateWithLifecycle()
    var resumePlayback by remember { mutableStateOf(playbackPreferences.resumePlayback()) }
    var gaplessPlayback by remember { mutableStateOf(playbackPreferences.gaplessPlayback()) }
    var crossfadeSeconds by remember { mutableIntStateOf(playbackPreferences.crossfadeSeconds()) }
    var normalizeVolume by remember { mutableStateOf(playbackPreferences.normalizeVolume()) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSonoraDialog by remember { mutableStateOf(false) }
    var mediaManagementAllowed by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                MediaStore.canManageMedia(context),
        )
    }
    var ringtoneAllowed by remember(context) {
        mutableStateOf(Settings.System.canWrite(context))
    }
    var installUpdatesAllowed by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                context.packageManager.canRequestPackageInstalls(),
        )
    }
    var batteryUnrestricted by remember(context) {
        mutableStateOf(
            (context.getSystemService(PowerManager::class.java))
                ?.isIgnoringBatteryOptimizations(context.packageName) == true,
        )
    }
    var musicPermissionAllowed by remember(context) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                },
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var photoPermissionAllowed by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES,
                ) == PackageManager.PERMISSION_GRANTED ||
                (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                        ContextCompat.checkSelfPermission(
                            context,
                            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                        ) == PackageManager.PERMISSION_GRANTED
                ),
        )
    }
    var notificationPermissionAllowed by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    fun refreshPermissionStates() {
        mediaManagementAllowed =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                MediaStore.canManageMedia(context)
        ringtoneAllowed = Settings.System.canWrite(context)
        installUpdatesAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                context.packageManager.canRequestPackageInstalls()
        batteryUnrestricted =
            (context.getSystemService(PowerManager::class.java))
                ?.isIgnoringBatteryOptimizations(context.packageName) == true
        musicPermissionAllowed = ContextCompat.checkSelfPermission(
            context,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            },
        ) == PackageManager.PERMISSION_GRANTED
        photoPermissionAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES,
                ) == PackageManager.PERMISSION_GRANTED ||
                (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                        ContextCompat.checkSelfPermission(
                            context,
                            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                        ) == PackageManager.PERMISSION_GRANTED
                )
        notificationPermissionAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }
    val lifecycleOwner = LocalLifecycleOwner.current
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

    DisposableEffect(lifecycleOwner, context) {
        refreshPermissionStates()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissionStates()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val setupPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshPermissionStates()
    }

    fun openMediaManagementSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
                        data = Uri.parse("package:" + context.packageName)
                    },
                )
            }
        }
    }

    fun openRingtoneSettings() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + context.packageName),
                ),
            )
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.packageName),
                ),
            )
        }
    }

    fun openBatterySettings(enable: Boolean) {
        val action = if (enable) {
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        } else {
            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
        runCatching {
            context.startActivity(
                Intent(action).apply {
                    if (enable) {
                        data = Uri.parse("package:" + context.packageName)
                    }
                },
            )
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
            .verticalScroll(rememberScrollState())
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
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = if (darkTheme) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color.White
                            },
                            uncheckedTrackColor = if (darkTheme) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color(0xFFE3E3E8)
                            },
                            uncheckedBorderColor = if (darkTheme) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                Color(0xFFC1C1C8)
                            },
                        ),
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
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = if (darkTheme) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color.White
                            },
                            uncheckedTrackColor = if (darkTheme) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color(0xFFE3E3E8)
                            },
                            uncheckedBorderColor = if (darkTheme) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                Color(0xFFC1C1C8)
                            },
                        ),
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

        SettingsSection(stringResource(R.string.settings_playback)) {
            SettingsRow(
                icon = Icons.Default.PlayArrow,
                title = stringResource(R.string.settings_resume_playback),
                subtitle = stringResource(R.string.settings_resume_playback_detail),
                trailing = {
                    PlaybackSwitch(
                        checked = resumePlayback,
                        darkTheme = darkTheme,
                        onCheckedChange = {
                            resumePlayback = it
                            playbackPreferences.setResumePlayback(it)
                        },
                    )
                },
            )
            SettingsRow(
                icon = Icons.Default.AllInclusive,
                title = stringResource(R.string.settings_gapless_playback),
                subtitle = stringResource(R.string.settings_gapless_playback_detail),
                trailing = {
                    PlaybackSwitch(
                        checked = gaplessPlayback,
                        darkTheme = darkTheme,
                        onCheckedChange = {
                            gaplessPlayback = it
                            playbackPreferences.setGaplessPlayback(it)
                        },
                    )
                },
            )
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(R.string.settings_normalize_volume),
                subtitle = stringResource(R.string.settings_normalize_volume_detail),
                trailing = {
                    PlaybackSwitch(
                        checked = normalizeVolume,
                        darkTheme = darkTheme,
                        onCheckedChange = {
                            normalizeVolume = it
                            playbackPreferences.setNormalizeVolume(it)
                        },
                    )
                },
            )
            CrossfadeSelector(
                seconds = crossfadeSeconds,
                onSecondsChange = {
                    crossfadeSeconds = it
                    playbackPreferences.setCrossfadeSeconds(it)
                },
            )
        }

        SettingsSection(stringResource(R.string.settings_permissions)) {
            if (!musicPermissionAllowed) {
                SettingsPermissionRow(
                    icon = Icons.Default.MusicNote,
                    title = stringResource(R.string.settings_permission_music),
                    subtitle = stringResource(R.string.settings_permission_music_detail),
                    status = stringResource(R.string.settings_permission_allow),
                    onClick = {
                        setupPermissionLauncher.launch(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            },
                        )
                    },
                )
            }
            if (!photoPermissionAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SettingsPermissionRow(
                    icon = Icons.Default.Image,
                    title = stringResource(R.string.settings_permission_photos),
                    subtitle = stringResource(R.string.settings_permission_photos_detail),
                    status = stringResource(R.string.settings_permission_allow),
                    onClick = {
                        setupPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    },
                )
            }
            if (!notificationPermissionAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SettingsPermissionRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_permission_notifications),
                    subtitle = stringResource(R.string.settings_permission_notifications_detail),
                    status = stringResource(R.string.settings_permission_allow),
                    onClick = {
                        setupPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            SettingsPermissionRow(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.settings_permission_media_edit),
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    stringResource(R.string.settings_permission_media_edit_detail)
                } else {
                    stringResource(R.string.settings_permission_media_ondemand)
                },
                status = when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->
                        stringResource(R.string.settings_permission_ondemand)
                    mediaManagementAllowed ->
                        stringResource(R.string.settings_permission_allowed)
                    else ->
                        stringResource(R.string.settings_permission_allow)
                },
                onClick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ::openMediaManagementSettings
                } else {
                    null
                },
            )
            SettingsPermissionRow(
                icon = Icons.Default.DeleteOutline,
                title = stringResource(R.string.settings_permission_media_delete),
                subtitle = stringResource(R.string.settings_permission_media_delete_ondemand),
                status = stringResource(R.string.settings_permission_ondemand),
                onClick = null,
            )
            SettingsPermissionRow(
                icon = Icons.Default.Phone,
                title = stringResource(R.string.settings_permission_ringtone),
                subtitle = stringResource(R.string.settings_permission_ringtone_detail),
                status = if (ringtoneAllowed) {
                    stringResource(R.string.settings_permission_allowed)
                } else {
                    stringResource(R.string.settings_permission_allow)
                },
                onClick = ::openRingtoneSettings,
            )
            SettingsPermissionRow(
                icon = Icons.Default.SystemUpdate,
                title = stringResource(R.string.settings_permission_install_updates),
                subtitle = stringResource(R.string.settings_permission_install_updates_detail),
                status = if (installUpdatesAllowed) {
                    stringResource(R.string.settings_permission_allowed)
                } else {
                    stringResource(R.string.settings_permission_allow)
                },
                onClick = ::openInstallPermissionSettings,
            )
            SettingsRow(
                icon = Icons.Default.BatteryFull,
                title = stringResource(R.string.settings_unrestricted_battery),
                subtitle = stringResource(R.string.settings_unrestricted_battery_detail),
                onClick = { openBatterySettings(!batteryUnrestricted) },
                trailing = {
                    PlaybackSwitch(
                        checked = batteryUnrestricted,
                        darkTheme = darkTheme,
                        onCheckedChange = { openBatterySettings(it) },
                    )
                },
            )
        }

        SettingsSection(stringResource(R.string.settings_about)) {
            SettingsRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_sonora),
                subtitle = stringResource(R.string.about_sonora_detail),
                onClick = { showSonoraDialog = true },
            )
            SettingsRow(
                icon = Icons.Default.Code,
                title = stringResource(R.string.about_open_source),
                subtitle = stringResource(R.string.about_open_source_detail),
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(SONORA_REPOSITORY)),
                    )
                },
            )
        }
        Spacer(Modifier.height(112.dp))
    }

    if (showSonoraDialog) {
        SonoraAboutDialog(
            onDismiss = { showSonoraDialog = false },
            onCheckForUpdates = onCheckForUpdates,
        )
    }

    if (!pureBlack && showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Choose how Sonora looks on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
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
                TextButton(
                    onClick = { showThemeDialog = false },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsPermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    status: String,
    onClick: (() -> Unit)?,
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            Surface(
                color = if (status == stringResource(R.string.settings_permission_allowed)) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                },
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    status,
                    color = if (status == stringResource(R.string.settings_permission_allowed)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        },
    )
}

@Composable
private fun PlaybackSwitch(
    checked: Boolean,
    darkTheme: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = if (darkTheme) {
                MaterialTheme.colorScheme.onSurface
            } else {
                Color.White
            },
            uncheckedTrackColor = if (darkTheme) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color(0xFFE3E3E8)
            },
            uncheckedBorderColor = if (darkTheme) {
                MaterialTheme.colorScheme.outline
            } else {
                Color(0xFFC1C1C8)
            },
        ),
    )
}

@Composable
private fun CrossfadeSelector(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    stringResource(R.string.settings_crossfade),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (seconds == 0) {
                        stringResource(R.string.settings_crossfade_off)
                    } else {
                        stringResource(R.string.settings_crossfade_value, seconds)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Slider(
            value = seconds.toFloat(),
            onValueChange = { onSecondsChange(it.toInt()) },
            valueRange = 0f..12f,
            steps = 11,
            modifier = Modifier.padding(start = 42.dp, top = 2.dp),
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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

private const val SONORA_REPOSITORY = "https://github.com/Darkstar085/Sonora"

@Composable
private fun SonoraAboutDialog(
    onDismiss: () -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.about_sound_wave),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = Int.MAX_VALUE,
    )
    val loveComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.love),
    )
    val loveProgress by animateLottieCompositionAsState(
        composition = loveComposition,
        iterations = Int.MAX_VALUE,
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.sonora_launcher_logo),
                            contentDescription = stringResource(R.string.about_sonora),
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.about_sonora),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )

                Text(
                    text = stringResource(R.string.about_sonora_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                val animationColor = MaterialTheme.colorScheme.primary.toArgb()
                val dynamicProperties = rememberLottieDynamicProperties(
                    rememberLottieDynamicProperty(
                        property = LottieProperty.COLOR,
                        value = animationColor,
                        "**",
                        "Fill 1",
                    ),
                )

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    dynamicProperties = dynamicProperties,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .padding(top = 14.dp),
                )

                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable(onClick = onCheckForUpdates),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.about_made_with_prefix),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LottieAnimation(
                        composition = loveComposition,
                        progress = { loveProgress },
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = stringResource(R.string.about_made_with_suffix),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        } else {
            Color.Transparent
        },
        border = if (selected) {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            )
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    RoundedCornerShape(14.dp)
                ),
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
