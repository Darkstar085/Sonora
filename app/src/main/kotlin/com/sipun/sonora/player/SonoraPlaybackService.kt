package com.sipun.sonora.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sipun.sonora.MainActivity
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.SonoraPreferences

@OptIn(UnstableApi::class)
class SonoraPlaybackService : MediaSessionService() {
    private val preferences by lazy { SonoraPreferences(applicationContext) }
    private val transitionHandler = Handler(Looper.getMainLooper())
    private var mediaSession: MediaSession? = null
    private var activePlayer: ExoPlayer? = null
    private var crossfadePlayer: ExoPlayer? = null
    private var crossfadeFromIndex = C.INDEX_UNSET
    private var crossfadeTargetIndex = C.INDEX_UNSET
    private var crossfadeStartElapsedMs = 0L
    private var crossfadeDurationMs = 0L

    override fun onCreate() {
        super.onCreate()
        setForegroundServiceTimeoutMs(120_000L)

        val initialPlayer = buildPlayer(handleAudioFocus = true)
        activePlayer = initialPlayer
        initialPlayer.addListener(serviceListener)

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            .also { it.setSmallIcon(R.drawable.sonora_launcher_monochrome) }

        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, initialPlayer)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

        addSession(mediaSession!!)
        transitionHandler.post(transitionRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        transitionHandler.removeCallbacks(transitionRunnable)
        cancelCrossfade()
        mediaSession?.release()
        mediaSession = null
        activePlayer?.release()
        activePlayer = null
        super.onDestroy()
    }

    private fun buildPlayer(handleAudioFocus: Boolean): ExoPlayer =
        ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(handleAudioFocus)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.DEFAULT,
                handleAudioFocus,
            )
            .setPauseAtEndOfMediaItems(
                preferences.crossfadeSeconds() == 0 && !preferences.gaplessPlayback(),
            )
            .build()

    private val serviceListener = object : Player.Listener {
        override fun onMediaItemTransition(
            mediaItem: androidx.media3.common.MediaItem?,
            reason: Int,
        ) {
            val active = activePlayer ?: return

            if (crossfadePlayer != null) {
                when {
                    active.currentMediaItemIndex == crossfadeTargetIndex &&
                        reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                        active.volume = 0f
                    }

                    active.currentMediaItemIndex != crossfadeFromIndex -> {
                        cancelCrossfade()
                        active.volume = 1f
                    }
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady &&
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                preferences.crossfadeSeconds() == 0 &&
                !preferences.gaplessPlayback()
            ) {
                transitionHandler.postDelayed(
                    { activePlayer?.play() },
                    NON_GAPLESS_DELAY_MS,
                )
            }
        }

        override fun onTimelineChanged(
            timeline: androidx.media3.common.Timeline,
            reason: Int,
        ) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED && crossfadePlayer != null) {
                cancelCrossfade()
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (crossfadePlayer != null) cancelCrossfade()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            if (crossfadePlayer != null) cancelCrossfade()
        }
    }

    private val transitionRunnable = object : Runnable {
        override fun run() {
            val active = activePlayer

            if (active != null) {
                val crossfadeMs = preferences.crossfadeSeconds() * 1_000L
                active.setPauseAtEndOfMediaItems(
                    crossfadeMs == 0L && !preferences.gaplessPlayback(),
                )

                if (crossfadePlayer != null) {
                    updateCrossfade(active)
                } else if (crossfadeMs > 0L) {
                    maybePrepareCrossfade(active, crossfadeMs)
                } else if (active.volume != 1f) {
                    active.volume = 1f
                }
            }

            transitionHandler.postDelayed(this, TRANSITION_TICK_MS)
        }
    }

    private fun maybePrepareCrossfade(active: ExoPlayer, crossfadeMs: Long) {
        if (!active.isPlaying || active.currentMediaItemIndex < 0) return

        val duration = active.duration
        if (duration == C.TIME_UNSET || duration <= 0L) return

        val remaining = duration - active.currentPosition
        val prepareThreshold = crossfadeMs + PREPARE_BUFFER_MS
        if (remaining !in 0..prepareThreshold || !active.hasNextMediaItem()) return

        val nextIndex = active.getNextMediaItemIndex()
        if (nextIndex == C.INDEX_UNSET) return

        val items = List(active.mediaItemCount) { active.getMediaItemAt(it) }
        val next = buildPlayer(handleAudioFocus = false)

        next.setMediaItems(items, nextIndex, 0L)
        next.shuffleModeEnabled = active.shuffleModeEnabled
        next.repeatMode = active.repeatMode
        next.playbackParameters = active.playbackParameters
        next.volume = 0f
        next.prepare()

        crossfadePlayer = next
        crossfadeFromIndex = active.currentMediaItemIndex
        crossfadeTargetIndex = nextIndex
        crossfadeStartElapsedMs = 0L
        crossfadeDurationMs = crossfadeMs
    }

    private fun updateCrossfade(active: ExoPlayer) {
        val next = crossfadePlayer ?: return

        if (active.currentMediaItemIndex == crossfadeTargetIndex) {
            finalizeCrossfade(active, next)
            return
        }

        if (active.currentMediaItemIndex != crossfadeFromIndex) {
            cancelCrossfade()
            active.volume = 1f
            return
        }

        val remaining = active.duration - active.currentPosition
        if (crossfadeStartElapsedMs == 0L) {
            if (remaining <= crossfadeDurationMs) {
                next.play()
            }
            if (!next.isPlaying) return
            crossfadeStartElapsedMs = SystemClock.elapsedRealtime()
        } else if (!next.isPlaying) {
            return
        }

        val elapsed = SystemClock.elapsedRealtime() - crossfadeStartElapsedMs
        val progress = (elapsed.toFloat() / crossfadeDurationMs.coerceAtLeast(1L))
            .coerceIn(0f, 1f)

        active.volume = 1f - progress
        next.volume = progress

        if (progress >= 1f) {
            finalizeCrossfade(active, next)
        }
    }

    private fun finalizeCrossfade(oldPlayer: ExoPlayer, nextPlayer: ExoPlayer) {
        nextPlayer.volume = 1f
        nextPlayer.addListener(serviceListener)
        mediaSession?.setPlayer(nextPlayer)
        activePlayer = nextPlayer

        oldPlayer.removeListener(serviceListener)
        oldPlayer.stop()
        oldPlayer.release()

        crossfadePlayer = null
        crossfadeFromIndex = C.INDEX_UNSET
        crossfadeTargetIndex = C.INDEX_UNSET
        crossfadeStartElapsedMs = 0L
        crossfadeDurationMs = 0L
    }

    private fun cancelCrossfade() {
        crossfadePlayer?.let { next ->
            next.stop()
            next.release()
        }
        crossfadePlayer = null
        crossfadeFromIndex = C.INDEX_UNSET
        crossfadeTargetIndex = C.INDEX_UNSET
        crossfadeStartElapsedMs = 0L
        crossfadeDurationMs = 0L
        activePlayer?.volume = 1f
    }

    private companion object {
        private const val NOTIFICATION_CHANNEL_ID = "sonora_playback"
        private const val TRANSITION_TICK_MS = 50L
        private const val PREPARE_BUFFER_MS = 3_000L
        private const val NON_GAPLESS_DELAY_MS = 350L
    }
}
