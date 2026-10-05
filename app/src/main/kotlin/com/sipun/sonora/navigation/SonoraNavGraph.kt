package com.sipun.sonora.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sipun.sonora.feature.album.AlbumDetailScreen
import com.sipun.sonora.feature.artist.ArtistDetailScreen
import com.sipun.sonora.feature.home.HomeScreen
import com.sipun.sonora.feature.lyrics.LyricsScreen
import com.sipun.sonora.feature.nowplaying.NowPlayingScreen
import com.sipun.sonora.feature.playlists.PlaylistDetailScreen
import com.sipun.sonora.feature.settings.SettingsScreen
import com.sipun.sonora.player.PlayerController

@Composable
internal fun SonoraNavGraph(
    navController: NavHostController,
    player: PlayerController,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier,
    enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
    popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
    predictivePopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition,
    predictivePopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition,
) {
    NavHost(
        navController = navController,
        startDestination = SonoraRoute.Home.route,
        modifier = modifier,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        predictivePopEnterTransition = predictivePopEnterTransition,
        predictivePopExitTransition = predictivePopExitTransition,
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
        composable(SonoraRoute.Settings.route) {
            SettingsScreen(onCheckForUpdates = onCheckForUpdates)
        }
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
                onOpenAlbum = { albumId ->
                    navController.navigate(SonoraRoute.Album.createRoute(albumId))
                },
                onOpenLyrics = { navController.navigate(SonoraRoute.Lyrics.route) },
            )
        }
        composable(SonoraRoute.Lyrics.route) {
            LyricsScreen(playerController = player, onBack = { navController.popBackStack() })
        }
    }
}
