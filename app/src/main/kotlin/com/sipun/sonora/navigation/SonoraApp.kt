package com.sipun.sonora.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.*
import com.sipun.sonora.feature.favorites.FavoritesScreen
import com.sipun.sonora.feature.home.HomeScreen
import com.sipun.sonora.feature.library.LibraryScreen
import com.sipun.sonora.feature.nowplaying.NowPlayingScreen
import com.sipun.sonora.feature.settings.SettingsScreen
import com.sipun.sonora.player.PlayerController
import com.sipun.sonora.ui.components.SonoraBottomBar

@Composable
fun SonoraApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val player = remember(context) { PlayerController(context.applicationContext) }

    DisposableEffect(player) { onDispose { player.release() } }

    Scaffold(bottomBar = { SonoraBottomBar(navController, bottomNavigationRoutes) }) { padding ->
        NavHost(navController, SonoraRoute.Home.route, Modifier.padding(padding)) {
            composable(SonoraRoute.Home.route) { HomeScreen(player) { navController.navigate(SonoraRoute.NowPlaying.route) } }
            composable(SonoraRoute.Favorites.route) { FavoritesScreen() }
            composable(SonoraRoute.Library.route) { LibraryScreen(player) { navController.navigate(SonoraRoute.NowPlaying.route) } }
            composable(SonoraRoute.Settings.route) { SettingsScreen() }
            composable(SonoraRoute.NowPlaying.route) { NowPlayingScreen(player) { navController.popBackStack() } }
        }
    }
}
