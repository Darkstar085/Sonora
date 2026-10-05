package com.sipun.sonora.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongMoreButton
import com.sipun.sonora.ui.components.SongActionConfig
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
internal fun Overview(
    songs: List<Song>,
    favoriteCount: Int,
    playlistCount: Int,
    currentSongId: Long?,
    player: PlayerController,
    preferences: SonoraPreferences,
    open: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
) {
    val albums = songs.map(Song::album).distinct()
    val artists = songs.map(Song::artist).distinct()
    val recentlyAdded = remember(songs) { songs.sortedWith(compareByDescending<Song> { it.dateAddedSeconds }.thenByDescending { it.id }) }
    val songsById = remember(songs) { songs.associateBy(Song::id) }
    val listeningHistory = remember(currentSongId, songs) {
        preferences.listeningHistoryIds().mapNotNull(songsById::get).take(5)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = SonoraRed)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(songs.first(), Modifier.size(100.dp))
                    Column(Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Your library", color = MaterialTheme.colorScheme.onPrimary)
                        Text(songs.size.toString() + " songs", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall)
                        Text("Ready to play", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        FilledTonalButton(
                            onClick = { player.playQueue(songs); open() },
                            modifier = Modifier.padding(top = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = SonoraRed),
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play all")
                        }
                    }
                }
            }
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                item {
                    OverviewMetric("Songs", songs.size, Modifier.width(124.dp), Icons.Default.MusicNote)
                }
                item {
                    OverviewMetric("Albums", albums.size, Modifier.width(124.dp), Icons.Default.Album)
                }
                item {
                    OverviewMetric("Artists", artists.size, Modifier.width(124.dp), Icons.Default.Person)
                }
                item {
                    OverviewMetric("Favorites", favoriteCount, Modifier.width(124.dp), Icons.Default.Favorite)
                }
                item {
                    OverviewMetric("Playlists", playlistCount, Modifier.width(124.dp), Icons.AutoMirrored.Filled.QueueMusic)
                }
            }
        }
        item {
            Text("Recently added", style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 10.dp)) {
                items(recentlyAdded.take(10), key = { it.id }) { song ->
                    Box(Modifier.width(172.dp)) {
                        Card(onClick = { player.playQueue(songs, songs.indexOf(song)); open() }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Artwork(song, Modifier.fillMaxWidth().aspectRatio(1f))
                                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                                Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        SongMoreButton(song=song, preferences=preferences, modifier=Modifier.align(Alignment.TopEnd).padding(4.dp), onOpenAlbum=openAlbum, onOpenArtist=openArtist)
                    }
                }
            }
        }
        if (listeningHistory.isNotEmpty()) {
            item {
                Text("Continue listening", style = MaterialTheme.typography.titleLarge)
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column {
                        listeningHistory.forEachIndexed { index, song ->
                            Card(onClick = { player.playQueue(songs, songs.indexOf(song)); open() }, modifier=Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=Color.Transparent)) {
                                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
                                    Artwork(song, Modifier.size(52.dp))
                                    Column(Modifier.padding(start=12.dp).weight(1f)) {
                                        Text(song.title, style=MaterialTheme.typography.titleMedium, maxLines=1, overflow=TextOverflow.Ellipsis)
                                        Text(song.artist, color=MaterialTheme.colorScheme.onSurfaceVariant, maxLines=1, overflow=TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.PlayArrow, null, tint=SonoraRed)
                                }
                            }
                            if (index < listeningHistory.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun OverviewMetric(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon?.let { Icon(it, null, tint = SonoraRed, modifier = Modifier.size(22.dp)) }
            Text(value.toString(), style = MaterialTheme.typography.titleLarge)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

