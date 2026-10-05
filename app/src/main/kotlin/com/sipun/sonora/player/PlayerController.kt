package com.sipun.sonora.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerController(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = SonoraPreferences(appContext)
    private val artworkLoader = ArtworkLoader(appContext)
    private val artworkScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var artworkJob: Job? = null
    private val _state = MutableStateFlow(
        PlayerState(currentSong = preferences.lastPlayed()),
    )
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<Song>, Int>? = null
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, SonoraPlaybackService::class.java)),
    ).buildAsync()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateState()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            loadCurrentArtwork()
        }
    }

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    pendingQueue?.let { (songs, index) ->
                        pendingQueue = null
                        playQueue(songs, index)
                    }
                    updateState()
                }
            },
            context.mainExecutor,
        )
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val mediaController = controller ?: run {
            pendingQueue = songs to startIndex
            return
        }
        mediaController.setMediaItems(
            songs.map(::toMediaItem),
            startIndex.coerceIn(0, songs.lastIndex),
            C.TIME_UNSET,
        )
        mediaController.prepare()
        mediaController.play()
        loadCurrentArtwork()
        updateState(songs)
    }

    fun playNext(song: Song) {
        controller?.let { mediaController ->
            val item = toMediaItem(song)
            val insertIndex = (mediaController.currentMediaItemIndex + 1).coerceAtLeast(0)
            mediaController.addMediaItem(insertIndex, item)
            updateState()
        }
    }

    fun addToQueue(song: Song) {
        controller?.let { mediaController ->
            mediaController.addMediaItem(toMediaItem(song))
            updateState()
        }
    }

    fun togglePlayPause() {
        controller?.let { mediaController ->
            if (mediaController.isPlaying) {
                mediaController.pause()
            } else if (mediaController.currentMediaItem != null) {
                mediaController.play()
            } else {
                preferences.lastPlayed()?.let { song ->
                    mediaController.setMediaItem(toMediaItem(song))
                    mediaController.prepare()
                    mediaController.play()
                    loadCurrentArtwork()
                }
            }
        }
        updateState()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        updateState()
    }

    fun skipNext() {
        controller?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem()
        updateState()
    }

    fun skipPrevious() {
        controller?.let {
            if (it.currentPosition > 3_000L) it.seekTo(0L)
            else if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem()
        }
        updateState()
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
        updateState()
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        updateState()
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
        updateState()
    }

    fun refresh() = updateState()

    fun release() {
        artworkJob?.cancel()
        artworkScope.cancel()
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
    }

    private fun loadCurrentArtwork() {
        val mediaController = controller ?: return
        val item = mediaController.currentMediaItem ?: return
        if (item.mediaMetadata.artworkData != null) return

        artworkJob?.cancel()
        artworkJob = artworkScope.launch {
            val artwork = withContext(Dispatchers.IO) {
                artworkLoader.extractEmbeddedArtwork(item.localConfiguration?.uri)
            } ?: return@launch

            val current = mediaController.currentMediaItem ?: return@launch
            if (current.mediaId != item.mediaId) return@launch

            val metadata = current.mediaMetadata.buildUpon()
                .setArtworkData(
                    artwork,
                    MediaMetadata.PICTURE_TYPE_FRONT_COVER,
                )
                .build()
            mediaController.replaceMediaItem(
                mediaController.currentMediaItemIndex,
                current.buildUpon()
                    .setMediaMetadata(metadata)
                    .build(),
            )
        }
    }

    private fun toMediaItem(song: Song): MediaItem =
        MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri?.let(Uri::parse))
                    .build(),
            )
            .build()

    private fun updateState(queue: List<Song>? = null) {
        val mediaController = controller ?: return
        val currentItem = mediaController.currentMediaItem
        val resolvedQueue = queue ?: _state.value.queue
        val currentSong = resolvedQueue.firstOrNull { it.id.toString() == currentItem?.mediaId }
            ?: currentItem?.toSong()
            ?: _state.value.currentSong

        currentSong?.let(preferences::saveLastPlayed)
        _state.value = PlayerState(
            currentSong = currentSong,
            isPlaying = mediaController.isPlaying,
            positionMs = mediaController.currentPosition.coerceAtLeast(0L),
            durationMs = mediaController.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L)
                ?: 0L,
            queue = resolvedQueue,
            queueIndex = currentSong?.let(resolvedQueue::indexOf) ?: -1,
            hasPrevious = mediaController.hasPreviousMediaItem(),
            hasNext = mediaController.hasNextMediaItem(),
            shuffleEnabled = mediaController.shuffleModeEnabled,
            repeatMode = when (mediaController.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            },
        )
    }

    private fun MediaItem.toSong(): Song {
        val metadata = mediaMetadata
        val id = mediaId.toLongOrNull() ?: 0L
        val uri = localConfiguration?.uri?.toString().orEmpty()
        return Song(
            id = id,
            title = metadata.title?.toString().orEmpty().ifBlank { "Unknown title" },
            artist = metadata.artist?.toString().orEmpty().ifBlank { "Unknown artist" },
            album = metadata.albumTitle?.toString().orEmpty().ifBlank { "Unknown album" },
            durationMs = 0L,
            uri = uri,
            albumArtUri = metadata.artworkUri?.toString(),
        )
    }
}
