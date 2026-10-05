package com.sipun.sonora.ui.components

data class SongActionConfig(
    val showPlaylist: Boolean = true,
    val showFavorite: Boolean = true,
    val showAlbum: Boolean = true,
    val showArtist: Boolean = true,
    val showRemoveFromPlaylist: Boolean = false,
    val showPlayNext: Boolean = false,
    val showAddToQueue: Boolean = false,
    val showRemoveFromDevice: Boolean = false,
    val showInfo: Boolean = true,
    val showEditMetadata: Boolean = false,
)
