package com.sipun.sonora.feature.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun LibraryScreen(playerController: PlayerController, onOpenNowPlaying: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { AndroidMusicRepository(context.contentResolver) }
    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        loading = true
        songs = repository.songs()
        loading = false
    }

    if (!granted) return LibraryPermission { launcher.launch(permission) }
    if (loading) return BoxedLibraryState()
    if (songs.isEmpty()) return EmptyLibraryContent()

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            Text("Your library", style = MaterialTheme.typography.headlineSmall)
            Text(songs.size.toString() + " songs", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            Card(onClick = { playerController.playQueue(songs, index); onOpenNowPlaying() }, Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp)) {
                        Icon(Icons.Default.Album, null, tint = SonoraRed, modifier = Modifier.fillMaxSize().padding(12.dp))
                        song.albumArtUri?.let { AsyncImage(model = it, contentDescription = "Album artwork", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium)
                        Text(song.artist + " • " + song.album, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun LibraryPermission(onRequest: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.LibraryMusic, null, tint = MaterialTheme.colorScheme.primary)
        Text("Access your music", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
        Text("Sonora needs audio access to discover music stored on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        Button(onClick = onRequest) { Text("Allow access") }
    }
}
@Composable private fun BoxedLibraryState() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Text("Scanning your music…", modifier = Modifier.padding(top = 12.dp))
    }
}
@Composable private fun EmptyLibraryContent() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AudioFile, null, tint = MaterialTheme.colorScheme.primary)
        Text("No music found", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
        Text("Add supported audio files to this device and open Sonora again.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}
