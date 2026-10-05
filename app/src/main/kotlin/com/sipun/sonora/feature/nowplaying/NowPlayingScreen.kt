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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.player.RepeatMode
import com.sipun.sonora.ui.components.AddToPlaylistDialog
import com.sipun.sonora.ui.theme.SonoraBackground
import com.sipun.sonora.ui.theme.SonoraRed
import com.sipun.sonora.ui.theme.SonoraSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NowPlayingScreen(
    playerController: PlayerController,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { SonoraPreferences(context) }
    val state by playerController.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showArtist by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var favorite by remember(state.currentSong?.id) {
        mutableStateOf(state.currentSong?.id?.let { it in preferences.favoriteIds() } == true)
    }
    var sleepTimerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(Unit) {
        while (isActive) {
            playerController.refresh()
            delay(500)
        }
    }

    if (showPlaylistPicker) {
        state.currentSong?.let { song ->
            AddToPlaylistDialog(song.id, preferences) { showPlaylistPicker = false }
        }
    }

    if (showMore) {
        MoreOptionsSheet(
            onDismiss = { showMore = false },
            onAlbum = {
                showMore = false
                state.currentSong?.let { onOpenAlbum(it.album) }
            },
            onArtist = {
                showMore = false
                showArtist = true
            },
            onPlaylist = {
                showMore = false
                showPlaylistPicker = true
            },
            onRingtone = {
                showMore = false
                state.currentSong?.let { setAsRingtone(context, it) }
            },
            onSleepTimer = {
                showMore = false
                showSleepTimer = true
            },
            onSpeed = {
                showMore = false
                showSpeed = true
            },
            onEqualizer = {
                showMore = false
                openEqualizer(context)
            },
            onInfo = {
                showMore = false
                showInfo = true
            },
            onRemove = {
                showMore = false
                showRemoveConfirmation = true
            },
        )
    }

    if (showQueue) {
        QueueDialog(
            queue = state.queue,
            currentId = state.currentSong?.id,
            onSelect = { song ->
                val index = state.queue.indexOfFirst { it.id == song.id }
                if (index >= 0) playerController.playQueue(state.queue, index)
                showQueue = false
            },
            onDismiss = { showQueue = false },
        )
    }

    if (showArtist) {
        ArtistDialog(
            artist = state.currentSong?.artist.orEmpty(),
            songs = state.queue.filter { it.artist == state.currentSong?.artist },
            onSelect = { song ->
                val index = state.queue.indexOfFirst { it.id == song.id }
                if (index >= 0) playerController.playQueue(state.queue, index)
                showArtist = false
            },
            onDismiss = { showArtist = false },
        )
    }

    if (showInfo) {
        state.currentSong?.let { song ->
            SongInfoDialog(song, onDismiss = { showInfo = false })
        }
    }

    if (showSpeed) {
        PlaybackSpeedDialog(
            currentSpeed = 1f,
            onSelect = {
                playerController.setPlaybackSpeed(it)
                showSpeed = false
            },
            onDismiss = { showSpeed = false },
        )
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            onSelect = { minutes ->
                sleepTimerJob?.cancel()
                if (minutes != null) {
                    sleepTimerJob = scope.launch {
                        delay(minutes * 60_000L)
                        playerController.togglePlayPause()
                    }
                }
                showSleepTimer = false
            },
            onDismiss = { showSleepTimer = false },
        )
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text("Remove from library?") },
            text = { Text("This removes the audio file from the device's media library.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val song = state.currentSong
                        if (song != null) {
                            scope.launch {
                                val removed = withContext(Dispatchers.IO) {
                                    runCatching {
                                        context.contentResolver.delete(Uri.parse(song.uri), null, null) > 0
                                    }.getOrDefault(false)
                                }
                                Toast.makeText(
                                    context,
                                    if (removed) "Removed from library" else "Unable to remove this song",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                showRemoveConfirmation = false
                                if (removed) playerController.skipNext()
                            }
                        }
                    },
                ) { Text("Remove", color = SonoraRed) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = SonoraBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SonoraBackground),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {},
            )
        },
    ) { padding ->
        val song = state.currentSong
        val max = state.durationMs.coerceAtLeast(1L)

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = song?.id,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.94f))
                        .togetherWith(fadeOut() + scaleOut(targetScale = 1.04f))
                        .using(SizeTransform(clip = false))
                },
                label = "now_playing_track_transition",
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            song?.title ?: "Select a song from your library",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    initialDelayMillis = 1_000,
                                    repeatDelayMillis = 1_000,
                                ),
                        )
                    }
                    Text(
                        song?.artist ?: "Sonora",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            AnimatedContent(
                targetState = song?.id,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.96f))
                        .togetherWith(fadeOut() + scaleOut(targetScale = 1.03f))
                        .using(SizeTransform(clip = false))
                },
                label = "album_art_transition",
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.82f)
                        .widthIn(max = 340.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Album, null, tint = SonoraRed, modifier = Modifier.size(72.dp))
                    song?.albumArtUri?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Album artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            Spacer(Modifier.height(38.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                shape = RoundedCornerShape(28.dp),
                color = SonoraSurface,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = state.positionMs.coerceIn(0L, max).toFloat(),
                        onValueChange = { playerController.seekTo(it.toLong()) },
                        valueRange = 0f..max.toFloat(),
                        enabled = song != null && state.durationMs > 0,
                        colors = SliderDefaults.colors(
                            thumbColor = SonoraRed,
                            activeTrackColor = SonoraRed,
                        ),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatPlaybackTime(state.positionMs), style = MaterialTheme.typography.labelMedium)
                        Text(formatPlaybackTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(Modifier.height(34.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = playerController::cycleRepeat) {
                            Icon(
                                if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                "Repeat " + state.repeatMode.name.lowercase(),
                                tint = if (state.repeatMode != RepeatMode.OFF) SonoraRed else LocalContentColor.current,
                            )
                        }
                        IconButton(
                            onClick = playerController::skipPrevious,
                            enabled = state.hasPrevious || state.positionMs > 3_000L,
                        ) {
                            Icon(Icons.Default.SkipPrevious, "Previous")
                        }
                        FilledIconButton(
                            onClick = playerController::togglePlayPause,
                            enabled = song != null,
                            modifier = Modifier.size(72.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = SonoraRed,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (state.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        IconButton(onClick = playerController::skipNext, enabled = state.hasNext) {
                            Icon(Icons.Default.SkipNext, "Next")
                        }
                        IconButton(onClick = playerController::toggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle,
                                "Shuffle",
                                tint = if (state.shuffleEnabled) SonoraRed else LocalContentColor.current,
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuickAction(Icons.Default.Description, "Lyrics") {
                            Toast.makeText(context, "Lyrics are not available for this song", Toast.LENGTH_SHORT).show()
                        }
                        QuickAction(Icons.AutoMirrored.Filled.QueueMusic, "Queue") { showQueue = true }
                        QuickAction(
                            if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Add to Favorite",
                        ) {
                            song?.let { favorite = preferences.toggleFavorite(it.id) }
                        }
                        QuickAction(Icons.Default.MoreHoriz, "More") { showMore = true }
                    }
                }
            }
        }
    }
}

