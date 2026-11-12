package com.chillysloth.neomusic.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Playlists stored as a small JSON file in app storage. The list order in the file
 * is the user's custom playlist order.
 */
class PlaylistStore(context: Context, private val scope: CoroutineScope) {

    private val file by lazy { File(context.filesDir, "playlists.json") }
    private val writeLock = Mutex()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        _playlists.value = runCatching { read() }.getOrDefault(emptyList())
    }

    /** Shows fixed playlists (previews / screenshot tests). Not persisted. */
    fun setPreviewPlaylists(list: List<Playlist>) {
        _playlists.value = list
    }

    private fun read(): List<Playlist> {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("tracks")
            Playlist(
                id = o.getLong("id"),
                name = o.getString("name"),
                trackIds = (0 until ids.length()).map { ids.getLong(it) },
                createdAt = o.optLong("created"),
                modifiedAt = o.optLong("modified"),
            )
        }
    }

    private fun update(transform: (List<Playlist>) -> List<Playlist>) {
        val next = transform(_playlists.value)
        _playlists.value = next
        scope.launch(Dispatchers.IO) {
            writeLock.withLock {
                val arr = JSONArray()
                next.forEach { p ->
                    arr.put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("created", p.createdAt)
                        put("modified", p.modifiedAt)
                        put("tracks", JSONArray().apply { p.trackIds.forEach { put(it) } })
                    })
                }
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(arr.toString())
                tmp.renameTo(file)
            }
        }
    }

    fun create(name: String, initialTracks: List<Long> = emptyList()): Long {
        val now = System.currentTimeMillis()
        val id = (_playlists.value.maxOfOrNull { it.id } ?: 0L).coerceAtLeast(now) + 1
        update { it + Playlist(id, name.trim(), initialTracks.distinct(), now, now) }
        return id
    }

    fun rename(id: Long, name: String) = edit(id) { it.copy(name = name.trim()) }

    fun delete(id: Long) = update { list -> list.filterNot { it.id == id } }

    /** Adds tracks to the end of the playlist, skipping ones already in it. Returns number added. */
    fun addTracks(id: Long, trackIds: List<Long>): Int {
        var added = 0
        edit(id) { p ->
            val existing = p.trackIds.toHashSet()
            val fresh = trackIds.distinct().filterNot { it in existing }
            added = fresh.size
            p.copy(trackIds = p.trackIds + fresh)
        }
        return added
    }

    fun removeTrack(id: Long, trackId: Long) = edit(id) { p -> p.copy(trackIds = p.trackIds - trackId) }

    fun setTrackOrder(id: Long, trackIds: List<Long>) = edit(id) { it.copy(trackIds = trackIds) }

    /** Reorders playlists themselves (custom sort). */
    fun movePlaylist(from: Int, to: Int) = update { list ->
        if (from !in list.indices || to !in list.indices) list
        else list.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Called after files are deleted so playlists don't keep dangling entries. */
    fun purgeTracks(trackIds: Set<Long>) = update { list ->
        list.map { p ->
            if (p.trackIds.any { it in trackIds }) p.copy(trackIds = p.trackIds.filterNot { it in trackIds }) else p
        }
    }

    private fun edit(id: Long, block: (Playlist) -> Playlist) = update { list ->
        list.map { if (it.id == id) block(it).copy(modifiedAt = System.currentTimeMillis()) else it }
    }
}
