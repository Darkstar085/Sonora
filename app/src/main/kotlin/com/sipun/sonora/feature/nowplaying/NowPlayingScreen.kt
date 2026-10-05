package com.sipun.sonora.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sipun.sonora.ui.theme.SonoraRed

@Composable
fun NowPlayingScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.weight(0.6f))
            Column(
                modifier = Modifier.weight(2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No song playing",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("Select a song from your library", style = MaterialTheme.typography.headlineSmall)
                Text("Sonora", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = 0f,
                    onValueChange = {},
                    enabled = false,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        disabledActiveTrackColor = SonoraRed,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {}) { Icon(Icons.Default.Repeat, contentDescription = "Repeat") }
                    IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous") }
                    FilledIconButton(
                        onClick = {},
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = SonoraRed,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Play")
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, contentDescription = "Next") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, contentDescription = "Shuffle") }
                }
            }
            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}
