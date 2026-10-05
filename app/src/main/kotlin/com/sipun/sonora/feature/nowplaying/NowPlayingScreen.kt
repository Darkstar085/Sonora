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

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(78.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(54.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                color = SonoraRed.copy(alpha = 0.08f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint = SonoraRed)
                }
            }
        }
    }
}

@Composable
private fun MoreOptionsSheet(
    onDismiss: () -> Unit,
    onAlbum: () -> Unit,
    onArtist: () -> Unit,
    onPlaylist: () -> Unit,
    onRingtone: () -> Unit,
    onSleepTimer: () -> Unit,
    onSpeed: () -> Unit,
    onEqualizer: () -> Unit,
    onInfo: () -> Unit,
    onRemove: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                "More options",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            MoreOption(Icons.Default.Album, "Go to album", onAlbum)
            MoreOption(Icons.Default.Person, "Go to artist", onArtist)
            MoreOption(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist", onPlaylist)
            MoreOption(Icons.Default.Notifications, "Set as ringtone", onRingtone)
            MoreOption(Icons.Default.Timer, "Sleep timer", onSleepTimer)
            MoreOption(Icons.Default.Speed, "Playback speed", onSpeed)
            MoreOption(Icons.Default.Equalizer, "Equalizer", onEqualizer)
            MoreOption(Icons.Default.Info, "Show song info", onInfo)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MoreOption(Icons.Default.DeleteOutline, "Remove from library", onRemove, destructive = true)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MoreOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    ListItem(
        headlineContent = { Text(label, color = if (destructive) SonoraRed else LocalContentColor.current) },
        leadingContent = { Icon(icon, null, tint = if (destructive) SonoraRed else LocalContentColor.current) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun QueueDialog(
    queue: List<Song>,
    currentId: Long?,
    onSelect: (Song) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Queue") },
        text = {
            if (queue.isEmpty()) Text("The current queue is not available.")
            else Column(Modifier.heightIn(max = 420.dp)) {
                queue.forEach { song ->
                    TextButton(onClick = { onSelect(song) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                song.artist,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ArtistDialog(
    artist: String,
    songs: List<Song>,
    onSelect: (Song) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(artist.ifBlank { "Unknown artist" }) },
        text = {
            if (songs.isEmpty()) {
                Text("Artist view is based on the current playback queue.")
            } else {
                Column(Modifier.heightIn(max = 420.dp)) {
                    songs.forEach { song ->
                        TextButton(onClick = { onSelect(song) }, modifier = Modifier.fillMaxWidth()) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SongInfoDialog(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Song info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Title", song.title)
                InfoRow("Artist", song.artist)
                InfoRow("Album", song.album)
                song.genre?.let { InfoRow("Genre", it) }
                song.year?.let { InfoRow("Year", it.toString()) }
                song.trackNumber?.let { InfoRow("Track", it.toString()) }
                song.folder?.let { InfoRow("Folder", it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            Column {
                speeds.forEach { speed ->
                    TextButton(
                        onClick = { onSelect(speed) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (speed == currentSpeed) "✓  " else "    " + speed.toString() + "×")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SleepTimerDialog(
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                options.forEach { minutes ->
                    TextButton(onClick = { onSelect(minutes) }, modifier = Modifier.fillMaxWidth()) {
                        Text("$minutes minutes")
                    }
                }
                TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel timer")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun setAsRingtone(context: android.content.Context, song: Song) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + context.packageName),
            ),
        )
        Toast.makeText(context, "Allow Sonora to change system settings, then try again.", Toast.LENGTH_LONG).show()
        return
    }

    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            val resolver = context.contentResolver
            val source = Uri.parse(song.uri)
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "Sonora_" + song.title.replace("/", "_") + ".mp3")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/*")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Ringtones/")
                put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: error("Unable to create ringtone")
            resolver.openInputStream(source).use { input ->
                resolver.openOutputStream(uri).use { output ->
                    requireNotNull(input)
                    requireNotNull(output)
                    input.copyTo(output)
                }
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            withContext(Dispatchers.Main) {
                RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, uri)
                Toast.makeText(context, "Ringtone set", Toast.LENGTH_SHORT).show()
            }
        }.onFailure {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Unable to set ringtone", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun openEqualizer(context: android.content.Context) {
    val intent = Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
    }
}

