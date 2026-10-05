@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SonoraSearchBar
import com.sipun.sonora.ui.theme.SonoraRed

private val tabs = listOf("Overview", "Songs", "Albums", "Artists")

@Composable
fun HomeScreen(playerController: PlayerController, onOpenNowPlaying: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { AndroidMusicRepository(context.contentResolver) }
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(Unit) { songs = repository.songs() }

    val filtered = songs.filter {
        query.isBlank() ||
            it.title.contains(query, true) ||
            it.artist.contains(query, true) ||
            it.album.contains(query, true)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Listening", style = MaterialTheme.typography.headlineSmall) },
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "Notifications") }
                IconButton(onClick = {}) { Icon(Icons.Default.Person, "Profile") }
            },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SonoraSearchBar(value = query, onValueChange = { query = it })
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp, divider = {}) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(label) },
                        selectedContentColor = SonoraRed,
                    )
                }
            }
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (tab) {
                    0 -> Overview(songs, playerController, onOpenNowPlaying)
                    1 -> SongList(filtered, playerController, onOpenNowPlaying)
                    2 -> AlbumList(filtered, playerController, onOpenNowPlaying)
                    else -> ArtistList(filtered, playerController, onOpenNowPlaying)
                }
            }
        }
    }
}

@Composable
private fun Overview(songs: List<Song>, player: PlayerController, open: () -> Unit) {
    val albums = songs.map(Song::album).distinct()
    val artists = songs.map(Song::artist).distinct()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { Text(songs.size.toString() + " songs • " + albums.size + " albums • " + artists.size + " artists", style = MaterialTheme.typography.titleMedium) }
        item {
            Text("Your music", style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 8.dp)) {
                items(songs.take(10), key = { it.id }) { song ->
                    Card(onClick = { player.playQueue(songs, songs.indexOf(song)); open() }, modifier = Modifier.width(180.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Icon(Icons.Default.MusicNote, null, tint = SonoraRed)
                            Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
        item { Text("Browse", style = MaterialTheme.typography.titleLarge) }
        item { Text("Use Songs, Albums, and Artists tabs to start playback.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SongList(songs: List<Song>, player: PlayerController, open: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            Card(onClick = { player.playQueue(songs, index); open() }, Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(song, Modifier.size(56.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium)
                        Text(song.artist + " • " + song.album, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumList(songs: List<Song>, player: PlayerController, open: () -> Unit) {
    val albums = songs.groupBy(Song::album)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        items(albums.keys.sorted(), key = { it }) { album ->
            val tracks = albums.getValue(album)
            Card(onClick = { player.playQueue(tracks); open() }, Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(tracks.first(), Modifier.size(56.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(album, style = MaterialTheme.typography.titleMedium)
                        Text(tracks.first().artist + " • " + tracks.size + " songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistList(songs: List<Song>, player: PlayerController, open: () -> Unit) {
    val artists = songs.groupBy(Song::artist)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        items(artists.keys.sorted(), key = { it }) { artist ->
            val tracks = artists.getValue(artist)
            Card(onClick = { player.playQueue(tracks); open() }, Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = SonoraRed)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(artist, style = MaterialTheme.typography.titleMedium)
                        Text(tracks.size.toString() + " songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Artwork(song: Song, modifier: Modifier) {
    Box(modifier) {
        Icon(Icons.Default.Album, null, tint = SonoraRed, modifier = Modifier.fillMaxSize().padding(12.dp))
        song.albumArtUri?.let { coil3.compose.AsyncImage(model = it, contentDescription = "Album artwork", modifier = Modifier.fillMaxSize()) }
    }
}
