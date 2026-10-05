package com.sipun.sonora.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sipun.sonora.navigation.SonoraRoute

private data class BottomDestination(
    val route: SonoraRoute,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(SonoraRoute.Home, "Home", Icons.Default.Home),
    BottomDestination(SonoraRoute.Favorites, "Favorites", Icons.Default.Favorite),
    BottomDestination(SonoraRoute.Library, "Library", Icons.Default.LibraryMusic),
    BottomDestination(SonoraRoute.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun SonoraBottomBar(
    navController: NavHostController,
    destinations: List<SonoraRoute>,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        destinations.forEach { route ->
            val destination = bottomDestinations.first { it.route.route == route.route }

            NavigationBarItem(
                selected = currentRoute == route.route,
                onClick = {
                    navController.navigate(route.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(SonoraRoute.Home.route) {
                            saveState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = {
                    Text(destination.label)
                },
            )
        }
    }
}
