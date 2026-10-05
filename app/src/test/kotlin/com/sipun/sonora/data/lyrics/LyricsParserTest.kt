package com.sipun.sonora.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsParserTest {
    @Test
    fun parsesSyncedLyrics() {
        val lyrics = LyricsParser.parse(
            "[00:01.20]First line\n[00:03.450]Second line\n[01:02.5]Third line"
        )
        assertEquals(1_200L, lyrics.lines[0].startMs)
        assertEquals("First line", lyrics.lines[0].text)
        assertEquals(3_450L, lyrics.lines[1].startMs)
        assertEquals(62_500L, lyrics.lines[2].startMs)
        assertEquals(true, lyrics.synced)
    }

    @Test
    fun parsesPlainLyrics() {
        val lyrics = LyricsParser.parse("First line\n\nSecond line")
        assertEquals(listOf("First line", "Second line"), lyrics.lines.map(LyricsLine::text))
        assertEquals(false, lyrics.synced)
    }
}
