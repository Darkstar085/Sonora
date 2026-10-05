package com.sipun.sonora.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sipun.sonora.feature.album.AlbumDetailScreen
import com.sipun.sonora.feature.home.HomeScreen
import com.sipun.sonora.feature.artist.ArtistDetailScreen
import com.sipun.sonora.feature.nowplaying.NowPlayingScreen
import com.sipun.sonora.feature.playlists.PlaylistDetailScreen
import com.sipun.sonora.feature.settings.SettingsScreen
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SonoraMiniPlayer

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
    val showMiniPlayer = miniPlayerVisible && playerState.currentSong != null

    LaunchedEffect(currentRoute) {
        if (currentRoute == SonoraRoute.NowPlaying.route) {
            miniPlayerVisible = false
        } else {
            delay(460)
            miniPlayerVisible = true
        }
    }

    DisposableEffect(player) { onDispose { player.release() } }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = SonoraRoute.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                if (targetState.destination.route == SonoraRoute.NowPlaying.route) {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 360,
                            easing = FastOutSlowInEasing,
                        ),
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
            predictivePopEnterTransition = {
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
            predictivePopExitTransition = {
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
        ) {
            composable(SonoraRoute.Home.route) {
                HomeScreen(
                    player,
                    { navController.navigate(SonoraRoute.NowPlaying.route) },
                    { navController.navigate(SonoraRoute.Album.createRoute(it)) },
                    { navController.navigate(SonoraRoute.Artist.createRoute(it)) },
                    { navController.navigate(SonoraRoute.Settings.route) },
                    { navController.navigate(SonoraRoute.Playlist.createRoute(it)) },
                )
            }
            composable(SonoraRoute.Settings.route) { SettingsScreen(onCheckForUpdates = onCheckForUpdates) }
            composable(
                SonoraRoute.Album.route,
                arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
            ) { entry ->
                AlbumDetailScreen(
                    albumName = Uri.decode(entry.arguments?.getString("albumId").orEmpty()),
                    playerController = player,
                    onBack = { navController.popBackStack() },
                    onOpenNowPlaying = { navController.navigate(SonoraRoute.NowPlaying.route) },
                    onOpenArtist = { navController.navigate(SonoraRoute.Artist.createRoute(it)) },
                )
            }
            composable(
                SonoraRoute.Artist.route,
                arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
            ) { entry ->
                ArtistDetailScreen(
                    artistName = Uri.decode(entry.arguments?.getString("artistId").orEmpty()),
                    playerController = player,
                    onBack = { navController.popBackStack() },
                    onOpenNowPlaying = { navController.navigate(SonoraRoute.NowPlaying.route) },
                    onOpenAlbum = { navController.navigate(SonoraRoute.Album.createRoute(it)) },
                )
            }
            composable(
                SonoraRoute.Playlist.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
            ) { entry ->
                PlaylistDetailScreen(
                    playlistId = entry.arguments?.getString("playlistId").orEmpty(),
                    playerController = player,
                    onBack = { navController.popBackStack() },
                    onOpenNowPlaying = { navController.navigate(SonoraRoute.NowPlaying.route) },
                    onOpenAlbum = { navController.navigate(SonoraRoute.Album.createRoute(it)) },
                    onOpenArtist = { navController.navigate(SonoraRoute.Artist.createRoute(it)) },
                )
            }
            composable(SonoraRoute.NowPlaying.route) {
                NowPlayingScreen(
                    playerController = player,
                    onBack = { navController.popBackStack() },
                    onOpenAlbum = { albumId -> navController.navigate(SonoraRoute.Album.createRoute(albumId)) },
                )
            }
        }

        if (showMiniPlayer) {
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
                        onOpenNowPlaying = {
                            navController.navigate(SonoraRoute.NowPlaying.route)
                        },
                    )
                }
            }
        }
    }
}