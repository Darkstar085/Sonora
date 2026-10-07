package com.sipun.sonora.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.preferences.SonoraPlaylist
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongActionConfig
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.ui.components.SongMoreButton
import java.text.Normalizer

@Composable
internal fun SongList(
    songs: List<Song>,
    player: PlayerController,
    preferences: SonoraPreferences,
    open: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
    onChanged: () -> Unit,
) {
    val sortedSongs = remember(songs) { songs.sortedWith(songTitleComparator) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
    ) {
        itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
            SongCard(
                song = song,
                player = player,
                onClick = { player.playQueue(sortedSongs, index); open() },
                preferences = preferences,
                onOpenAlbum = openAlbum,
                onOpenArtist = openArtist,
                actions = SongActionConfig(
                    showRemoveFromDevice = true,
                    showEditMetadata = true,
                ),
                onChanged = onChanged,
            )
        }
    }
}

private val songTitleComparator = Comparator<Song> { first, second ->
    naturalCompare(first.title, second.title)
        .takeIf { it != 0 }
        ?: naturalCompare(first.artist, second.artist)
            .takeIf { it != 0 }
        ?: naturalCompare(first.album, second.album)
            .takeIf { it != 0 }
        ?: first.id.compareTo(second.id)
}

private fun naturalCompare(first: String, second: String): Int {
    val left = normalizeForSort(first)
    val right = normalizeForSort(second)
    var leftIndex = 0
    var rightIndex = 0

    while (leftIndex < left.length && rightIndex < right.length) {
        val leftChar = left[leftIndex]
        val rightChar = right[rightIndex]

        if (leftChar.isDigit() && rightChar.isDigit()) {
            val leftStart = leftIndex
            val rightStart = rightIndex
            while (leftIndex < left.length && left[leftIndex].isDigit()) leftIndex++
            while (rightIndex < right.length && right[rightIndex].isDigit()) rightIndex++

            val leftNumber = left.substring(leftStart, leftIndex).trimStart('0').ifBlank { "0" }
            val rightNumber = right.substring(rightStart, rightIndex).trimStart('0').ifBlank { "0" }

            if (leftNumber.length != rightNumber.length) {
                return leftNumber.length.compareTo(rightNumber.length)
            }

            val numericCompare = leftNumber.compareTo(rightNumber)
            if (numericCompare != 0) return numericCompare
            continue
        }

        val charCompare = leftChar.lowercaseChar().compareTo(rightChar.lowercaseChar())
        if (charCompare != 0) return charCompare

        leftIndex++
        rightIndex++
    }

    return left.length.compareTo(right.length)
}

private fun normalizeForSort(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC)
        .filterNot { Character.getType(it.code) == Character.FORMAT.toInt() }
        .trim()


@Composable
internal fun FavoriteList(
    songs: List<Song>,
    player: PlayerController,
    preferences: SonoraPreferences,
    open: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
    onChanged: () -> Unit,
) {
    if (songs.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.FavoriteBorder,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "No favorites yet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                "Tap the heart on a song to save it here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
    ) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongCard(
                song = song,
                player = player,
                onClick = { player.playQueue(songs, index); open() },
                preferences = preferences,
                onOpenAlbum = openAlbum,
                onOpenArtist = openArtist,
                onChanged = onChanged,
            )
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    player: PlayerController,
    onClick: () -> Unit,
    preferences: SonoraPreferences,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    actions: SongActionConfig = SongActionConfig(),
    onChanged: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(song, Modifier.size(56.dp))
            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist + " • " + song.album,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SongMoreButton(
                song = song,
                preferences = preferences,
                playerController = player,
                actions = SongActionConfig(
                    showPlayNext = true,
                    showAddToQueue = true,
                    showEditMetadata = actions.showEditMetadata,
                    showRemoveFromDevice = actions.showRemoveFromDevice,
                ),
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
                onChanged = onChanged,
            )
        }
    }
}

@Composable
internal fun AlbumList(songs: List<Song>, openAlbum: (String) -> Unit) {
    val albums = songs.groupBy(Song::album)
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 85.dp)
    ) {
        items(albums.keys.sorted(), key = { it }) { album ->
            val tracks = albums.getValue(album)
            Card(
                onClick = { openAlbum(album) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(tracks.first(), Modifier.size(64.dp))
                    Column(
                        Modifier
                            .padding(start = 12.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            album,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            tracks.first().artist + " • " + songCountLabel(tracks.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                }
            }
        }
    }
}

@Composable
internal fun ArtistList(songs: List<Song>, openArtist: (String) -> Unit) {
    val artists = songs.groupBy(Song::artist)
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 85.dp)
    ) {
        items(artists.keys.sorted(), key = { it }) { artist ->
            val tracks = artists.getValue(artist)
            Card(
                onClick = { openArtist(artist) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        tracks.firstOrNull()?.let {
                            SongArtworkImage(
                                song = it,
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = "Artist artwork",
                            )
                        }
                    }
                    Column(
                        Modifier
                            .padding(start = 12.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            artist,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            songCountLabel(tracks.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaylistList(
    playlists: List<SonoraPlaylist>,
    openPlaylist: (String) -> Unit,
    onRefresh: () -> Unit,
    preferences: SonoraPreferences,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your playlists", style = MaterialTheme.typography.titleLarge)
                Text(
                    playlists.size.toString() + if (playlists.size == 1) " playlist" else " playlists",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            var showCreate by remember { mutableStateOf(false) }
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, "Create playlist", tint = MaterialTheme.colorScheme.primary)
            }
            if (showCreate) {
                CreatePlaylistDialog(
                    onDismiss = { showCreate = false },
                    onCreate = {
                        preferences.createPlaylist(it)
                        showCreate = false
                        onRefresh()
                    },
                )
            }
        }

        if (playlists.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.PlaylistPlay,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    "No playlists yet",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    "Create a playlist to organize your music.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 85.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        onClick = { openPlaylist(playlist.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(58.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        RoundedCornerShape(16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.PlaylistPlay,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(
                                Modifier
                                    .padding(start = 12.dp)
                                    .weight(1f)
                            ) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    playlist.songIds.size.toString() + if (playlist.songIds.size == 1) " song" else " songs",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                preferences.deletePlaylist(playlist.id)
                                onRefresh()
                            }) {
                                Icon(Icons.Default.DeleteOutline, "Delete playlist")
                            }
                        }
                    }
                }
            }
        }
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

@Composable
internal fun Artwork(song: Song, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        SongArtworkImage(
            song = song,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
