package com.sipun.sonora.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.player.PlayerState
import com.sipun.sonora.ui.components.SonoraMiniPlayer

@Composable
internal fun SonoraMiniPlayerOverlay(
    player: PlayerController,
    playerState: PlayerState,
    visible: Boolean,
    onOpenNowPlaying: () -> Unit,
) {
    if (!visible || playerState.currentSong == null) return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SonoraMiniPlayer(
                playerController = player,
                onOpenNowPlaying = onOpenNowPlaying,
            )
        }
    }
}
