package com.chillysloth.neomusic.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.chillysloth.neomusic.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentTrackId: Long? = null,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Long> = emptyList(),
    val currentIndex: Int = -1,
    val durationMs: Long = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

/** Wraps a [MediaController] bound to [PlaybackService] and exposes simple state flows. */
class PlayerConnection(private val context: Context, private val scope: CoroutineScope) {

    private var controller: MediaController? = null
    private var connecting = false
    private val pendingActions = ArrayList<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private var ticker: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }
    }

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        scope.launch {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val c = runCatching {
                MediaController.Builder(context, token)
                    .setListener(object : MediaController.Listener {
                        override fun onDisconnected(controller: MediaController) {
                            // Service went away; reconnect lazily on the next command.
                            if (this@PlayerConnection.controller === controller) {
                                this@PlayerConnection.controller = null
                                ticker?.cancel()
                            }
                        }
                    })
                    .buildAsync().await()
            }.getOrNull()
            connecting = false
            if (c == null) return@launch
            controller = c
            c.addListener(listener)
            publish(c)
            pendingActions.forEach { it(c) }
            pendingActions.clear()
            startTicker()
        }
    }

    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else {
            pendingActions += action
            connect()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                controller?.let { _position.value = it.currentPosition.coerceAtLeast(0) }
                delay(if (_state.value.isPlaying) 250 else 1000)
            }
        }
    }

    private fun publish(p: Player) {
        val queue = (0 until p.mediaItemCount).mapNotNull { p.getMediaItemAt(it).mediaId.toLongOrNull() }
        _state.value = PlayerState(
            currentTrackId = p.currentMediaItem?.mediaId?.toLongOrNull(),
            isPlaying = p.isPlaying,
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            queue = queue,
            currentIndex = p.currentMediaItemIndex,
            durationMs = p.duration.takeIf { it > 0 } ?: 0L,
            hasNext = p.hasNextMediaItem(),
            hasPrevious = p.hasPreviousMediaItem(),
        )
        _position.value = p.currentPosition.coerceAtLeast(0)
    }

    /** Shows a fixed player state (previews / screenshot tests). */
    fun setPreviewState(state: PlayerState, positionMs: Long) {
        _state.value = state
        _position.value = positionMs
    }

    /** Replaces the queue with [tracks] and starts at [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean? = null) {
        if (tracks.isEmpty()) return
        withController { c ->
            val items = tracks.map { it.toMediaItem() }
            if (shuffle != null) c.shuffleModeEnabled = shuffle
            val start = if (shuffle == true && startIndex == 0) tracks.indices.random() else startIndex
            c.setMediaItems(items, start.coerceIn(tracks.indices), 0L)
            c.prepare()
            c.play()
        }
    }

    fun playNext(tracks: List<Track>) = withController { c ->
        if (c.mediaItemCount == 0) {
            play(tracks); return@withController
        }
        c.addMediaItems(c.currentMediaItemIndex + 1, tracks.map { it.toMediaItem() })
    }

    fun addToQueue(tracks: List<Track>) = withController { c ->
        if (c.mediaItemCount == 0) {
            play(tracks); return@withController
        }
        c.addMediaItems(tracks.map { it.toMediaItem() })
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    fun next() = withController { it.seekToNextMediaItem() }

    /** Restarts the track if more than 3 s in, otherwise goes to the previous one (like Samsung Music). */
    fun previous() = withController { c ->
        if (c.currentPosition > 3000 || !c.hasPreviousMediaItem()) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) = withController { it.seekTo(ms); _position.value = ms }

    fun skipToQueueIndex(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(index); c.play()
        }
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun removeFromQueue(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun moveInQueue(from: Int, to: Int) = withController { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    /** Drops deleted tracks from the queue. */
    fun removeTracks(ids: Set<Long>) = withController { c ->
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId.toLongOrNull() in ids) c.removeMediaItem(i)
        }
    }

    fun release() {
        ticker?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setAlbumArtist(albumArtist)
            .setArtworkUri(Uri.parse("content://media/external/audio/albumart/$albumId"))
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setExtras(bundleOf("albumId" to albumId))
            .build()
    )
    .build()
