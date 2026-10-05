package com.sipun.sonora.data.preferences

import android.content.Context
import com.sipun.sonora.domain.model.Song

internal class PlaybackPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

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

    private companion object {
        const val FILE_NAME = "sonora_preferences"
        const val LAST_PLAYED_ID = "last_played_id"
        const val LAST_PLAYED_TITLE = "last_played_title"
        const val LAST_PLAYED_ARTIST = "last_played_artist"
        const val LAST_PLAYED_ALBUM = "last_played_album"
        const val LAST_PLAYED_URI = "last_played_uri"
        const val LAST_PLAYED_ART = "last_played_art"
        const val LISTENING_HISTORY = "listening_history"
        const val MAX_LISTENING_HISTORY = 10
    }
}
