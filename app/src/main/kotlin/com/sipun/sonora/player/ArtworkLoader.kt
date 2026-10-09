package com.sipun.sonora.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.io.files.Path
import org.jaudiotagger.kt.AudioTagger
import java.io.ByteArrayOutputStream
import java.io.File

internal class ArtworkLoader(private val context: Context) {
    companion object {
        private const val MAX_ARTWORK_SIZE = 512
        private const val MAX_ARTWORK_BYTES = 2 * 1024 * 1024
    }

    fun extractEmbeddedArtwork(uri: Uri?): ByteArray? {
        if (uri == null) return null

        val retriever = MediaMetadataRetriever()
        val embedded = try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }

        val processed = embedded?.let {
            if (it.size <= MAX_ARTWORK_BYTES) it else resizeArtwork(it)
        }
        return processed ?: runCatching { extractWithJaudiotagger(uri) }.getOrNull()
    }

    fun extractArtwork(audioUri: Uri?, artworkUri: Uri?): ByteArray? =
        extractEmbeddedArtwork(audioUri) ?: extractArtworkFromUri(artworkUri)

    private fun extractArtworkFromUri(uri: Uri?): ByteArray? {
        if (uri == null) return null
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            if (bytes.size <= MAX_ARTWORK_BYTES) bytes else resizeArtwork(bytes)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractWithJaudiotagger(uri: Uri): ByteArray? {
        val suffix = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                        .substringAfterLast('.', "")
                        .takeIf { it.isNotBlank() }
                        ?.let { ".$it" }
                } else {
                    null
                }
            }
        }.getOrNull() ?: ".audio"

        val temp = File.createTempFile("sonora-artwork-", suffix, context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            AudioTagger.read(Path(temp.absolutePath)).tag.artworks
                .firstOrNull()?.data
                ?.let { if (it.size <= MAX_ARTWORK_BYTES) it else resizeArtwork(it) }
        } catch (_: Exception) {
            null
        } finally {
            temp.delete()
        }
    }

    private fun resizeArtwork(embedded: ByteArray): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
            ?: return null
        return try {
            val maxDimension = maxOf(bitmap.width, bitmap.height)
            val scaled = if (maxDimension > MAX_ARTWORK_SIZE) {
                val scale = MAX_ARTWORK_SIZE.toFloat() / maxDimension
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                bitmap
            }
            ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, output)
                output.toByteArray()
            }.also {
                if (scaled !== bitmap) scaled.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
