package com.sipun.sonora.data.lyrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
private data class CachedLyrics(val lines: List<CachedLine>, val synced: Boolean)

@Serializable
private data class CachedLine(val text: String, val startMs: Long? = null)

internal class LyricsCache(private val directory: File) {
    private val json = Json { ignoreUnknownKeys = true }

    fun get(key: String): Lyrics? = runCatching {
        val file = File(directory, sha256(key) + ".json")
        if (!file.isFile) return null
        json.decodeFromString<CachedLyrics>(file.readText()).let {
            Lyrics(it.lines.map { line -> LyricsLine(line.text, line.startMs) }, it.synced)
        }
    }.getOrNull()

    fun put(key: String, lyrics: Lyrics) {
        runCatching {
            directory.mkdirs()
            val file = File(directory, sha256(key) + ".json")
            file.writeText(
                json.encodeToString(
                    CachedLyrics(
                        lyrics.lines.map { CachedLine(it.text, it.startMs) }, lyrics.synced
                    )
                )
            )
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
