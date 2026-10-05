package com.sipun.sonora.feature.nowplaying

fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
