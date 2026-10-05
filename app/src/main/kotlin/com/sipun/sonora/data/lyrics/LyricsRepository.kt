package com.sipun.sonora.data.lyrics

import com.sipun.sonora.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class LyricsRepository {
    private val client = LrcLibClient()
    suspend fun get(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        client.fetch(song.title, song.artist, song.album)
    }
}
