package com.sipun.sonora.widget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.player.SonoraPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SonoraWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in setOf(
                SonoraWidgetActions.ACTION_TOGGLE_PLAYBACK,
                SonoraWidgetActions.ACTION_PREVIOUS,
                SonoraWidgetActions.ACTION_NEXT,
                SonoraWidgetActions.ACTION_SHUFFLE,
                SonoraWidgetActions.ACTION_REPEAT,
            )
        ) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        val future = MediaController.Builder(
            appContext,
            SessionToken(
                appContext,
                ComponentName(appContext, SonoraPlaybackService::class.java),
            ),
        ).buildAsync()

        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull()
                if (controller == null) {
                    MediaController.releaseFuture(future)
                    pendingResult.finish()
                    return@addListener
                }

                val needsLibraryQueue = controller.mediaItemCount <= 1 && when (action) {
                    SonoraWidgetActions.ACTION_TOGGLE_PLAYBACK ->
                        controller.currentMediaItem == null
                    SonoraWidgetActions.ACTION_PREVIOUS,
                    SonoraWidgetActions.ACTION_NEXT,
                    SonoraWidgetActions.ACTION_SHUFFLE,
                    SonoraWidgetActions.ACTION_REPEAT -> true
                    else -> false
                }

                if (!needsLibraryQueue) {
                    try {
                        performAction(appContext, controller, action)
                    } catch (_: Exception) {
                    } finally {
                        MediaController.releaseFuture(future)
                        pendingResult.finish()
                    }
                } else {
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val songs = runCatching {
                            AndroidMusicRepository(appContext.contentResolver).songs()
                        }.getOrDefault(emptyList())

                        withContext(Dispatchers.Main.immediate) {
                            try {
                                ensureLibraryQueue(appContext, controller, songs)
                                performAction(appContext, controller, action)
                            } catch (_: Exception) {
                            } finally {
                                MediaController.releaseFuture(future)
                                pendingResult.finish()
                            }
                        }
                    }
                }
            },
            appContext.mainExecutor,
        )
    }

    private fun performAction(
        context: Context,
        controller: MediaController,
        action: String,
    ) {
        when (action) {
            SonoraWidgetActions.ACTION_TOGGLE_PLAYBACK -> {
                if (controller.currentMediaItem == null) {
                    openSonora(context)
                } else if (controller.playWhenReady) {
                    controller.pause()
                } else {
                    controller.play()
                }
            }

            SonoraWidgetActions.ACTION_PREVIOUS -> {
                if (controller.currentPosition > PREVIOUS_RESTART_THRESHOLD_MS) {
                    controller.seekTo(0L)
                } else if (controller.hasPreviousMediaItem()) {
                    controller.seekToPreviousMediaItem()
                }
            }

            SonoraWidgetActions.ACTION_NEXT -> {
                if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
            }

            SonoraWidgetActions.ACTION_SHUFFLE -> {
                controller.shuffleModeEnabled = !controller.shuffleModeEnabled
            }

            SonoraWidgetActions.ACTION_REPEAT -> {
                controller.repeatMode = when (controller.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        }

        context.sendBroadcast(
            Intent(SonoraWidgetActions.ACTION_REFRESH).setPackage(context.packageName),
        )
    }

    private fun ensureLibraryQueue(
        context: Context,
        controller: MediaController,
        songs: List<Song>,
    ) {
        if (songs.isEmpty() || controller.mediaItemCount > 1) return

        val currentItem = controller.currentMediaItem
        val currentSong = findCurrentSong(songs, currentItem)
        if (currentItem != null && currentSong == null) return
        if (currentItem != null && songs.size == 1) return

        val preferences = SonoraPreferences(context)
        val savedSong = preferences.lastPlayed()
        val targetSong = currentSong ?: savedSong?.let { saved ->
            songs.firstOrNull { it.id == saved.id } 
        } ?: songs.firstOrNull()
        val targetIndex = targetSong?.let(songs::indexOf)?.takeIf { it >= 0 } ?: 0
        val position = when {
            currentItem != null -> controller.currentPosition.coerceAtLeast(0L)
            preferences.resumePlayback() && targetSong != null ->
                preferences.lastPlayedPositionMs(targetSong.id).coerceAtLeast(0L)
            else -> 0L
        }

        val wasPlayWhenReady = currentItem != null && controller.playWhenReady
        val shuffleMode = controller.shuffleModeEnabled
        val repeatMode = controller.repeatMode

        controller.setMediaItems(
            songs.map { song -> song.toMediaItem() },
            targetIndex,
            position,
        )
        controller.shuffleModeEnabled = shuffleMode
        controller.repeatMode = repeatMode
        controller.prepare()
        controller.playWhenReady = wasPlayWhenReady
    }

    private fun findCurrentSong(songs: List<Song>, item: MediaItem?): Song? {
        if (item == null) return null
        val mediaId = item.mediaId.toLongOrNull()
        val mediaUri = item.localConfiguration?.uri?.toString()
        return songs.firstOrNull { song ->
            (mediaId != null && song.id == mediaId) || sameMediaUri(song.uri, mediaUri)
        } ?: run {
            val title = item.mediaMetadata.title?.toString().orEmpty()
            val artist = item.mediaMetadata.artist?.toString().orEmpty()
            songs.firstOrNull { song ->
                title.isNotBlank() && artist.isNotBlank() &&
                    song.title.equals(title, ignoreCase = true) &&
                    song.artist.equals(artist, ignoreCase = true)
            }
        }
    }

    private fun sameMediaUri(first: String, second: String?): Boolean {
        if (second.isNullOrBlank()) return false
        return runCatching {
            Uri.parse(first).normalizeScheme() == Uri.parse(second).normalizeScheme()
        }.getOrDefault(first == second)
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri?.let(Uri::parse))
                    .build(),
            )
            .build()

    private fun openSonora(context: Context) {
        context.startActivity(
            Intent(context, com.sipun.sonora.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    private companion object {
        const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L
    }
}
