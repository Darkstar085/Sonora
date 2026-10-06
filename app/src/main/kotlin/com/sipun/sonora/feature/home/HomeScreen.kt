@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SonoraSearchBar
import kotlinx.coroutines.launch

private val tabs = listOf(
    R.string.tab_overview,
    R.string.tab_songs,
    R.string.tab_albums,
    R.string.tab_artists,
    R.string.tab_favorites,
    R.string.tab_playlists,
)

private val tabIcons = listOf(
    Icons.Default.Home,
    Icons.Default.MusicNote,
    Icons.Default.Album,
    Icons.Default.Person,
    Icons.Default.Favorite,
    Icons.AutoMirrored.Filled.QueueMusic,
)

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
    val metadataRefreshVersion by AndroidMusicRepository.refreshVersion.collectAsState()

    LaunchedEffect(metadataRefreshVersion) {
        repository.invalidateCache()
        isLoading = true
        loadError = null
        try {
            songs = repository.songs()
            favoriteIds = preferences.favoriteIds()
            playlists = preferences.playlists()
        } catch (error: SecurityException) {
            loadError = context.getString(R.string.home_permission_error)
        } catch (error: Exception) {
            loadError = error.message ?: context.getString(R.string.home_load_error)
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
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            stringResource(R.string.home_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = AndroidMusicRepository::notifyMetadataChanged,
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh library",
                        )
                    }
                    IconButton(
                        onClick = {
                            searchActive = !searchActive
                            if (searchActive) scope.launch { pagerState.animateScrollToPage(1) } else query =
                                ""
                        },
                    ) {
                        Icon(
                            if (searchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = stringResource(
                                if (searchActive) R.string.action_close_search else R.string.action_search_music
                            ),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.action_settings))
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, label ->
                    val selected = pagerState.currentPage == index

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        ) {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = stringResource(label),
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(3.dp)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (loadError != null) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.home_load_title),
                        style = MaterialTheme.typography.titleLarge
                    )
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
                        0 -> Overview(
                            songs,
                            favoriteIds.size,
                            playlists.size,
                            playerState.currentSong?.id,
                            playerController,
                            preferences,
                            onOpenNowPlaying,
                            onOpenAlbum,
                            onOpenArtist
                        )

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
                                    repository.invalidateCache()
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
        Icon(
            Icons.Default.MusicOff,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            stringResource(R.string.home_no_music),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            stringResource(R.string.home_add_music),
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
        Icon(
            Icons.Default.SearchOff,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            stringResource(R.string.home_no_music),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            stringResource(R.string.home_no_search_results, query),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun songCountLabel(count: Int): String =
    if (count == 1) {
        stringResource(R.string.song_count_one)
    } else {
        stringResource(R.string.song_count_other, count)
    }
