package com.sipun.sonora.data.lyrics

data class Lyrics(val lines: List<LyricsLine>, val synced: Boolean)
data class LyricsLine(val text: String, val startMs: Long? = null)
