package com.sipun.sonora.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.theme.SonoraRed
import com.sipun.sonora.ui.theme.SonoraSurface

@Composable
fun SonoraMiniPlayer(
    playerController: PlayerController,
    onOpenNowPlaying: () -> Unit,
) {
    val state by playerController.state.collectAsStateWithLifecycle()
    val song = state.currentSong ?: return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = SonoraSurface,
        tonalElevation = 0.dp,
        shadowElevation = 5.dp,
        onClick = onOpenNowPlaying,
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 7.dp, end = 6.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Album,
                        null,
                        tint = SonoraRed,
                        modifier = Modifier.size(28.dp),
                    )
                    song.albumArtUri?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Album artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = playerController::skipPrevious,
                    enabled = state.hasPrevious,
                ) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }
                IconButton(
                    onClick = playerController::togglePlayPause,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        tint = SonoraRed,
                    )
                }
                IconButton(
                    onClick = playerController::skipNext,
                    enabled = state.hasNext,
                ) {
                    Icon(Icons.Default.SkipNext, "Next")
                }
            }

            LinearProgressIndicator(
                progress = {
                    if (state.durationMs > 0) {
                        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = SonoraRed,
                trackColor = SonoraSurface,
            )
        }
    }
}
