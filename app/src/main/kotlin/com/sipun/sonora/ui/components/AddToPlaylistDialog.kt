package com.sipun.sonora.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun AddToPlaylistDialog(songId: Long, preferences: SonoraPreferences, onDismiss: () -> Unit) {
    var playlists by remember { mutableStateOf(preferences.playlists()) }
    var showCreate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text("Create a playlist to save this song.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(Modifier.heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            val added = songId in playlist.songIds
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text(if (added) "Already added" else playlist.songIds.size.toString() + " songs") },
                                leadingContent = { Icon(Icons.Default.LibraryMusic, null, tint = if (added) SonoraRed else MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingContent = {
                                    if (added) Text("Added", color = SonoraRed, style = MaterialTheme.typography.labelMedium)
                                    else IconButton(onClick = {
                                        preferences.addToPlaylist(playlist.id, songId)
                                        playlists = preferences.playlists()
                                    }) { Icon(Icons.Default.Add, "Add to " + playlist.name) }
                                },
                            )
                        }
                    }
                }
                TextButton(onClick = { showCreate = true }, modifier = Modifier.padding(top = 6.dp)) { Text("Create new playlist") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New playlist") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist name") }, singleLine = true) },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.createPlaylist(name)?.let { preferences.addToPlaylist(it.id, songId) }
                        playlists = preferences.playlists()
                        showCreate = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
}
