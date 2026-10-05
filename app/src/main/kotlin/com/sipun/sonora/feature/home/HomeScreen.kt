@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
