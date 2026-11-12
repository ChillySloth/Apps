package com.chillysloth.neomusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Playlists stored as a small JSON file in app storage. The list order in the file
 * is the user's custom playlist order.
 */
class PlaylistStore(private val context: Context, private val scope: CoroutineScope) {

    private val file by lazy { File(context.filesDir, "playlists.json") }
    private val coversDir by lazy { File(context.filesDir, "covers") }

    /** Where a custom cover image named [name] lives. */
    fun coverFile(name: String): File = File(coversDir, name)
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
                coverAlbumId = if (o.has("coverAlbum")) o.getLong("coverAlbum") else null,
                coverImage = o.optString("coverImage").takeIf { it.isNotEmpty() },
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
                        p.coverAlbumId?.let { put("coverAlbum", it) }
                        p.coverImage?.let { put("coverImage", it) }
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

    fun delete(id: Long) {
        _playlists.value.firstOrNull { it.id == id }?.coverImage?.let(::deleteCoverFile)
        update { list -> list.filterNot { it.id == id } }
    }

    /** Uses an album's art as the cover. */
    fun setCoverAlbum(id: Long, albumId: Long) = setCover(id, albumId = albumId, image = null)

    /** Back to the automatic cover (mosaic of the playlist's albums). */
    fun resetCover(id: Long) = setCover(id, albumId = null, image = null)

    private fun setCover(id: Long, albumId: Long?, image: String?) {
        val old = _playlists.value.firstOrNull { it.id == id }?.coverImage
        edit(id) { it.copy(coverAlbumId = albumId, coverImage = image) }
        if (old != null && old != image) deleteCoverFile(old)
    }

    private fun deleteCoverFile(name: String) {
        scope.launch(Dispatchers.IO) { runCatching { coverFile(name).delete() } }
    }

    /**
     * Copies a picked image into app storage as a square JPEG and makes it the cover.
     * Returns false if the image couldn't be read.
     */
    suspend fun importCoverImage(id: Long, uri: Uri): Boolean {
        val name = withContext(Dispatchers.IO) {
            runCatching {
                val square = decodeSquare(uri, 1000) ?: return@runCatching null
                coversDir.mkdirs()
                // Unique name per change, so cached images of the old cover are never reused.
                val name = "playlist_${id}_${System.currentTimeMillis()}.jpg"
                coverFile(name).outputStream().use { square.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                square.recycle()
                name
            }.getOrNull()
        } ?: return false
        setCover(id, albumId = null, image = name)
        return true
    }

    /** Decodes [uri] (respecting EXIF rotation on Android 9+), center-crops it square and caps it at [maxPx]. */
    private fun decodeSquare(uri: Uri, maxPx: Int): Bitmap? {
        val resolver = context.contentResolver
        val bmp: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val shortSide = minOf(info.size.width, info.size.height)
                if (shortSide > maxPx) {
                    val scale = maxPx.toFloat() / shortSide
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null
        }
        val side = minOf(bmp.width, bmp.height)
        val cropped = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        if (cropped !== bmp) bmp.recycle()
        if (side <= maxPx) return cropped
        val scaled = Bitmap.createScaledBitmap(cropped, maxPx, maxPx, true)
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }

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
