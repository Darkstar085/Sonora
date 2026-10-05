package com.sipun.sonora.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.ByteArrayOutputStream

internal class ArtworkLoader(private val context: Context) {
    companion object {
        private const val MAX_ARTWORK_SIZE = 512
        private const val MAX_ARTWORK_BYTES = 2 * 1024 * 1024
    }

    fun extractEmbeddedArtwork(uri: Uri?): ByteArray? {
        if (uri == null) return null

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val embedded = retriever.embeddedPicture ?: return null
            if (embedded.size <= MAX_ARTWORK_BYTES) {
                embedded
            } else {
                resizeArtwork(embedded)
            }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
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
