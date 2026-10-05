@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.data.preferences.SonoraPlaylist
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongMoreButton
import com.sipun.sonora.ui.components.SonoraSearchBar
import com.sipun.sonora.ui.theme.SonoraRed

private val tabs = listOf("Overview", "Songs", "Albums", "Artists", "Favorites", "Playlists")

@Composable
fun HomeScreen(
    playerController: PlayerController,
    onOpenNowPlaying: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { AndroidMusicRepository(context.contentResolver) }
    val preferences = remember(context) { SonoraPreferences(context) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var favoriteIds by remember { mutableStateOf(preferences.favoriteIds()) }
    var playlists by remember { mutableStateOf(preferences.playlists()) }
    val playerState by playerController.state.collectAsState()

    LaunchedEffect(Unit) {
        isLoading = true
        loadError = null
        try {
            songs = repository.songs()
            favoriteIds = preferences.favoriteIds()
            playlists = preferences.playlists()
        } catch (error: SecurityException) {
            loadError = "Sonora does not have permission to read your music."
        } catch (error: Exception) {
            loadError = error.message ?: "Unable to load your music library."
        } finally {
            isLoading = false
        }
    }

    val filtered = songs.filter {
        query.isBlank() ||
            it.title.contains(query, true) ||
            it.artist.contains(query, true) ||
            it.album.contains(query, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Column {
                        Text("Sonora", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Your music, simply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            searchActive = !searchActive
                            if (searchActive) scope.launch { pagerState.animateScrollToPage(1) } else query = ""
                        },
                    ) {
                        Icon(
                            if (searchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (searchActive) "Close search" else "Search music",
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (searchActive) {
                SonoraSearchBar(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = SonoraRed,
                edgePadding = 0.dp,
                divider = {},
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(label) },
                        selectedContentColor = SonoraRed,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SonoraRed)
                }
            } else if (loadError != null) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Unable to load music", style = MaterialTheme.typography.titleLarge)
                    Text(
                        loadError.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else if (songs.isEmpty()) {
                EmptyLibraryState()
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> Overview(songs, favoriteIds.size, playlists.size, playerState.currentSong?.id, playerController, preferences, onOpenNowPlaying, onOpenAlbum, onOpenArtist)
                        1 -> if (filtered.isEmpty() && query.isNotBlank()) EmptySearchState(query)
                        else SongList(
                            filtered,
                            playerController,
                            preferences,
                            onOpenNowPlaying,
                            onOpenAlbum,
                            onOpenArtist,
                            onChanged = {
                                scope.launch {
                                    songs = repository.songs()
                                    favoriteIds = preferences.favoriteIds()
                                    playlists = preferences.playlists()
                                }
                            },
                        )
                        2 -> AlbumList(filtered, onOpenAlbum)
                        3 -> ArtistList(filtered, onOpenArtist)
                        4 -> FavoriteList(
                            songs.filter { it.id in favoriteIds },
                            playerController,
                            preferences,
                            onOpenNowPlaying,
                            onOpenAlbum,
                            onOpenArtist,
                        ) {
                            favoriteIds = preferences.favoriteIds()
                        }
                        else -> PlaylistList(playlists, onOpenPlaylist, onRefresh = {
                            playlists = preferences.playlists()
                        }, preferences = preferences)
                    }
                }
            }
        }
    }
}

@Composable
private fun Overview(
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

@Composable
private fun SongList(
    songs: List<Song>,
    player: PlayerController,
    preferences: SonoraPreferences,
    open: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
    onChanged: () -> Unit,
) {
    LazyColumn(
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
                showRemoveFromDevice = true,
                onChanged = onChanged,
            )
        }
    }
}

@Composable
private fun FavoriteList(
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
            Icon(Icons.Default.FavoriteBorder, null, tint = SonoraRed, modifier = Modifier.size(56.dp))
            Text("No favorites yet", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 14.dp))
            Text(
                "Tap the heart on a song to save it here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        return
    }

    LazyColumn(
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
    showRemoveFromDevice: Boolean = false,
    onChanged: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(song, Modifier.size(56.dp))
            Column(
                Modifier.padding(start = 12.dp).weight(1f),
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
                showPlayNext = true,
                showAddToQueue = true,
                showRemoveFromDevice = showRemoveFromDevice,
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
                onChanged = onChanged,
            )
        }
    }
}

@Composable
private fun AlbumList(songs: List<Song>, openAlbum: (String) -> Unit) {
    val albums = songs.groupBy(Song::album)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 85.dp)) {
        items(albums.keys.sorted(), key = { it }) { album ->
            val tracks = albums.getValue(album)
            Card(
                onClick = { openAlbum(album) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(tracks.first(), Modifier.size(64.dp))
                    Column(
                        Modifier.padding(start = 12.dp).weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(album, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
private fun ArtistList(songs: List<Song>, openArtist: (String) -> Unit) {
    val artists = songs.groupBy(Song::artist)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 85.dp)) {
        items(artists.keys.sorted(), key = { it }) { artist ->
            val tracks = artists.getValue(artist)
            Card(
                onClick = { openArtist(artist) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Person, null, tint = SonoraRed)
                        tracks.firstOrNull()?.albumArtUri?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = "Artist artwork",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Column(
                        Modifier.padding(start = 12.dp).weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(artist, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(songCountLabel(tracks.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<SonoraPlaylist>,
    openPlaylist: (String) -> Unit,
    onRefresh: () -> Unit,
    preferences: SonoraPreferences,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
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
                Icon(Icons.Default.Add, "Create playlist", tint = SonoraRed)
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
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = SonoraRed, modifier = Modifier.size(56.dp))
                Text("No playlists yet", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 14.dp))
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
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = SonoraRed)
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
private fun Artwork(song: Song, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Album, null, tint = SonoraRed, modifier = Modifier.size(44.dp))
        song.albumArtUri?.let {
            AsyncImage(
                model = it,
                contentDescription = "Album artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun EmptyLibraryState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.MusicOff, null, tint = SonoraRed, modifier = Modifier.size(48.dp))
        Text("No music found", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Text(
            "Add music to your device and try again.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.SearchOff, null, tint = SonoraRed, modifier = Modifier.size(48.dp))
        Text("No music found", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Text(
            "Nothing matches \"" + query + "\".",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun songCountLabel(count: Int): String = if (count == 1) "1 song" else count.toString() + " songs"
