package com.sipun.sonora.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sipun.sonora.feature.favorites.FavoritesScreen
import com.sipun.sonora.feature.home.HomeScreen
import com.sipun.sonora.feature.library.LibraryScreen
import com.sipun.sonora.feature.nowplaying.NowPlayingScreen
import com.sipun.sonora.feature.settings.SettingsScreen
import com.sipun.sonora.ui.components.SonoraBottomBar

@Composable
fun SonoraApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            SonoraBottomBar(
                navController = navController,
                destinations = bottomNavigationRoutes,
            )
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = SonoraRoute.Home.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(SonoraRoute.Home.route) {
                HomeScreen(
                    onOpenNowPlaying = {
                        navController.navigate(SonoraRoute.NowPlaying.route)
                    },
                )
            }
            composable(SonoraRoute.Favorites.route) {
                FavoritesScreen()
            }
            composable(SonoraRoute.Library.route) {
                LibraryScreen()
            }
            composable(SonoraRoute.Settings.route) {
                SettingsScreen()
            }
            composable(SonoraRoute.NowPlaying.route) {
                NowPlayingScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
