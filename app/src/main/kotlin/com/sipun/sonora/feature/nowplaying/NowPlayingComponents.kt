@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.sipun.sonora.feature.nowplaying

import android.content.ContentValues
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
internal fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(78.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(54.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
internal fun MoreOptionsSheet(
    onDismiss: () -> Unit,
    onAlbum: () -> Unit,
    onArtist: () -> Unit,
    onPlaylist: () -> Unit,
    onRingtone: () -> Unit,
    onSleepTimer: () -> Unit,
    onSpeed: () -> Unit,
    onEqualizer: () -> Unit,
    onEditMetadata: () -> Unit,
    onInfo: () -> Unit,
    onRemove: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                "More options",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            MoreOption(Icons.Default.Album, "Go to album", onAlbum)
            MoreOption(Icons.Default.Person, "Go to artist", onArtist)
            MoreOption(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist", onPlaylist)
            MoreOption(Icons.Default.Notifications, "Set as ringtone", onRingtone)
            MoreOption(Icons.Default.Timer, "Sleep timer", onSleepTimer)
            MoreOption(Icons.Default.Speed, "Playback speed", onSpeed)
            MoreOption(Icons.Default.Equalizer, "Equalizer", onEqualizer)
            MoreOption(Icons.Default.Edit, "Edit metadata", onEditMetadata)
            MoreOption(Icons.Default.Info, "Show song info", onInfo)
            MoreOption(
                Icons.Default.DeleteOutline,
                "Remove from library",
                onRemove,
                destructive = true
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MoreOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    ListItem(
        headlineContent = {
            Text(
                label,
                color = if (destructive) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        },
        leadingContent = {
            Icon(
                icon,
                null,
                tint = if (destructive) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
internal fun QueueDialog(
    queue: List<Song>,
    currentId: Long?,
    onSelect: (Song) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Queue") },
        text = {
            if (queue.isEmpty()) {
                Text("The current queue is not available.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    items(queue, key = { it.id }) { song ->
                        TextButton(
                            onClick = { onSelect(song) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    song.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    song.artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
internal fun ArtistDialog(
    artist: String,
    songs: List<Song>,
    onSelect: (Song) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(artist.ifBlank { "Unknown artist" }) },
        text = {
            if (songs.isEmpty()) {
                Text("Artist view is based on the current playback queue.")
            } else {
                Column(Modifier.heightIn(max = 420.dp)) {
                    songs.forEach { song ->
                        TextButton(
                            onClick = { onSelect(song) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
internal fun SongInfoDialog(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Song info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Title", song.title)
                InfoRow("Artist", song.artist)
                InfoRow("Album", song.album)
                song.genre?.let { InfoRow("Genre", it) }
                song.year?.let { InfoRow("Year", it.toString()) }
                song.trackNumber?.let { InfoRow("Track", it.toString()) }
                song.folder?.let { InfoRow("Folder", it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            Column {
                speeds.forEach { speed ->
                    TextButton(
                        onClick = { onSelect(speed) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (speed == currentSpeed) "✓  " else "    " + speed.toString() + "×")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
internal fun SleepTimerDialog(
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                options.forEach { minutes ->
                    TextButton(
                        onClick = { onSelect(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$minutes minutes")
                    }
                }
                TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel timer")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

internal fun setAsRingtone(context: android.content.Context, song: Song) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + context.packageName),
            ),
        )
        Toast.makeText(
            context,
            "Allow Sonora to change system settings, then try again.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            val resolver = context.contentResolver
            val source = Uri.parse(song.uri)
            val collection =
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    "Sonora_" + song.title.replace("/", "_") + ".mp3"
                )
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/*")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Ringtones/")
                put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: error("Unable to create ringtone")
            resolver.openInputStream(source).use { input ->
                resolver.openOutputStream(uri).use { output ->
                    requireNotNull(input)
                    requireNotNull(output)
                    input.copyTo(output)
                }
            }
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null
            )
            withContext(Dispatchers.Main) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    uri
                )
                Toast.makeText(context, "Ringtone set", Toast.LENGTH_SHORT).show()
            }
        }.onFailure {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Unable to set ringtone", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

internal fun openEqualizer(context: android.content.Context) {
    val intent = Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
    }
}

