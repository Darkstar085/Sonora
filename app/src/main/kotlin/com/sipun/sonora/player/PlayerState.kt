package com.sipun.sonora.player

import com.sipun.sonora.domain.model.Song

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}
