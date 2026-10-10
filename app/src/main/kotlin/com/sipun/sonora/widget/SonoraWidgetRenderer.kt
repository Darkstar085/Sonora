package com.sipun.sonora.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.widget.RemoteViews
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sipun.sonora.MainActivity
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.AppTheme
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.data.preferences.ThemePreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.ArtworkLoader
import com.sipun.sonora.player.SonoraPlaybackService
import kotlin.math.max

object SonoraWidgetRenderer {
    private const val REQUEST_OPEN_APP = 901
    private const val REQUEST_TOGGLE = 902
    private const val REQUEST_PREVIOUS = 903
    private const val REQUEST_NEXT = 904
    private const val REQUEST_SHUFFLE = 905
    private const val REQUEST_REPEAT = 906
    private const val PROGRESS_BITMAP_WIDTH = 256
    private const val PROGRESS_BITMAP_HEIGHT = 20
    private val artworkCache = mutableMapOf<String, Bitmap>()
    private val artworkMissing = mutableSetOf<String>()
    private val mediaStoreArtworkUriCache = mutableMapOf<String, String?>()

    fun update(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        layoutResId: Int,
        fullPlayer: Boolean,
    ) {
        if (ids.isEmpty()) return
        val appContext = context.applicationContext
        val future = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, SonoraPlaybackService::class.java)),
        ).buildAsync()
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull()
                try {
                    render(appContext, manager, ids, layoutResId, fullPlayer, controller)
                } finally {
                    MediaController.releaseFuture(future)
                }
            },
            appContext.mainExecutor,
        )
    }

    fun refreshFromPlayer(context: Context, player: Player?) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val widgets = listOf(
            WidgetSpec(
                SonoraMiniPlayerWidgetProvider::class.java,
                R.layout.widget_mini_player,
                false,
            ),
            WidgetSpec(
                SonoraFullPlayerWidgetProvider::class.java,
                R.layout.widget_full_player,
                true,
            ),
        )
        widgets.forEach { spec ->
            val ids = manager.getAppWidgetIds(ComponentName(appContext, spec.providerClass))
            if (ids.isNotEmpty()) {
                render(appContext, manager, ids, spec.layoutResId, spec.fullPlayer, player)
            }
        }
    }

    private fun render(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        layoutResId: Int,
        fullPlayer: Boolean,
        player: Player?,
    ) {
        val preferences = SonoraPreferences(context)
        val themePreferences = ThemePreferences.from(context)
        val accent = themePreferences.resolvedWidgetAccent()
        val darkTheme = isDarkTheme(context, themePreferences)
        val textColor = if (darkTheme) 0xFFF5F5F5.toInt() else 0xFF202124.toInt()
        val secondaryTextColor = if (darkTheme) 0xFFBDBDBD.toInt() else 0xFF62656F.toInt()
        val item = player?.currentMediaItem
        val savedSong = preferences.lastPlayed()
        val song = item?.let { it.toSong(savedSong) } ?: savedSong
        val isPlaying = player?.let {
            it.playWhenReady && it.currentMediaItem != null && it.playbackState != Player.STATE_ENDED
        } == true
        val duration = player?.duration
            ?.takeIf { item != null && it != C.TIME_UNSET }
            ?.coerceAtLeast(0L) ?: 0L
        val position = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
        val repeatMode = player?.repeatMode ?: Player.REPEAT_MODE_OFF
        val shuffleEnabled = player?.shuffleModeEnabled == true

        val views = RemoteViews(context.packageName, layoutResId)
        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (darkTheme) {
                R.drawable.widget_glass_dark
            } else {
                R.drawable.widget_glass_light
            },
        )
        views.setTextViewText(
            R.id.widget_title,
            song?.title?.takeIf(String::isNotBlank) ?: context.getString(R.string.widget_nothing_playing),
        )
        views.setTextViewText(
            R.id.widget_artist,
            song?.artist?.takeIf(String::isNotBlank)
                ?: context.getString(R.string.widget_open_sonora_to_play),
        )
        views.setTextColor(R.id.widget_title, textColor)
        views.setTextColor(R.id.widget_artist, secondaryTextColor)
        if (fullPlayer) {
            views.setTextViewText(
                R.id.widget_status,
                when {
                    song == null -> context.getString(R.string.widget_nothing_playing)
                    isPlaying -> context.getString(R.string.widget_now_playing)
                    else -> context.getString(R.string.widget_paused)
                },
            )
            views.setTextColor(R.id.widget_status, secondaryTextColor)
        }
        if (fullPlayer) {
            views.setTextViewText(R.id.widget_position, formatTime(position))
            views.setTextViewText(R.id.widget_duration, formatTime(duration))
            views.setTextColor(R.id.widget_position, secondaryTextColor)
            views.setTextColor(R.id.widget_duration, secondaryTextColor)
            views.setImageViewBitmap(
                R.id.widget_progress,
                progressBitmap(
                    if (duration > 0L) position.toFloat() / duration.toFloat() else 0f,
                    accent,
                    if (darkTheme) 0x55FFFFFF else 0x44000000,
                    showThumb = true,
                ),
            )
            views.setImageViewResource(
                R.id.widget_repeat_button,
                if (repeatMode == Player.REPEAT_MODE_ONE) {
                    R.drawable.widget_repeat_one
                } else {
                    R.drawable.widget_repeat
                },
            )
            val inactiveControlBackground = if (darkTheme) {
                R.drawable.widget_control_background
            } else {
                R.drawable.widget_control_background_light
            }
            val repeatEnabled = repeatMode != Player.REPEAT_MODE_OFF
            views.setInt(
                R.id.widget_shuffle_button,
                "setColorFilter",
                if (shuffleEnabled) accent else textColor,
            )
            views.setInt(
                R.id.widget_repeat_button,
                "setColorFilter",
                if (repeatEnabled) accent else textColor,
            )
            views.setInt(R.id.widget_repeat_background, "setBackgroundResource", inactiveControlBackground)
            views.setInt(R.id.widget_shuffle_background, "setBackgroundResource", inactiveControlBackground)
            if (repeatEnabled) {
                views.setImageViewBitmap(
                    R.id.widget_repeat_background,
                    activeControlBackgroundBitmap(accent, if (darkTheme) 0x33 else 0x22),
                )
            }
            if (shuffleEnabled) {
                views.setImageViewBitmap(
                    R.id.widget_shuffle_background,
                    activeControlBackgroundBitmap(accent, if (darkTheme) 0x33 else 0x22),
                )
            }
            views.setInt(R.id.widget_previous_button, "setBackgroundResource", inactiveControlBackground)
            views.setInt(R.id.widget_next_button, "setBackgroundResource", inactiveControlBackground)
            views.setInt(R.id.widget_previous_button, "setColorFilter", textColor)
            views.setInt(R.id.widget_next_button, "setColorFilter", textColor)
            views.setOnClickPendingIntent(
                R.id.widget_previous_button,
                actionIntent(context, SonoraWidgetActions.ACTION_PREVIOUS, REQUEST_PREVIOUS),
            )
            views.setOnClickPendingIntent(
                R.id.widget_next_button,
                actionIntent(context, SonoraWidgetActions.ACTION_NEXT, REQUEST_NEXT),
            )
            views.setOnClickPendingIntent(
                R.id.widget_shuffle_button,
                actionIntent(context, SonoraWidgetActions.ACTION_SHUFFLE, REQUEST_SHUFFLE),
            )
            views.setOnClickPendingIntent(
                R.id.widget_repeat_button,
                actionIntent(context, SonoraWidgetActions.ACTION_REPEAT, REQUEST_REPEAT),
            )
        } else {
            views.setImageViewBitmap(
                R.id.widget_progress,
                progressBitmap(
                    if (duration > 0L) position.toFloat() / duration.toFloat() else 0f,
                    accent,
                    if (darkTheme) 0x55FFFFFF else 0x44000000,
                ),
            )
        }

        val artwork = song?.let {
            loadArtwork(context, item, it.albumArtUri, it.uri, savedSong?.albumArtUri)
        }
        if (artwork != null) {
            views.setImageViewBitmap(R.id.widget_artwork, artwork)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.widget_music_note)
            views.setInt(R.id.widget_artwork, "setColorFilter", accent)
        }
        views.setInt(R.id.widget_play_circle, "setColorFilter", accent)
        views.setViewVisibility(
            R.id.widget_play_icon,
            if (isPlaying) android.view.View.GONE else android.view.View.VISIBLE,
        )
        views.setViewVisibility(
            R.id.widget_pause_icon,
            if (isPlaying) android.view.View.VISIBLE else android.view.View.GONE,
        )
        views.setOnClickPendingIntent(
            R.id.widget_play_circle,
            actionIntent(context, SonoraWidgetActions.ACTION_TOGGLE_PLAYBACK, REQUEST_TOGGLE),
        )
        views.setOnClickPendingIntent(
            R.id.widget_play_icon,
            actionIntent(context, SonoraWidgetActions.ACTION_TOGGLE_PLAYBACK, REQUEST_TOGGLE),
        )
        views.setOnClickPendingIntent(
            R.id.widget_pause_icon,
            actionIntent(context, SonoraWidgetActions.ACTION_TOGGLE_PLAYBACK, REQUEST_TOGGLE),
        )
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                REQUEST_OPEN_APP,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun isDarkTheme(context: Context, preferences: ThemePreferences): Boolean {
        if (preferences.pureBlack.value) return true
        return when (preferences.theme.value) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.SYSTEM -> {
                val mask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                mask == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, SonoraWidgetActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun progressBitmap(
        progress: Float,
        accent: Int,
        trackColor: Int,
        showThumb: Boolean = false,
    ): Bitmap {
        val height = if (showThumb) PROGRESS_BITMAP_HEIGHT else 4
        val bitmap = Bitmap.createBitmap(
            PROGRESS_BITMAP_WIDTH,
            height,
            Bitmap.Config.ARGB_8888,
        )
        val cutoff = (progress.coerceIn(0f, 1f) * (PROGRESS_BITMAP_WIDTH - 1)).toInt()
        val trackTop = (height / 2 - 1).coerceAtLeast(0)
        val trackBottom = (height / 2).coerceAtMost(height - 1)
        for (x in 0 until PROGRESS_BITMAP_WIDTH) {
            val inThumb = showThumb &&
                x in (cutoff - 1).coerceAtLeast(0)..(cutoff + 1).coerceAtMost(PROGRESS_BITMAP_WIDTH - 1)
            for (y in 0 until height) {
                val isTrack = y in trackTop..trackBottom
                val color = when {
                    inThumb -> accent
                    isTrack && x < cutoff -> accent
                    isTrack -> trackColor
                    else -> android.graphics.Color.TRANSPARENT
                }
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    private fun formatTime(positionMs: Long): String {
        val totalSeconds = (positionMs / 1_000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }

    private fun loadArtwork(
        context: Context,
        item: MediaItem?,
        artworkUri: String?,
        audioUri: String?,
        fallbackArtworkUri: String?,
    ): Bitmap? {
        val artworkData = item?.mediaMetadata?.artworkData
        val metadataArtwork = artworkData?.let(::decodeArtwork)
        val audioUriText = audioUri?.takeIf(String::isNotBlank)
        val uriCandidates = linkedSetOf<String>().apply {
            listOf(
                item?.mediaMetadata?.artworkUri?.toString(),
                artworkUri,
                fallbackArtworkUri,
                mediaStoreAlbumArtworkUri(context, item),
            ).forEach { candidate ->
                candidate?.takeIf(String::isNotBlank)?.let(::add)
            }
        }
        val cacheKey = listOfNotNull(
            item?.mediaId?.takeIf(String::isNotBlank),
            audioUriText,
            uriCandidates.joinToString("|"),
        ).joinToString("|").takeIf(String::isNotBlank) ?: return metadataArtwork

        artworkCache[cacheKey]?.let { return it }
        if (metadataArtwork != null) {
            cacheArtwork(cacheKey, metadataArtwork)
            return metadataArtwork
        }
        if (cacheKey in artworkMissing && artworkData == null) return null

        val loader = ArtworkLoader(context)
        val embedded = runCatching {
            loader.extractEmbeddedArtwork(audioUriText?.let(Uri::parse))
        }.getOrNull()?.let(::decodeArtwork)

        val decoded = embedded ?: uriCandidates.firstNotNullOfOrNull { candidate ->
            runCatching {
                loader.extractArtwork(null, Uri.parse(candidate))?.let(::decodeArtwork)
            }.getOrNull()
        }

        if (decoded != null) {
            cacheArtwork(cacheKey, decoded)
        } else {
            if (artworkMissing.size >= 16) artworkMissing.clear()
            artworkMissing.add(cacheKey)
        }
        return decoded
    }


    private fun activeControlBackgroundBitmap(accent: Int, alpha: Int): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accent and 0x00FFFFFF) or (alpha shl 24)
        }
        Canvas(bitmap).drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return bitmap
    }

    private fun cacheArtwork(key: String, bitmap: Bitmap) {
        if (artworkCache.size >= 12) artworkCache.clear()
        artworkMissing.remove(key)
        artworkCache[key] = bitmap
    }

    private fun mediaStoreAlbumArtworkUri(context: Context, item: MediaItem?): String? {
        val mediaId = item?.mediaId?.takeIf(String::isNotBlank) ?: return null
        if (mediaStoreArtworkUriCache.containsKey(mediaId)) {
            return mediaStoreArtworkUriCache[mediaId]
        }

        val rowId = mediaId.toLongOrNull()
        val artworkUri = if (rowId == null) {
            null
        } else {
            runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                    "${MediaStore.Audio.Media._ID}=?",
                    arrayOf(rowId.toString()),
                    null,
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) {
                        null
                    } else {
                        val albumId = cursor.getLong(0)
                        if (albumId <= 0L) {
                            null
                        } else {
                            Uri.parse("content://media/external/audio/albumart")
                                .buildUpon()
                                .appendPath(albumId.toString())
                                .build()
                                .toString()
                        }
                    }
                }
            }.getOrNull()
        }

        if (mediaStoreArtworkUriCache.size >= 64) mediaStoreArtworkUriCache.clear()
        mediaStoreArtworkUriCache[mediaId] = artworkUri
        return artworkUri
    }

    private fun decodeArtwork(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sampleSize = (max(bounds.outWidth, bounds.outHeight) / 256).coerceAtLeast(1)
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }

    private fun MediaItem.toSong(savedSong: Song?): Song {
        val metadata = mediaMetadata
        return Song(
            id = mediaId.toLongOrNull() ?: savedSong?.id ?: 0L,
            title = metadata.title?.toString().orEmpty().ifBlank { savedSong?.title ?: "Unknown title" },
            artist = metadata.artist?.toString().orEmpty().ifBlank { savedSong?.artist ?: "Unknown artist" },
            album = metadata.albumTitle?.toString().orEmpty().ifBlank { savedSong?.album ?: "Unknown album" },
            durationMs = 0L,
            uri = localConfiguration?.uri?.toString() ?: savedSong?.uri.orEmpty(),
            albumArtUri = metadata.artworkUri?.toString() ?: savedSong?.albumArtUri,
        )
    }

    private data class WidgetSpec(
        val providerClass: Class<*>,
        val layoutResId: Int,
        val fullPlayer: Boolean,
    )
}