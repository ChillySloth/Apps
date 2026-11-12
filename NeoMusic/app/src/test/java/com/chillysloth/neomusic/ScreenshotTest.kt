package com.chillysloth.neomusic

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.chillysloth.neomusic.data.Playlist
import com.chillysloth.neomusic.data.Track
import com.chillysloth.neomusic.playback.PlayerState
import com.chillysloth.neomusic.ui.AppActions
import com.chillysloth.neomusic.ui.AppRoot
import com.chillysloth.neomusic.ui.LocalActions
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.LocalBottomInset
import com.chillysloth.neomusic.ui.player.NowPlayingScreen
import com.chillysloth.neomusic.ui.screens.AlbumScreen
import com.chillysloth.neomusic.ui.screens.HomeScreen
import com.chillysloth.neomusic.ui.screens.SearchScreen
import com.chillysloth.neomusic.ui.screens.PlaylistScreen
import com.chillysloth.neomusic.ui.screens.TrackDetailsScreen
import com.chillysloth.neomusic.ui.theme.NeoTheme
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * Renders key screens on the JVM with fake data, so the look can be checked without a device.
 * Run: ./gradlew :app:recordPaparazziDebug  (images land in app/src/test/snapshots)
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar",
        maxPercentDifference = 0.5,
    )

    @Test fun home_light() = snap(dark = false) { AppRoot(it) }
    @Test fun home_dark() = snap(dark = true) { AppRoot(it) }
    @Test fun playlistsTab_light() = snap(dark = false) { HomeScreen(initialTab = 1) }
    @Test fun albumsTab_light() = snap(dark = false) { HomeScreen(initialTab = 2) }
    @Test fun artistsTab_dark() = snap(dark = true) { HomeScreen(initialTab = 3) }
    @Test fun search_light() = snap(dark = false) { SearchScreen() }
    @Test fun album_light() = snap(dark = false) { AlbumScreen(10) }
    // Track picker opened while a song is playing: the confirm bar must not be covered by the mini player.
    @Test fun picker_whilePlaying() = snap(dark = false) { AppRoot(it, startDestination = "pick/2") }
    @Test fun playlist_albumCover_light() = snap(dark = false) { PlaylistScreen(2) }
    @Test fun coverDialog_light() = snap(dark = false) { app ->
        val lib = app.repository.library.value
        com.chillysloth.neomusic.ui.components.PlaylistCoverDialog(
            samplePlaylists[1], listOf(12L, 11L, 10L).map { lib.albumById[it]!! },
            onDismiss = {}, onGallery = {}, onAlbum = {}, onAutomatic = {},
        )
    }
    @Test fun playlist_dark() = snap(dark = true) { PlaylistScreen(1) }
    @Test fun details_light() = snap(dark = false) { TrackDetailsScreen(3) }
    @Test fun nowPlaying_light() = snap(dark = false, inset = 0) { app ->
        NowPlayingScreen(app.repository.library.value.trackById[3]!!, showQueue = false, onToggleQueue = {}, onMinimize = {})
    }
    @Test fun nowPlaying_dark() = snap(dark = true, inset = 0) { app ->
        NowPlayingScreen(app.repository.library.value.trackById[3]!!, showQueue = false, onToggleQueue = {}, onMinimize = {})
    }
    @Test fun queue_light() = snap(dark = false, inset = 0) { app ->
        NowPlayingScreen(app.repository.library.value.trackById[3]!!, showQueue = true, onToggleQueue = {}, onMinimize = {})
    }

    private fun snap(dark: Boolean, inset: Int = 92, content: @Composable (NeoMusicApp) -> Unit) {
        if (dark) paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_6.copy(nightMode = NightMode.NIGHT))
        val app = NeoMusicApp.forPreview(paparazzi.context)
        app.repository.setPreviewTracks(sampleTracks)
        app.playlists.setPreviewPlaylists(samplePlaylists)
        app.player.setPreviewState(
            PlayerState(
                currentTrackId = 3, isPlaying = true, queue = sampleTracks.map { it.id }.take(9),
                currentIndex = 2, durationMs = 214_000, hasNext = true, hasPrevious = true,
            ),
            positionMs = 81_000,
        )
        paparazzi.snapshot {
            Providers(app, inset) { NeoTheme(dark) { content(app) } }
        }
    }

    @Composable
    private fun Providers(app: NeoMusicApp, inset: Int, content: @Composable () -> Unit) {
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
            }
        }
        val backOwner = object : OnBackPressedDispatcherOwner {
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
            override val lifecycle: Lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        CompositionLocalProvider(
            LocalApp provides app,
            LocalActions provides NoopActions,
            LocalBottomInset provides inset.dp,
            LocalActivityResultRegistryOwner provides registryOwner,
            LocalOnBackPressedDispatcherOwner provides backOwner,
            LocalLifecycleOwner provides backOwner,
            LocalViewModelStoreOwner provides object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            },
            content = content,
        )
    }

    private object NoopActions : AppActions {
        override fun play(tracks: List<Track>, startIndex: Int, shuffle: Boolean?) = Unit
        override fun openAlbum(id: Long) = Unit
        override fun openArtist(id: Long) = Unit
        override fun openPlaylist(id: Long) = Unit
        override fun openTrackDetails(id: Long) = Unit
        override fun openSearch() = Unit
        override fun openTrackPicker(playlistId: Long) = Unit
        override fun back() = Unit
        override fun addToPlaylist(tracks: List<Track>) = Unit
        override fun delete(tracks: List<Track>) = Unit
        override fun createPlaylist(initialTracks: List<Track>, openAfter: Boolean) = Unit
        override fun renamePlaylist(id: Long) = Unit
        override fun deletePlaylist(id: Long) = Unit
        override fun changePlaylistCover(id: Long) = Unit
        override fun toast(message: String) = Unit
    }

    companion object {
        private fun t(id: Long, title: String, artist: String, artistId: Long, album: String, albumId: Long, n: Int, sec: Int, year: Int) =
            Track(
                id = id, title = title, artist = artist, artistId = artistId, album = album, albumId = albumId,
                albumArtist = null, durationMs = sec * 1000L, trackNumber = n, year = year,
                dateAddedSec = 1_700_000_000L + id * 3600, sizeBytes = sec * 40_000L, mimeType = "audio/mpeg",
                path = "/storage/emulated/0/Music/$artist/$album/${"%02d".format(n)} $title.mp3",
                relativePath = "Music/$artist/$album/", displayName = "${"%02d".format(n)} $title.mp3",
            )

        val sampleTracks = listOf(
            t(1, "Concrete Sunrise", "The Paper Kites Club", 100, "Hard Edges", 10, 1, 201, 2023),
            t(2, "Offset Shadow", "The Paper Kites Club", 100, "Hard Edges", 10, 2, 188, 2023),
            t(3, "Bold Type Lover", "The Paper Kites Club", 100, "Hard Edges", 10, 3, 214, 2023),
            t(4, "Thick Lines", "The Paper Kites Club", 100, "Hard Edges", 10, 4, 245, 2023),
            t(5, "Yellow Button", "Mono Rabbit", 101, "Pink Static", 11, 1, 176, 2021),
            t(6, "Late Bus Home", "Mono Rabbit", 101, "Pink Static", 11, 2, 232, 2021),
            t(7, "Grid Paper Heart", "Mono Rabbit", 101, "Pink Static", 11, 3, 199, 2021),
            t(8, "Neon Laundromat", "Sofa Kingdom", 102, "Night Shift FM", 12, 1, 263, 2019),
            t(9, "Skate Park at 3AM", "Sofa Kingdom", 102, "Night Shift FM", 12, 2, 207, 2019),
            t(10, "Burger Thoughts", "Sofa Kingdom", 102, "Night Shift FM", 12, 3, 154, 2019),
            t(11, "Good Day Sticker", "Juniper & The Vans", 103, "Stay Positive", 13, 1, 222, 2024),
            t(12, "Let's Go", "Juniper & The Vans", 103, "Stay Positive", 13, 2, 190, 2024),
            t(13, "OMG!", "Juniper & The Vans", 103, "Stay Positive", 13, 3, 171, 2024),
        )

        val samplePlaylists = listOf(
            Playlist(1, "Morning Run", listOf(5, 1, 11, 12, 9, 3), 1, 5),
            Playlist(2, "Rainy Sundays", listOf(6, 8, 2), 2, 4, coverAlbumId = 12),
            Playlist(3, "Fresh finds", emptyList(), 3, 3),
        )
    }
}
