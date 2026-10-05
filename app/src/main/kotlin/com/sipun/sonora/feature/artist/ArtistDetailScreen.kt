@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sipun.sonora.feature.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SongMoreButton
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun ArtistDetailScreen(
    artistName: String,
    playerController: PlayerController,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { AndroidMusicRepository(context.contentResolver) }
    val preferences = remember(context) { SonoraPreferences(context) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(artistName) {
        songs = repository.songs().filter { it.artist.equals(artistName, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SonoraRed)
            }
        } else {
            val first = songs.first()
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth(0.72f).aspectRatio(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Person, null, tint = SonoraRed, modifier = Modifier.size(72.dp))
                            first.albumArtUri?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = "Artist artwork",
                                    Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        Text(
                            artistName,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        Text(
                            if (songs.size == 1) "1 song" else songs.size.toString() + " songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        FilledIconButton(
                            onClick = {
                                playerController.playQueue(songs)
                                onOpenNowPlaying()
                            },
                            modifier = Modifier.padding(top = 12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = SonoraRed,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play artist")
                        }
                    }
                }
                item {
                    Text(
                        "Songs",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    Card(
                        onClick = {
                            playerController.playQueue(songs, index)
                            onOpenNowPlaying()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                (index + 1).toString().padStart(2, '0'),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(34.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.album, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            SongMoreButton(
                                song = song,
                                preferences = preferences,
                                playerController = playerController,
                                showArtist = false,
                                showPlayNext = true,
                                showAddToQueue = true,
                                onOpenAlbum = onOpenAlbum,
                            )
                        }
                    }
                }
            }
        }
    }
}
