@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.sipun.sonora.feature.nowplaying

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongArtworkImage
import com.sipun.sonora.player.RepeatMode
import com.sipun.sonora.ui.components.AddToPlaylistDialog
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
    onOpenLyrics: () -> Unit,
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
    var showEditMetadata by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var favorite by remember(state.currentSong?.id) {
        mutableStateOf(state.currentSong?.id?.let { it in preferences.favoriteIds() } == true)
    }
    var sleepTimerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var audioInfo by remember(state.currentSong?.uri) {
        mutableStateOf<AudioInfo?>(null)
    }

    LaunchedEffect(state.currentSong?.uri) {
        audioInfo = state.currentSong?.uri?.let { uri ->
            withContext(Dispatchers.IO) { loadAudioInfo(context, uri) }
        }
    }

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
            onEditMetadata = {
                showMore = false
                showEditMetadata = true
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

    if (showEditMetadata) {
        state.currentSong?.let { song ->
            com.sipun.sonora.ui.components.MetadataEditorDialog(
                song = song,
                onDismiss = { showEditMetadata = false },
                onSaved = { updatedSong ->
                    showEditMetadata = false
                    playerController.updateCurrentSong(updatedSong)
                },
            )
        }
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
                                        context.contentResolver.delete(
                                            Uri.parse(song.uri),
                                            null,
                                            null
                                        ) > 0
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
                ) { Text("Remove", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
                            .height(48.dp),
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
                        modifier = Modifier.padding(top = 0.dp),
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
                    song?.let {
                        SongArtworkImage(
                            song = it,
                            modifier = Modifier.fillMaxSize(),
                            artworkData = state.artworkData,
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
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))

                    ExpressiveSeekBar(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        isPlaying = state.isPlaying,
                        enabled = song != null && state.durationMs > 0,
                        onSeek = playerController::seekTo,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            formatPlaybackTime(state.positionMs),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            formatPlaybackTime(state.durationMs),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    audioInfo?.let { info ->
                        AudioInfoPill(
                            info = info,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = playerController::cycleRepeat) {
                            Icon(
                                if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                "Repeat " + state.repeatMode.name.lowercase(),
                                tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current,
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
                                containerColor = MaterialTheme.colorScheme.primary,
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
                                tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuickAction(Icons.Default.Description, "Lyrics") { onOpenLyrics() }
                        QuickAction(Icons.AutoMirrored.Filled.QueueMusic, "Queue") {
                            showQueue = true
                        }
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
private fun ExpressiveSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
) {
    val progress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val primary = if (isPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White
    }
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(enabled, durationMs) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * durationMs).toLong())
                    }
                }
                .pointerInput(enabled, durationMs) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val centerY = size.height / 2f
                val trackStart = 0f
                val trackEnd = size.width
                val thumbX = trackStart + (trackEnd - trackStart) * progress

                val wavePath = androidx.compose.ui.graphics.Path()
                val waveLength = 34.dp.toPx()
                val amplitude = 4.dp.toPx()
                wavePath.moveTo(trackStart, centerY)
                var x = trackStart
                while (x <= thumbX) {
                    val phase = (x / waveLength) * (2f * kotlin.math.PI).toFloat()
                    wavePath.lineTo(
                        x,
                        centerY + kotlin.math.sin(phase) * amplitude,
                    )
                    x += 2.dp.toPx()
                }
                if (thumbX > trackStart) {
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

                if (thumbX < trackEnd) {
                    drawLine(
                        color = track,
                        start = androidx.compose.ui.geometry.Offset(thumbX, centerY),
                        end = androidx.compose.ui.geometry.Offset(trackEnd, centerY),
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
}

private data class AudioInfo(
    val bitrate: String,
    val format: String,
    val sampleRate: String,
)

private fun loadAudioInfo(context: android.content.Context, uriString: String): AudioInfo? {
    val uri = Uri.parse(uriString)
    return runCatching {
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                extractor.setDataSource(descriptor.fileDescriptor)
            } ?: return null
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull()?.div(1000)?.takeIf { it > 0 }
            var sampleRate = 0
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    break
                }
            }
            AudioInfo(
                bitrate = bitrate?.let { it.toString() + " kbps" } ?: "—",
                format = audioFormatLabel(mimeType),
                sampleRate = if (sampleRate > 0) {
                    val khz = sampleRate / 1000f
                    if (khz % 1f == 0f) {
                        khz.toInt().toString() + " kHz"
                    } else {
                        khz.toString() + " kHz"
                    }
                } else "—",
            )
        } finally {
            extractor.release()
            retriever.release()
        }
    }.getOrNull()
}

private fun audioFormatLabel(mimeType: String?): String = when (mimeType?.lowercase()) {
    "audio/mpeg" -> "MP3"
    "audio/mp4", "audio/x-m4a" -> "M4A"
    "audio/flac" -> "FLAC"
    "audio/ogg", "audio/vorbis" -> "OGG"
    "audio/wav", "audio/x-wav" -> "WAV"
    "audio/aac", "audio/aacp" -> "AAC"
    "audio/opus" -> "OPUS"
    else -> mimeType?.substringAfterLast('/')?.uppercase()?.takeIf { it.isNotBlank() } ?: "AUDIO"
}

@Composable
private fun AudioInfoPill(info: AudioInfo, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AudioInfoItem(Icons.Default.MusicNote, info.bitrate)
            AudioInfoDivider()
            AudioInfoItem(Icons.Default.GraphicEq, info.format)
            AudioInfoDivider()
            AudioInfoItem(Icons.Default.GraphicEq, info.sampleRate)
        }
    }
}

@Composable
private fun AudioInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun AudioInfoDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    )
}
