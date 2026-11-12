package com.chillysloth.neomusic.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val albumArtist: String?,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val dateAddedSec: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val path: String?,
    val relativePath: String?,
    val displayName: String?,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val tracks: List<Track>,
) {
    val durationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class Artist(
    val id: Long,
    val name: String,
    val tracks: List<Track>,
    val albums: List<Album>,
) {
    /** Album used for the artist's cover art. */
    val coverAlbumId: Long get() = albums.firstOrNull()?.id ?: tracks.firstOrNull()?.albumId ?: -1
}

data class Playlist(
    val id: Long,
    val name: String,
    val trackIds: List<Long>,
    val createdAt: Long,
    val modifiedAt: Long,
    /** Album whose art is used as the cover, chosen by the user. */
    val coverAlbumId: Long? = null,
    /** File name (in app storage) of a custom cover image picked from the gallery. */
    val coverImage: String? = null,
)

data class Library(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val loaded: Boolean = false,
) {
    val trackById: Map<Long, Track> by lazy { tracks.associateBy { it.id } }
    val albumById: Map<Long, Album> by lazy { albums.associateBy { it.id } }
    val artistById: Map<Long, Artist> by lazy { artists.associateBy { it.id } }
}

enum class TrackSort(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Date added"),
    DURATION("Duration"),
    YEAR("Year"),
}

enum class PlaylistTrackSort(val label: String) {
    CUSTOM("Custom order"),
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE_ADDED("Date added"),
}

enum class PlaylistSort(val label: String) {
    CUSTOM("Custom order"),
    NAME("Name"),
    CREATED("Date created"),
    MODIFIED("Date modified"),
    TRACK_COUNT("Number of tracks"),
}

enum class AlbumSort(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    YEAR("Year"),
    TRACK_COUNT("Number of tracks"),
}

enum class ArtistSort(val label: String) {
    NAME("Name"),
    TRACK_COUNT("Number of tracks"),
    ALBUM_COUNT("Number of albums"),
}

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class SortSpec<T : Enum<T>>(val key: T, val descending: Boolean)

private val collator = java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }

/** Locale-aware, case-insensitive comparison that ignores leading "The ". */
fun sortKey(s: String): String = s.trim().removePrefix("The ").removePrefix("the ")

val textComparator: Comparator<String> = Comparator { a, b -> collator.compare(sortKey(a), sortKey(b)) }

fun List<Track>.sortedTracks(sort: SortSpec<TrackSort>): List<Track> {
    val c: Comparator<Track> = when (sort.key) {
        TrackSort.TITLE -> compareBy(textComparator) { it.title }
        TrackSort.ARTIST -> compareBy<Track, String>(textComparator) { it.artist }
            .thenBy(textComparator) { it.album }.thenBy { it.trackNumber }
        TrackSort.ALBUM -> compareBy<Track, String>(textComparator) { it.album }.thenBy { it.trackNumber }
        TrackSort.DATE_ADDED -> compareBy { it.dateAddedSec }
        TrackSort.DURATION -> compareBy { it.durationMs }
        TrackSort.YEAR -> compareBy<Track> { it.year }.thenBy(textComparator) { it.album }.thenBy { it.trackNumber }
    }
    return sortedWith(if (sort.descending) c.reversed() else c)
}

fun List<Track>.sortedPlaylistTracks(sort: SortSpec<PlaylistTrackSort>): List<Track> {
    val mapped = when (sort.key) {
        PlaylistTrackSort.CUSTOM -> return if (sort.descending) reversed() else this
        PlaylistTrackSort.TITLE -> TrackSort.TITLE
        PlaylistTrackSort.ARTIST -> TrackSort.ARTIST
        PlaylistTrackSort.ALBUM -> TrackSort.ALBUM
        PlaylistTrackSort.DURATION -> TrackSort.DURATION
        PlaylistTrackSort.DATE_ADDED -> TrackSort.DATE_ADDED
    }
    return sortedTracks(SortSpec(mapped, sort.descending))
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatLongDuration(ms: Long): String {
    val totalMin = ms / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h} h ${m} min" else "${m} min"
}

fun pluralTracks(n: Int) = if (n == 1) "1 track" else "$n tracks"
fun pluralAlbums(n: Int) = if (n == 1) "1 album" else "$n albums"
