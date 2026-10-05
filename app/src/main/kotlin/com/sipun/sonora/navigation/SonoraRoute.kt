package com.sipun.sonora.navigation

import android.net.Uri

sealed interface SonoraRoute {
    val route: String
    data object Home : SonoraRoute { override val route = "home" }
    data object Favorites : SonoraRoute { override val route = "favorites" }
    data object Library : SonoraRoute { override val route = "library" }
    data object Settings : SonoraRoute { override val route = "settings" }
    data object NowPlaying : SonoraRoute { override val route = "now_playing" }
    data object Album : SonoraRoute { override val route = "album/{albumId}"; fun createRoute(albumId: String) = "album/" + Uri.encode(albumId) }
    data object Artist : SonoraRoute { override val route = "artist/{artistId}"; fun createRoute(artistId: String) = "artist/" + Uri.encode(artistId) }
    data object Playlist : SonoraRoute { override val route = "playlist/{playlistId}"; fun createRoute(playlistId: String) = "playlist/" + Uri.encode(playlistId) }
}
val bottomNavigationRoutes = listOf(SonoraRoute.Home, SonoraRoute.Favorites, SonoraRoute.Library, SonoraRoute.Settings)
