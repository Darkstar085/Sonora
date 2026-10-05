package com.sipun.sonora.domain.repository

import com.sipun.sonora.domain.model.Song

interface MusicRepository {
    suspend fun songs(): List<Song>
    suspend fun albums(): List<String>
    suspend fun artists(): List<String>
}
