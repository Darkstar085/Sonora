package com.sipun.sonora.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.player.PlayerState
import com.sipun.sonora.ui.components.SongMoreButton

@Composable
internal fun Overview(
    songs: List<Song>,
    favoriteCount: Int,
    playlistCount: Int,
    currentSongId: Long?,
    playerState: PlayerState,
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

    val nowPlayingSong = playerState.currentSong
    val showNowPlaying = nowPlayingSong != null

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LibraryLottieCard(
                songCount = songs.size,
                nowPlayingSong = nowPlayingSong,
                state = playerState,
                player = player,
                onOpenNowPlaying = open,
                onPlayAll = {
                    player.playQueueShuffled(songs)
                },
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverviewMetric(
                        stringResource(R.string.tab_songs),
                        songs.size,
                        Modifier.weight(1f),
                        Icons.Default.MusicNote,
                    )
                    OverviewMetricDivider()
                    OverviewMetric(
                        stringResource(R.string.tab_albums),
                        albums.size,
                        Modifier.weight(1f),
                        Icons.Default.Album,
                    )
                    OverviewMetricDivider()
                    OverviewMetric(
                        stringResource(R.string.tab_artists),
                        artists.size,
                        Modifier.weight(1f),
                        Icons.Default.Person,
                    )
                    OverviewMetricDivider()
                    OverviewMetric(
                        stringResource(R.string.tab_favorites),
                        favoriteCount,
                        Modifier.weight(1f),
                        Icons.Default.Favorite,
                    )
                    OverviewMetricDivider()
                    OverviewMetric(
                        stringResource(R.string.tab_playlists),
                        playlistCount,
                        Modifier.weight(1f),
                        Icons.AutoMirrored.Filled.PlaylistPlay,
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
                contentPadding = PaddingValues(top = 10.dp),
            ) {
                items(recentlyAdded.take(10), key = { it.id }) { song ->
                    Box(Modifier.width(188.dp)) {
                        Card(
                            onClick = { player.playQueue(songs, songs.indexOf(song)); open() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Box {
                                    Artwork(
                                        song,
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp)),
                                    )
                                    FilledIconButton(
                                        onClick = {
                                            player.playQueue(songs, songs.indexOf(song))
                                            open()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(38.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                        )
                                    }
                                }
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                                Text(
                                    song.artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
                            onOpenArtist = openArtist,
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column {
                        listeningHistory.forEachIndexed { index, song ->
                            val isCurrentSong = song.id == currentSongId
                            val durationMs = if (isCurrentSong) {
                                playerState.durationMs
                            } else {
                                song.durationMs
                            }
                            val positionMs = if (isCurrentSong) {
                                playerState.positionMs
                            } else {
                                preferences.lastPlayedPositionMs(song.id)
                            }
                            val progress = if (durationMs > 0L) {
                                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            Card(
                                onClick = { player.playQueue(songs, songs.indexOf(song)); open() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Artwork(
                                        song,
                                        Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                    )
                                    Column(
                                        Modifier
                                            .padding(start = 12.dp)
                                            .weight(1f),
                                    ) {
                                        Text(
                                            song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            song.artist,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(7.dp))
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                ),
                                        ) {
                                            if (progress > 0f) {
                                                Box(
                                                    Modifier
                                                        .fillMaxWidth(progress)
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary),
                                                )
                                            }
                                        }
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            player.playQueue(songs, songs.indexOf(song))
                                            open()
                                        },
                                        modifier = Modifier
                                            .padding(start = 10.dp)
                                            .size(44.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                        )
                                    }
                                }
                            }
                            if (index < listeningHistory.lastIndex) HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryLottieCard(
    songCount: Int,
    nowPlayingSong: Song?,
    state: PlayerState,
    player: PlayerController,
    onOpenNowPlaying: () -> Unit,
    onPlayAll: () -> Unit,
) {
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        onClick = if (nowPlayingSong != null) onOpenNowPlaying else onPlayAll,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        if (nowPlayingSong != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(nowPlayingSong, Modifier.size(116.dp))
                    Column(
                        Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            if (state.isPlaying) "Now playing" else "Paused",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            nowPlayingSong.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            nowPlayingSong.artist,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                ExpressiveHomeSeekBar(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    enabled = state.durationMs > 0L,
                    onSeek = player::seekTo,
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        formatTime(state.positionMs),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        formatTime(state.durationMs),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val controlBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    val activeBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

                    IconButton(
                        onClick = player::cycleRepeat,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.repeatMode == com.sipun.sonora.player.RepeatMode.OFF) {
                                    controlBackground
                                } else {
                                    activeBackground
                                }
                            ),
                    ) {
                        Icon(
                            if (state.repeatMode == com.sipun.sonora.player.RepeatMode.ONE) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode == com.sipun.sonora.player.RepeatMode.OFF) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    IconButton(
                        onClick = player::skipPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(controlBackground),
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        )
                    }
                    FilledIconButton(
                        onClick = player::togglePlayPause,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                        )
                    }
                    IconButton(
                        onClick = player::skipNext,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(controlBackground),
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        )
                    }
                    IconButton(
                        onClick = player::toggleShuffle,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.shuffleEnabled) activeBackground else controlBackground
                            ),
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                            },
                        )
                    }
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StaticVinyl(
                    modifier = Modifier.size(108.dp),
                )
                Column(
                    Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(R.string.your_library),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.song_count_other, songCount),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.ready_to_play),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                    FilledTonalButton(
                        onClick = onPlayAll,
                        modifier = Modifier.padding(top = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
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
}

@Composable
private fun ExpressiveHomeSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val primary = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                }
            }
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSeek(
                            ((change.position.x / size.width).coerceIn(
                                0f,
                                1f
                            ) * durationMs).toLong()
                        )
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val thumbX = size.width * progress
            val wavePath = androidx.compose.ui.graphics.Path()
            val waveLength = 34.dp.toPx()
            val amplitude = 4.dp.toPx()
            wavePath.moveTo(0f, centerY)

            var x = 0f
            while (x <= thumbX) {
                val phase = (x / waveLength) * (2f * kotlin.math.PI).toFloat()
                wavePath.lineTo(x, centerY + kotlin.math.sin(phase) * amplitude)
                x += 2.dp.toPx()
            }

            if (thumbX > 0f) {
                drawPath(
                    path = wavePath,
                    color = primary,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
            if (thumbX < size.width) {
                drawLine(
                    color = track,
                    start = androidx.compose.ui.geometry.Offset(thumbX, centerY),
                    end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
            drawLine(
                color = primary,
                start = androidx.compose.ui.geometry.Offset(thumbX, centerY - 11.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(thumbX, centerY + 11.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun StaticVinyl(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.40f
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(
                        Color(0xFF303036),
                        Color(0xFF111114),
                    ),
                ),
                radius = radius,
            )
            repeat(7) { index ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.045f),
                    radius = radius * (0.30f + index * 0.10f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                )
            }
            drawCircle(
                color = primary,
                radius = radius * 0.28f,
            )
            drawCircle(
                color = Color(0xFF17171A),
                radius = radius * 0.07f,
            )
        }
    }
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun OverviewMetric(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(
        modifier
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            icon?.let {
                Icon(
                    it,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OverviewMetricDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(44.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

