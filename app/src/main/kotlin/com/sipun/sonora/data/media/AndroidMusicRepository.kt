package com.sipun.sonora.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidMusicRepository(private val contentResolver: ContentResolver) : MusicRepository {
    override suspend fun songs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"

        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder)?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val track = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val year = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val relativePath = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val displayName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val songId = cursor.getLong(id)
                val currentAlbumId = cursor.getLong(albumId)
                songs += Song(
                    id = songId,
                    title = cursor.getString(title).orEmpty().ifBlank { cursor.getString(displayName).orEmpty().ifBlank { "Unknown title" } },
                    artist = cursor.getString(artist).orEmpty().ifBlank { "Unknown artist" },
                    album = cursor.getString(album).orEmpty().ifBlank { "Unknown album" },
                    durationMs = cursor.getLong(duration),
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId).toString(),
                    albumArtUri = if (currentAlbumId > 0) ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, currentAlbumId).toString() else null,
                    trackNumber = cursor.getInt(track).takeIf { it > 0 },
                    year = cursor.getInt(year).takeIf { it > 0 },
                    folder = cursor.getString(relativePath).orEmpty().ifBlank { null },
                )
            }
        }
        songs
    }

    override suspend fun albums(): List<String> = songs().map(Song::album).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    override suspend fun artists(): List<String> = songs().map(Song::artist).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
}
