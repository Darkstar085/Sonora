package com.sipun.sonora.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sipun.sonora.BuildConfig
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.AppTheme
import com.sipun.sonora.data.preferences.ThemePreferences
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun SettingsScreen(onCheckForUpdates: () -> Unit = {}) {
    val context = LocalContext.current
    val themePreferences = remember(context) {
        ThemePreferences.from(context.applicationContext)
    }
    val selectedTheme by themePreferences.theme.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

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
            SettingsRow(
                icon = Icons.Default.LightMode,
                title = stringResource(R.string.settings_theme),
                subtitle = themeLabel,
                onClick = { showThemeDialog = true },
                trailing = {
                    Surface(
                        color = SonoraRed.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            themeLabel,
                            color = SonoraRed,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                },
            )
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

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    stringResource(R.string.settings_theme),
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
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = SonoraRed,
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
                .background(SonoraRed.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = SonoraRed)
        }
        Column(
            Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
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
