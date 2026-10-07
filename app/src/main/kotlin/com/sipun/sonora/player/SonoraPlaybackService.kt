package com.sipun.sonora.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sipun.sonora.MainActivity
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.R

class SonoraPlaybackService : MediaSessionService() {
    private val preferences by lazy { SonoraPreferences(applicationContext) }
    private val transitionHandler = Handler(Looper.getMainLooper())
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setForegroundServiceTimeoutMs(120_000L)

        val player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            .also { it.setSmallIcon(R.drawable.sonora_launcher_monochrome) }

        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()

        addSession(mediaSession!!)
        initialPlayer.addListener(serviceListener)
        transitionHandler.post(transitionRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        transitionHandler.removeCallbacks(transitionRunnable)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private val serviceListener = object : androidx.media3.common.Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady &&
                reason == androidx.media3.common.Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                !preferences.gaplessPlayback()
            ) {
                transitionHandler.postDelayed({ mediaSession?.player?.play() }, NON_GAPLESS_DELAY_MS)
            }
        }
    }

    private val transitionRunnable = object : Runnable {
        override fun run() {
            mediaSession?.player?.setPauseAtEndOfMediaItems(!preferences.gaplessPlayback())
            transitionHandler.postDelayed(this, TRANSITION_TICK_MS)
        }
    }

    private companion object {
        private const val NOTIFICATION_CHANNEL_ID = "sonora_playback"
        private const val TRANSITION_TICK_MS = 100L
        private const val NON_GAPLESS_DELAY_MS = 350L
    }
}
