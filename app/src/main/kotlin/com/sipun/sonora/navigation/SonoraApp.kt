package com.sipun.sonora.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sipun.sonora.player.PlayerController
import kotlinx.coroutines.delay

@Composable
fun SonoraApp(onCheckForUpdates: () -> Unit = {}) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val player = remember(context) { PlayerController(context.applicationContext) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val slideOffset = with(LocalDensity.current) { 72.dp.roundToPx() }
    var miniPlayerVisible by remember { mutableStateOf(currentRoute != SonoraRoute.NowPlaying.route) }
    val playerState by player.state.collectAsStateWithLifecycle()
    val showMiniPlayer = miniPlayerVisible &&
        currentRoute != SonoraRoute.Lyrics.route &&
        playerState.currentSong != null

    LaunchedEffect(currentRoute) {
        if (currentRoute == SonoraRoute.NowPlaying.route) {
            miniPlayerVisible = false
        } else {
            delay(200)
            miniPlayerVisible = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SonoraNavGraph(
            navController = navController,
            player = player,
            onCheckForUpdates = onCheckForUpdates,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                if (targetState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeIn(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                    ) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 360,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideInVertically(
                        initialOffsetY = { slideOffset },
                        animationSpec = tween(
                            durationMillis = 360,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    EnterTransition.None
                }
            },
            exitTransition = {
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeOut(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideOutVertically(
                        targetOffsetY = { slideOffset },
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    ExitTransition.None
                }
            },
            popEnterTransition = {
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    EnterTransition.None
                }
            },
            popExitTransition = {
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideOutVertically(
                        targetOffsetY = { slideOffset },
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    ExitTransition.None
                }
            },
            predictivePopEnterTransition = { _ ->
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    EnterTransition.None
                }
            },
            predictivePopExitTransition = { _ ->
                if (initialState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideOutVertically(
                        targetOffsetY = { slideOffset },
                        animationSpec = tween(
                            durationMillis = 460,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    ExitTransition.None
                }
            },
        )

        SonoraMiniPlayerOverlay(
            player = player,
            playerState = playerState,
            visible = showMiniPlayer,
            onOpenNowPlaying = {
                navController.navigate(SonoraRoute.NowPlaying.route)
            },
        )
    }
}
