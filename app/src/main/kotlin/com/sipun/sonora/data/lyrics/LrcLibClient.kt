package com.sipun.sonora.data.lyrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal class LrcLibClient {
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun fetch(title: String, artist: String, album: String): Lyrics? {
        val url = "https://lrclib.net/api/get?track_name=" + enc(title) +
                "&artist_name=" + enc(artist) + "&album_name=" + enc(album)
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 15000
        return try {
            if (c.responseCode != 200) return null
            val r = json.decodeFromString<Response>(
                c.inputStream.bufferedReader().use { it.readText() })
            if (r.instrumental) null
            else r.syncedLyrics?.takeIf(String::isNotBlank)?.let(LyricsParser::parse)
                ?: r.plainLyrics?.takeIf(String::isNotBlank)?.let {
                    Lyrics(it.lines().filter(String::isNotBlank).map(::LyricsLine), false)
                }
        } finally {
            c.disconnect()
        }
    }

    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")
}

@Serializable
private data class Response(
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val instrumental: Boolean = false
)
