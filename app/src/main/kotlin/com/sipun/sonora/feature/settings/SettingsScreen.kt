package com.sipun.sonora.feature.settings

import androidx.compose.foundation.background
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
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun SettingsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(top = 20.dp, bottom = 18.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(
                "Make Sonora feel at home.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        SettingsSection("Playback") {
            SettingsRow(
                icon = Icons.Default.GraphicEq,
                title = "Audio player",
                subtitle = "Media3 with background playback",
            )
            SettingsRow(
                icon = Icons.Default.Headphones,
                title = "Playback controls",
                subtitle = "Shuffle, repeat, seek, previous and next",
            )
        }

        SettingsSection("Library") {
            SettingsRow(
                icon = Icons.Default.LibraryMusic,
                title = "Music library",
                subtitle = "Songs discovered from this device",
            )
            SettingsRow(
                icon = Icons.Default.Album,
                title = "Artwork",
                subtitle = "Album artwork is shown when available",
            )
        }

        SettingsSection("Appearance") {
            SettingsRow(
                icon = Icons.Default.LightMode,
                title = "Theme",
                subtitle = "Light Sonora",
                trailing = {
                    Surface(
                        color = SonoraRed.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            "Light",
                            color = SonoraRed,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                },
            )
        }

        SettingsSection("About") {
            SettingsRow(
                icon = Icons.Default.MusicNote,
                title = "Sonora",
                subtitle = "Local music player • v0.1",
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = SonoraRed,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(SonoraRed.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SonoraRed)
        }
        Column(
            Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        trailing?.invoke()
    }
}
