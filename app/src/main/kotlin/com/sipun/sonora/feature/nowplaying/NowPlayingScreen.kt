@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.nowplaying

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.player.RepeatMode
import com.sipun.sonora.ui.theme.SonoraRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun NowPlayingScreen(playerController: PlayerController, onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { SonoraPreferences(context) }
    val state by playerController.state.collectAsStateWithLifecycle()
    var favorite by remember(state.currentSong?.id) {
        mutableStateOf(state.currentSong?.id?.let { it in preferences.favoriteIds() } == true)
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            playerController.refresh()
            delay(500)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Now Playing") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = {
                IconButton(
                    onClick = { state.currentSong?.let { favorite = preferences.toggleFavorite(it.id) } },
                    enabled = state.currentSong != null,
                ) {
                    Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite")
                }
            },
        )
    }) { padding ->
        val max = state.durationMs.coerceAtLeast(1L)
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.weight(.18f))
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Album, null, tint = SonoraRed, modifier = Modifier.size(72.dp))
                state.currentSong?.albumArtUri?.let {
                    AsyncImage(model = it, contentDescription = "Album artwork", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Text(state.currentSong?.title ?: "Select a song from your library", style = MaterialTheme.typography.headlineSmall)
            Text(state.currentSong?.artist ?: "Sonora", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = state.positionMs.coerceIn(0L, max).toFloat(),
                onValueChange = { playerController.seekTo(it.toLong()) },
                valueRange = 0f..max.toFloat(),
                enabled = state.currentSong != null && state.durationMs > 0,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatPlaybackTime(state.positionMs), style = MaterialTheme.typography.labelMedium)
                Text(formatPlaybackTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = playerController::cycleRepeat) {
                    Icon(Icons.Default.Repeat, "Repeat " + state.repeatMode.name.lowercase(), tint = if (state.repeatMode != RepeatMode.OFF) SonoraRed else LocalContentColor.current)
                }
                IconButton(onClick = playerController::skipPrevious) { Icon(Icons.Default.SkipPrevious, "Previous") }
                FilledIconButton(
                    onClick = playerController::togglePlayPause,
                    enabled = state.currentSong != null,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = SonoraRed, contentColor = Color.White),
                ) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play")
                }
                IconButton(onClick = playerController::skipNext) { Icon(Icons.Default.SkipNext, "Next") }
                IconButton(onClick = playerController::toggleShuffle) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) SonoraRed else LocalContentColor.current)
                }
            }
            Spacer(Modifier.weight(.1f))
        }
    }
}
