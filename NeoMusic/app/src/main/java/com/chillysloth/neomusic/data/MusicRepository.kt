package com.chillysloth.neomusic.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reads the device audio library from MediaStore and keeps it up to date. */
class MusicRepository(private val context: Context, private val scope: CoroutineScope) {

    private val _library = MutableStateFlow(Library())
    val library: StateFlow<Library> = _library.asStateFlow()

    private var observerRegistered = false
    private var pendingReload: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            pendingReload?.cancel()
            pendingReload = scope.launch {
                delay(600) // MediaStore fires bursts of changes; coalesce them.
                reload()
            }
        }
    }

    fun start() {
        if (!observerRegistered) {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
            )
            observerRegistered = true
        }
        scope.launch { reload() }
    }

    suspend fun reload() {
        val tracks = withContext(Dispatchers.IO) { runCatching { queryTracks(context.contentResolver) }.getOrDefault(emptyList()) }
        _library.value = buildLibrary(tracks)
    }

    /** Shows a fixed library (previews / screenshot tests). */
    fun setPreviewTracks(tracks: List<Track>) {
        _library.value = buildLibrary(tracks)
    }

    private fun queryTracks(resolver: ContentResolver): List<Track> {
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            @Suppress("DEPRECATION") MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            "album_artist",
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) projection += MediaStore.Audio.Media.RELATIVE_PATH

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val result = ArrayList<Track>()
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection.toTypedArray(),
            selection,
            null,
            null,
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iArtistId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val iDate = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val iMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            @Suppress("DEPRECATION")
            val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val iAlbumArtist = c.getColumnIndex("album_artist")
            val iRel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1

            while (c.moveToNext()) {
                val name = c.getString(iName)
                val title = c.getString(iTitle)?.takeIf { it.isNotBlank() }
                    ?: name?.substringBeforeLast('.') ?: "Unknown"
                val rawTrack = c.getInt(iTrack)
                result += Track(
                    id = c.getLong(iId),
                    title = title,
                    artist = cleanUnknown(c.getString(iArtist), "Unknown artist"),
                    artistId = c.getLong(iArtistId),
                    album = cleanUnknown(c.getString(iAlbum), "Unknown album"),
                    albumId = c.getLong(iAlbumId),
                    albumArtist = if (iAlbumArtist >= 0) c.getString(iAlbumArtist)?.takeIf { it.isNotBlank() } else null,
                    durationMs = c.getLong(iDuration),
                    // MediaStore encodes disc number in the thousands (e.g. 2005 = disc 2, track 5).
                    trackNumber = rawTrack,
                    year = c.getInt(iYear),
                    dateAddedSec = c.getLong(iDate),
                    sizeBytes = c.getLong(iSize),
                    mimeType = c.getString(iMime),
                    path = c.getString(iData),
                    relativePath = if (iRel >= 0) c.getString(iRel) else null,
                    displayName = name,
                )
            }
        }
        return result
    }

    private fun cleanUnknown(value: String?, fallback: String): String =
        if (value.isNullOrBlank() || value == MediaStore.UNKNOWN_STRING) fallback else value

    private fun buildLibrary(tracks: List<Track>): Library {
        val albums = tracks.groupBy { it.albumId }.map { (id, ts) ->
            val sorted = ts.sortedWith(compareBy<Track> { it.trackNumber }.thenBy(textComparator) { it.title })
            val artist = ts.firstNotNullOfOrNull { it.albumArtist }
                ?: ts.map { it.artist }.distinct().let { if (it.size == 1) it[0] else "Various artists" }
            Album(
                id = id,
                title = ts.first().album,
                artist = artist,
                year = ts.maxOf { it.year },
                tracks = sorted,
            )
        }.sortedWith(compareBy(textComparator) { it.title })

        val albumsById = albums.associateBy { it.id }
        val artists = tracks.groupBy { it.artistId }.map { (id, ts) ->
            val artistAlbums = ts.map { it.albumId }.distinct().mapNotNull { albumsById[it] }
                .sortedByDescending { it.year }
            Artist(
                id = id,
                name = ts.first().artist,
                tracks = ts.sortedWith(compareBy<Track, String>(textComparator) { it.album }.thenBy { it.trackNumber }),
                albums = artistAlbums,
            )
        }.sortedWith(compareBy(textComparator) { it.name })

        return Library(
            tracks = tracks.sortedWith(compareBy(textComparator) { it.title }),
            albums = albums,
            artists = artists,
            loaded = true,
        )
    }

    /**
     * Deletes tracks. On Android 11+ the system shows its own confirmation, so an
     * [IntentSender] is returned for the caller to launch. Returns null when the
     * delete already happened.
     */
    suspend fun deleteTracks(tracks: List<Track>): DeleteResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createDeleteRequest(resolver, tracks.map { it.uri })
            return@withContext DeleteResult.NeedsConfirmation(pi.intentSender)
        }
        try {
            tracks.forEach { resolver.delete(it.uri, null, null) }
            DeleteResult.Done
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                DeleteResult.NeedsConfirmation(e.userAction.actionIntent.intentSender)
            } else {
                DeleteResult.Failed(e.message ?: "Permission denied")
            }
        }
    }

    sealed interface DeleteResult {
        data object Done : DeleteResult
        data class NeedsConfirmation(val intentSender: IntentSender) : DeleteResult
        data class Failed(val message: String) : DeleteResult
    }
}
