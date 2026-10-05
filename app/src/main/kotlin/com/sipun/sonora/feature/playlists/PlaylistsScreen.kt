package com.sipun.sonora.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.preferences.SonoraPlaylist
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun PlaylistsScreen(
    playerController: PlayerController,
    onOpenPlaylist: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { SonoraPreferences(context) }
    var playlists by remember { mutableStateOf(preferences.playlists()) }
    var showCreate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your playlists", style = MaterialTheme.typography.headlineSmall)
                Text(
                    playlists.size.toString() + if (playlists.size == 1) " playlist" else " playlists",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(
                onClick = { showCreate = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SonoraRed,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Icon(Icons.Default.Add, "Create playlist") }
        }

        if (playlists.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.LibraryMusic, null, tint = SonoraRed, modifier = Modifier.size(56.dp))
                Text("Create your first playlist", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 14.dp))
                Text(
                    "Build collections from your local music.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        onClick = { onOpenPlaylist(playlist.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(58.dp).background(
                                    SonoraRed.copy(alpha = 0.10f),
                                    RoundedCornerShape(16.dp),
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.LibraryMusic, null, tint = SonoraRed)
                            }
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    playlist.songIds.size.toString() + if (playlist.songIds.size == 1) " song" else " songs",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                preferences.deletePlaylist(playlist.id)
                                playlists = preferences.playlists()
                            }) {
                                Icon(Icons.Default.DeleteOutline, "Delete playlist")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onCreate = {
                preferences.createPlaylist(it)
                playlists = preferences.playlists()
                showCreate = false
            },
        )
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
