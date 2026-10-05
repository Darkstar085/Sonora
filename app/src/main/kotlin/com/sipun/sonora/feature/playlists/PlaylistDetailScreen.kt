@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.theme.SonoraRed
import com.sipun.sonora.ui.components.SongMoreButton
import com.sipun.sonora.ui.components.SongActionConfig

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playerController: PlayerController,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { AndroidMusicRepository(context.contentResolver) }
    val preferences = remember(context) { SonoraPreferences(context) }
    var playlist by remember { mutableStateOf(preferences.playlists().firstOrNull { it.id == playlistId }) }
    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(Unit) { allSongs = repository.songs() }

    val songs = playlist?.songIds?.mapNotNull { id -> allSongs.firstOrNull { it.id == id } }.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Playlist") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        if (playlist == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Playlist not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(220.dp).clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = SonoraRed, modifier = Modifier.size(72.dp))
                        }
                        Text(
                            playlist?.name ?: "Playlist",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        Text(
                            songs.size.toString() + if (songs.size == 1) " song" else " songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        if (songs.isNotEmpty()) {
                            FilledIconButton(
                                onClick = { playerController.playQueue(songs); onOpenNowPlaying() },
                                modifier = Modifier.padding(top = 12.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SonoraRed),
                            ) { Icon(Icons.Default.PlayArrow, "Play playlist") }
                        }
                    }
                }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    Card(
                        onClick = { playerController.playQueue(songs, index); onOpenNowPlaying() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text((index + 1).toString().padStart(2, '0'), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(34.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist + " • " + song.album, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            SongMoreButton(
                                song = song,
                                preferences = preferences,
                                playerController = playerController,
                                actions = SongActionConfig(
                                showPlaylist = false,
                                showRemoveFromPlaylist = true,
                                showPlayNext = true,
                                showAddToQueue = true,
                            ),
                                onOpenAlbum = onOpenAlbum,
                                onOpenArtist = onOpenArtist,
                                onRemoveFromPlaylist = {
                                    preferences.removeFromPlaylist(playlistId, song.id)
                                    playlist = preferences.playlists().firstOrNull { it.id == playlistId }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
