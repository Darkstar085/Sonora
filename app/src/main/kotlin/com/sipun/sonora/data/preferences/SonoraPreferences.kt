package com.sipun.sonora.data.preferences

import android.content.Context
import com.sipun.sonora.domain.model.Song

data class SonoraPlaylist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
)

class SonoraPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun favoriteIds(): Set<Long> =
        preferences.getStringSet(FAVORITE_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toLongOrNull)
            .toSet()

    fun saveLastPlayed(song: Song) {
        val history = listeningHistoryIds().toMutableList()
        history.remove(song.id)
        history.add(0, song.id)
        preferences.edit()
            .putLong(LAST_PLAYED_ID, song.id)
            .putString(LAST_PLAYED_TITLE, song.title)
            .putString(LAST_PLAYED_ARTIST, song.artist)
            .putString(LAST_PLAYED_ALBUM, song.album)
            .putString(LAST_PLAYED_URI, song.uri)
            .putString(LAST_PLAYED_ART, song.albumArtUri)
            .putString(LISTENING_HISTORY, history.take(MAX_LISTENING_HISTORY).joinToString(","))
            .apply()
    }

    fun listeningHistoryIds(): List<Long> =
        preferences.getString(LISTENING_HISTORY, null)
            ?.split(',')
            ?.mapNotNull(String::toLongOrNull)
            .orEmpty()


    fun lastPlayed(): Song? {
        val uri = preferences.getString(LAST_PLAYED_URI, null) ?: return null
        return Song(
            id = preferences.getLong(LAST_PLAYED_ID, 0L),
            title = preferences.getString(LAST_PLAYED_TITLE, null).orEmpty().ifBlank { "Unknown title" },
            artist = preferences.getString(LAST_PLAYED_ARTIST, null).orEmpty().ifBlank { "Unknown artist" },
            album = preferences.getString(LAST_PLAYED_ALBUM, null).orEmpty().ifBlank { "Unknown album" },
            durationMs = 0L,
            uri = uri,
            albumArtUri = preferences.getString(LAST_PLAYED_ART, null),
        )
    }

    fun clearLastPlayed() {
        preferences.edit()
            .remove(LAST_PLAYED_ID)
            .remove(LAST_PLAYED_TITLE)
            .remove(LAST_PLAYED_ARTIST)
            .remove(LAST_PLAYED_ALBUM)
            .remove(LAST_PLAYED_URI)
            .remove(LAST_PLAYED_ART)
            .apply()
    }

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

    fun playlists(): List<SonoraPlaylist> =
        preferences.getStringSet(PLAYLISTS, emptySet())
            .orEmpty()
            .mapNotNull { encoded ->
                val parts = encoded.split('|', limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val ids = parts[2].split(',').mapNotNull(String::toLongOrNull)
                SonoraPlaylist(parts[0], parts[1], ids)
            }
            .sortedBy { it.name.lowercase() }

    fun createPlaylist(name: String): SonoraPlaylist? {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return null
        val playlist = SonoraPlaylist(
            id = "playlist_" + System.currentTimeMillis(),
            name = cleanName,
            songIds = emptyList(),
        )
        savePlaylists(playlists() + playlist)
        return playlist
    }

    fun deletePlaylist(id: String) {
        savePlaylists(playlists().filterNot { it.id == id })
    }

    fun addToPlaylist(playlistId: String, songId: Long) {
        savePlaylists(
            playlists().map {
                if (it.id == playlistId && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it
            },
        )
    }

    fun removeFromPlaylist(playlistId: String, songId: Long) {
        savePlaylists(
            playlists().map {
                if (it.id == playlistId) it.copy(songIds = it.songIds.filterNot { id -> id == songId }) else it
            },
        )
    }

    private fun savePlaylists(playlists: List<SonoraPlaylist>) {
        preferences.edit()
            .putStringSet(
                PLAYLISTS,
                playlists.map { playlist ->
                    playlist.id + "|" + playlist.name.replace("|", " ") + "|" + playlist.songIds.joinToString(",")
                }.toSet(),
            )
            .apply()
    }

    companion object {
        private const val FILE_NAME = "sonora_preferences"
        private const val FAVORITE_IDS = "favorite_song_ids"
        private const val PLAYLISTS = "playlists"
        private const val LAST_PLAYED_ID = "last_played_id"
        private const val LAST_PLAYED_TITLE = "last_played_title"
        private const val LAST_PLAYED_ARTIST = "last_played_artist"
        private const val LAST_PLAYED_ALBUM = "last_played_album"
        private const val LAST_PLAYED_URI = "last_played_uri"
        private const val LAST_PLAYED_ART = "last_played_art"
        private const val LISTENING_HISTORY = "listening_history"
        private const val MAX_LISTENING_HISTORY = 10
    }
}
