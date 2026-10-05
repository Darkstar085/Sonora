package com.sipun.sonora.data.preferences

import android.content.Context

class SonoraPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun favoriteIds(): Set<Long> =
        preferences.getStringSet(FAVORITE_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toLongOrNull)
            .toSet()

    fun toggleFavorite(songId: Long): Boolean {
        val favorites = favoriteIds().toMutableSet()
        val isFavorite = if (favorites.add(songId)) true else {
            favorites.remove(songId)
            false
        }
        preferences.edit()
            .putStringSet(FAVORITE_IDS, favorites.map(Long::toString).toSet())
            .apply()
        return isFavorite
    }

    companion object {
        private const val FILE_NAME = "sonora_preferences"
        private const val FAVORITE_IDS = "favorite_song_ids"
    }
}
