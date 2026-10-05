package com.sipun.sonora.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.sipun.sonora.BuildConfig
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun SettingsScreen(onCheckForUpdates: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(top = 20.dp, bottom = 18.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Make Sonora feel at home.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }

        SettingsSection("Playback") {
            SettingsRow(Icons.Default.GraphicEq, "Audio player", "Media3 with background playback")
            SettingsRow(Icons.Default.Headphones, "Playback controls", "Shuffle, repeat, seek, previous and next")
        }

        SettingsSection("Library") {
            SettingsRow(Icons.Default.LibraryMusic, "Music library", "Songs discovered from this device")
            SettingsRow(Icons.Default.Album, "Artwork", "Album artwork is shown when available")
        }

        SettingsSection("Appearance") {
            SettingsRow(
                icon = Icons.Default.LightMode,
                title = "Theme",
                subtitle = "Light Sonora",
                trailing = {
                    Surface(color = SonoraRed.copy(alpha = 0.10f), shape = RoundedCornerShape(50)) {
                        Text("Light", color = SonoraRed, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                },
            )
        }

        SettingsSection("Updates") {
            SettingsRow(
                icon = Icons.Default.SystemUpdate,
                title = "Check for updates",
                subtitle = "Check GitHub for the latest Sonora release",
                onClick = onCheckForUpdates,
                trailing = {
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                },
            )
        }

        SettingsSection("About") {
            SettingsRow(Icons.Default.MusicNote, "Sonora", "Local music player • v" + BuildConfig.VERSION_NAME)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = SonoraRed, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp))
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(SonoraRed.copy(alpha = 0.10f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = SonoraRed)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
        trailing?.invoke()
    }
}
