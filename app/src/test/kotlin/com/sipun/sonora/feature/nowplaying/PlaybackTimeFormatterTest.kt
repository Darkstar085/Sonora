package com.sipun.sonora.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTimeFormatterTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatPlaybackTime(0))
        assertEquals("1:05", formatPlaybackTime(65_000))
        assertEquals("62:03", formatPlaybackTime(3_723_000))
    }
}
