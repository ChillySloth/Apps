package com.chillysloth.neomusic.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small persisted UI preferences (theme + sort choices). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("neo_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(enumOr(sp.getString("theme", null), ThemeMode.SYSTEM))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()
    fun setTheme(mode: ThemeMode) {
        _theme.value = mode; sp.edit().putString("theme", mode.name).apply()
    }

    val trackSort = sortPref("track_sort", TrackSort.TITLE)
    val playlistSort = sortPref("playlist_sort", PlaylistSort.CUSTOM)
    val albumSort = sortPref("album_sort", AlbumSort.TITLE)
    val artistSort = sortPref("artist_sort", ArtistSort.NAME)

    /** Sort inside each playlist, stored per playlist id. */
    fun playlistTrackSort(playlistId: Long): SortSpec<PlaylistTrackSort> =
        SortSpec(
            enumOr(sp.getString("pl_sort_$playlistId", null), PlaylistTrackSort.CUSTOM),
            sp.getBoolean("pl_sort_${playlistId}_desc", false),
        )

    fun setPlaylistTrackSort(playlistId: Long, spec: SortSpec<PlaylistTrackSort>) {
        sp.edit().putString("pl_sort_$playlistId", spec.key.name)
            .putBoolean("pl_sort_${playlistId}_desc", spec.descending).apply()
    }

    private inline fun <reified T : Enum<T>> sortPref(key: String, default: T): SortPref<T> {
        val initial = SortSpec(enumOr(sp.getString(key, null), default), sp.getBoolean("${key}_desc", false))
        return SortPref(initial) { spec ->
            sp.edit().putString(key, spec.key.name).putBoolean("${key}_desc", spec.descending).apply()
        }
    }

    class SortPref<T : Enum<T>>(initial: SortSpec<T>, private val persist: (SortSpec<T>) -> Unit) {
        private val state = MutableStateFlow(initial)
        val flow: StateFlow<SortSpec<T>> = state.asStateFlow()
        fun set(spec: SortSpec<T>) {
            state.value = spec; persist(spec)
        }
    }
}

inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
    name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
