package com.sipun.sonora.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongMoreButton

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
    val recentlyAdded =
        remember(songs) { songs.sortedWith(compareByDescending<Song> { it.dateAddedSeconds }.thenByDescending { it.id }) }
    val songsById = remember(songs) { songs.associateBy(Song::id) }
    val listeningHistory = remember(currentSongId, songs) {
        preferences.listeningHistoryIds().mapNotNull(songsById::get).take(5)
    }
    val libraryArtwork =
        songsById[currentSongId] ?: recentlyAdded.firstOrNull() ?: songs.firstOrNull()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 85.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    libraryArtwork?.let { Artwork(it, Modifier.size(100.dp)) }
                    Column(
                        Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.your_library),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            stringResource(R.string.song_count_other, songs.size),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            stringResource(R.string.ready_to_play),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        FilledTonalButton(
                            onClick = { player.playQueueShuffled(songs); open() },
                            modifier = Modifier.padding(top = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.play_all))
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
                    OverviewMetric(
                        stringResource(R.string.tab_songs),
                        songs.size,
                        Modifier.width(124.dp),
                        Icons.Default.MusicNote
                    )
                }
                item {
                    OverviewMetric(
                        stringResource(R.string.tab_albums),
                        albums.size,
                        Modifier.width(124.dp),
                        Icons.Default.Album
                    )
                }
                item {
                    OverviewMetric(
                        stringResource(R.string.tab_artists),
                        artists.size,
                        Modifier.width(124.dp),
                        Icons.Default.Person
                    )
                }
                item {
                    OverviewMetric(
                        stringResource(R.string.tab_favorites),
                        favoriteCount,
                        Modifier.width(124.dp),
                        Icons.Default.Favorite
                    )
                }
                item {
                    OverviewMetric(
                        stringResource(R.string.tab_playlists),
                        playlistCount,
                        Modifier.width(124.dp),
                        Icons.AutoMirrored.Filled.PlaylistPlay
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.recently_added),
                style = MaterialTheme.typography.titleLarge
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 10.dp)
            ) {
                items(recentlyAdded.take(10), key = { it.id }) { song ->
                    Box(Modifier.width(172.dp)) {
                        Card(
                            onClick = { player.playQueue(songs, songs.indexOf(song)); open() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Artwork(
                                    song, Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                )
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                                Text(
                                    song.artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        SongMoreButton(
                            song = song,
                            preferences = preferences,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            onOpenAlbum = openAlbum,
                            onOpenArtist = openArtist
                        )
                    }
                }
            }
        }
        if (listeningHistory.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.continue_listening),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.size(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column {
                        listeningHistory.forEachIndexed { index, song ->
                            Card(
                                onClick = { player.playQueue(songs, songs.indexOf(song)); open() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Artwork(song, Modifier.size(52.dp))
                                    Column(
                                        Modifier
                                            .padding(start = 12.dp)
                                            .weight(1f)
                                    ) {
                                        Text(
                                            song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            song.artist,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
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
            icon?.let {
                Icon(
                    it,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(value.toString(), style = MaterialTheme.typography.titleLarge)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

