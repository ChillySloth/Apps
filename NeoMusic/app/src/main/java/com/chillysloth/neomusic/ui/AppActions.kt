package com.chillysloth.neomusic.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.chillysloth.neomusic.data.Track

/** Everything a screen can ask the app shell to do. Provided by [AppRoot]. */
interface AppActions {
    /** [shuffle]: true = shuffle, false = in order, null = keep the player's current shuffle mode. */
    fun play(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean? = null)
    fun openAlbum(id: Long)
    fun openArtist(id: Long)
    fun openPlaylist(id: Long)
    fun openTrackDetails(id: Long)
    fun openSearch()
    fun openTrackPicker(playlistId: Long)
    fun back()
    fun addToPlaylist(tracks: List<Track>)
    fun delete(tracks: List<Track>)
    fun createPlaylist(initialTracks: List<Track> = emptyList(), openAfter: Boolean = false)
    fun renamePlaylist(id: Long)
    fun deletePlaylist(id: Long)
    fun changePlaylistCover(id: Long)
    fun toast(message: String)
}

val LocalActions = staticCompositionLocalOf<AppActions> { error("AppActions not provided") }
