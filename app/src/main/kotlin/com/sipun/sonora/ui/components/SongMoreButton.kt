@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.ui.theme.SonoraRed
import com.sipun.sonora.ui.theme.SonoraSurface

@Composable
fun SongMoreButton(
    song: Song,
    preferences: SonoraPreferences,
    playerController: com.sipun.sonora.player.PlayerController? = null,
    modifier: Modifier = Modifier,
    showPlaylist: Boolean = true,
    showFavorite: Boolean = true,
    showAlbum: Boolean = true,
    showArtist: Boolean = true,
    showRemoveFromPlaylist: Boolean = false,
    showPlayNext: Boolean = false,
    showAddToQueue: Boolean = false,
    showRemoveFromDevice: Boolean = false,
    showInfo: Boolean = true,
    onOpenAlbum: ((String) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onChanged: () -> Unit = {},
) {
    var showMore by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFavorite = song.id in preferences.favoriteIds()

    IconButton(
        onClick = { showMore = true },
        modifier = modifier,
    ) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }

    if (showMore) {
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            containerColor = SonoraSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    "More options",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )

                if (showPlayNext && playerController != null) {
                    SongMoreOption(
                        icon = Icons.Default.PlayArrow,
                        label = "Play next",
                        onClick = {
                            showMore = false
                            playerController.playNext(song)
                        },
                    )
                }

                if (showAddToQueue && playerController != null) {
                    SongMoreOption(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        label = "Add to queue",
                        onClick = {
                            showMore = false
                            playerController.addToQueue(song)
                        },
                    )
                }

                if (showPlaylist) {
                    SongMoreOption(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        label = "Add to playlist",
                        onClick = {
                            showMore = false
                            showPlaylistDialog = true
                        },
                    )
                }

                if (showFavorite) {
                    SongMoreOption(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        onClick = {
                            showMore = false
                            preferences.toggleFavorite(song.id)
                            onChanged()
                        },
                    )
                }

                if (showRemoveFromPlaylist) {
                    if (showPlaylist || showFavorite) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    SongMoreOption(
                        icon = Icons.Default.RemoveCircleOutline,
                        label = "Remove from playlist",
                        onClick = {
                            showMore = false
                            onRemoveFromPlaylist?.invoke()
                        },
                    )
                }

                if (showAlbum && onOpenAlbum != null) {
                    SongMoreOption(
                        icon = Icons.Default.Album,
                        label = "Open album",
                        onClick = {
                            showMore = false
                            onOpenAlbum(song.album)
                        },
                    )
                }

                if (showArtist && onOpenArtist != null) {
                    SongMoreOption(
                        icon = Icons.Default.Person,
                        label = "Open artist",
                        onClick = {
                            showMore = false
                            onOpenArtist(song.artist)
                        },
                    )
                }

                if (showInfo) {
                    SongMoreOption(
                        icon = Icons.Default.Info,
                        label = "Song info",
                        onClick = {
                            showMore = false
                            showInfoDialog = true
                        },
                    )
                }

                if (showRemoveFromDevice) {
                    SongMoreOption(
                        icon = Icons.Default.Delete,
                        label = "Remove from device",
                        onClick = {
                            showMore = false
                            showRemoveConfirmation = true
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(song.id, preferences) {
            showPlaylistDialog = false
            onChanged()
        }
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text("Remove from device?") },
            text = { Text("This will permanently remove the audio file from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val removed = withContext(Dispatchers.IO) {
                                runCatching {
                                    context.contentResolver.delete(Uri.parse(song.uri), null, null) > 0
                                }.getOrDefault(false)
                            }
                            showRemoveConfirmation = false
                            if (removed) onChanged()
                        }
                    },
                ) { Text("Remove", color = SonoraRed) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Song info") },
            text = {
                Column {
                    Text(song.title, style = MaterialTheme.typography.titleMedium)
                    Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(song.album, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (song.durationMs > 0L) {
                        Text(
                            formatDuration(song.durationMs),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    song.folder?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            },
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
private fun SongMoreOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
