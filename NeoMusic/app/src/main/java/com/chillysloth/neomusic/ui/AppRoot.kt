package com.chillysloth.neomusic.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chillysloth.neomusic.NeoMusicApp
import com.chillysloth.neomusic.data.MusicRepository
import com.chillysloth.neomusic.data.Track
import com.chillysloth.neomusic.ui.components.AddToPlaylistDialog
import com.chillysloth.neomusic.ui.components.ConfirmDialog
import com.chillysloth.neomusic.ui.components.MessageBanner
import com.chillysloth.neomusic.ui.components.PlaylistCoverDialog
import com.chillysloth.neomusic.ui.components.TextInputDialog
import com.chillysloth.neomusic.ui.player.MiniPlayer
import com.chillysloth.neomusic.ui.player.MiniPlayerHeight
import com.chillysloth.neomusic.ui.player.NowPlayingScreen
import com.chillysloth.neomusic.ui.screens.AlbumScreen
import com.chillysloth.neomusic.ui.screens.ArtistScreen
import com.chillysloth.neomusic.ui.screens.HomeScreen
import com.chillysloth.neomusic.ui.screens.PlaylistScreen
import com.chillysloth.neomusic.ui.screens.SearchScreen
import com.chillysloth.neomusic.ui.screens.TrackDetailsScreen
import com.chillysloth.neomusic.ui.screens.TrackPickerScreen
import com.chillysloth.neomusic.ui.theme.Neo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalApp = staticCompositionLocalOf<NeoMusicApp> { error("App not provided") }

/** Space screens should leave at the bottom so the mini player never covers content. */
val LocalBottomInset = staticCompositionLocalOf { 0.dp }

private sealed interface DialogState {
    data class AddToPlaylist(val tracks: List<Track>) : DialogState
    data class CreatePlaylist(val tracks: List<Track>, val openAfter: Boolean) : DialogState
    data class RenamePlaylist(val id: Long, val current: String) : DialogState
    data class DeletePlaylist(val id: Long, val name: String) : DialogState
    data class DeleteTracks(val tracks: List<Track>) : DialogState
    data class PlaylistCover(val id: Long) : DialogState
}

@Composable
fun AppRoot(app: NeoMusicApp, startDestination: String = "home") {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val library by app.repository.library.collectAsStateWithLifecycle()
    val playerState by app.player.state.collectAsStateWithLifecycle()
    val currentTrack = playerState.currentTrackId?.let { library.trackById[it] }

    var dialog by remember { mutableStateOf<DialogState?>(null) }
    val messages = remember { MessageState(scope) }

    // ---- Player sheet: 0 = collapsed (mini player), 1 = expanded (now playing)
    var sheet by remember { mutableFloatStateOf(0f) }
    var sheetJob by remember { mutableStateOf<Job?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    fun settleSheet(target: Float, velocity: Float = 0f, spec: AnimationSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 500f)) {
        sheetJob?.cancel()
        sheetJob = scope.launch {
            animate(sheet, target, velocity, spec) { v, _ -> sheet = v }
            if (target == 0f) showQueue = false
        }
    }
    LaunchedEffect(currentTrack == null) { if (currentTrack == null) settleSheet(0f) }

    // ---- Deleting files (needs a system confirmation on Android 10+)
    var pendingDelete by remember { mutableStateOf<List<Track>>(emptyList()) }
    fun afterDeleted(tracks: List<Track>) {
        val ids = tracks.map { it.id }.toSet()
        app.player.removeTracks(ids)
        app.playlists.purgeTracks(ids)
        scope.launch { app.repository.reload() }
        messages.show(if (tracks.size == 1) "Deleted \"${tracks[0].title}\"" else "Deleted ${tracks.size} tracks")
    }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val tracks = pendingDelete
        pendingDelete = emptyList()
        if (result.resultCode == Activity.RESULT_OK && tracks.isNotEmpty()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) afterDeleted(tracks)
            else scope.launch {
                // Android 10: permission granted for this item, now delete it for real.
                if (app.repository.deleteTracks(tracks) is MusicRepository.DeleteResult.Done) afterDeleted(tracks)
            }
        }
    }
    fun performDelete(tracks: List<Track>) = scope.launch {
        when (val r = app.repository.deleteTracks(tracks)) {
            MusicRepository.DeleteResult.Done -> afterDeleted(tracks)
            is MusicRepository.DeleteResult.NeedsConfirmation -> {
                pendingDelete = tracks
                deleteLauncher.launch(IntentSenderRequest.Builder(r.intentSender).build())
            }
            is MusicRepository.DeleteResult.Failed -> {
                messages.show("Couldn't delete: ${r.message}")
            }
        }
    }

    // ---- Custom playlist covers from the system photo picker (no storage permission needed)
    var pendingCoverId by remember { mutableStateOf<Long?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = pendingCoverId
        pendingCoverId = null
        if (uri != null && id != null) scope.launch {
            if (app.playlists.importCoverImage(id, uri)) messages.show("Cover updated")
            else messages.show("Couldn't read that image")
        }
    }

    val actions = remember {
        object : AppActions {
            private fun go(route: String) {
                if (sheet > 0f) settleSheet(0f)
                nav.navigate(route) { launchSingleTop = true }
            }

            override fun play(tracks: List<Track>, startIndex: Int, shuffle: Boolean?) =
                app.player.play(tracks, startIndex, shuffle)

            override fun openAlbum(id: Long) = go("album/$id")
            override fun openArtist(id: Long) = go("artist/$id")
            override fun openPlaylist(id: Long) = go("playlist/$id")
            override fun openTrackDetails(id: Long) = go("track/$id")
            override fun openSearch() = go("search")
            override fun openTrackPicker(playlistId: Long) = go("pick/$playlistId")
            override fun back() {
                nav.popBackStack()
            }

            override fun addToPlaylist(tracks: List<Track>) {
                dialog = DialogState.AddToPlaylist(tracks)
            }

            override fun delete(tracks: List<Track>) {
                // Android 11+ shows its own system confirmation, so skip ours to avoid asking twice.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) performDelete(tracks)
                else dialog = DialogState.DeleteTracks(tracks)
            }

            override fun createPlaylist(initialTracks: List<Track>, openAfter: Boolean) {
                dialog = DialogState.CreatePlaylist(initialTracks, openAfter)
            }

            override fun renamePlaylist(id: Long) {
                val p = app.playlists.playlists.value.firstOrNull { it.id == id } ?: return
                dialog = DialogState.RenamePlaylist(id, p.name)
            }

            override fun deletePlaylist(id: Long) {
                val p = app.playlists.playlists.value.firstOrNull { it.id == id } ?: return
                dialog = DialogState.DeletePlaylist(id, p.name)
            }

            override fun changePlaylistCover(id: Long) {
                dialog = DialogState.PlaylistCover(id)
            }

            override fun toast(message: String) = messages.show(message)
        }
    }

    BackHandler(enabled = sheet > 0.5f) {
        if (showQueue) showQueue = false else settleSheet(0f)
    }

    // The track picker has its own bottom action bar, so the mini player steps aside there.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showMiniPlayer = currentTrack != null && route?.startsWith("pick/") != true
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset: Dp = navInset + if (showMiniPlayer) MiniPlayerHeight + 20.dp else 0.dp

    CompositionLocalProvider(LocalApp provides app, LocalActions provides actions, LocalBottomInset provides bottomInset) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Neo.colors.paper)) {
            val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(220)) },
                exitTransition = { slideOutHorizontally(tween(280)) { -it / 6 } + fadeOut(tween(200)) },
                popEnterTransition = { slideInHorizontally(tween(280)) { -it / 6 } + fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(280)) { it / 4 } + fadeOut(tween(200)) },
            ) {
                composable("home") { HomeScreen() }
                composable("search") { SearchScreen() }
                composable("album/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    AlbumScreen(it.arguments!!.getLong("id"))
                }
                composable("artist/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    ArtistScreen(it.arguments!!.getLong("id"))
                }
                composable("playlist/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    PlaylistScreen(it.arguments!!.getLong("id"))
                }
                composable("track/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    TrackDetailsScreen(it.arguments!!.getLong("id"))
                }
                composable("pick/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    TrackPickerScreen(it.arguments!!.getLong("id"))
                }
            }

            if (currentTrack != null) {
                val dragState = rememberDraggableState { delta ->
                    sheetJob?.cancel()
                    sheet = (sheet - delta / heightPx).coerceIn(0f, 1f)
                }
                val onDragStopped: suspend kotlinx.coroutines.CoroutineScope.(Float) -> Unit = { velocity ->
                    val target = when {
                        velocity < -900f -> 1f
                        velocity > 900f -> 0f
                        sheet > 0.5f -> 1f
                        else -> 0f
                    }
                    settleSheet(target, -velocity / heightPx)
                }

                // Mini player
                if (sheet < 1f && showMiniPlayer) {
                    MiniPlayer(
                        currentTrack,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 12.dp, end = 16.dp, bottom = 14.dp)
                            .graphicsLayer {
                                alpha = (1f - sheet * 2.5f).coerceIn(0f, 1f)
                                translationY = -sheet * heightPx * 0.25f
                            }
                            .draggable(dragState, Orientation.Vertical, onDragStopped = onDragStopped),
                        onExpand = { settleSheet(1f) },
                    )
                }

                // Full player slides up over everything
                if (sheet > 0f) {
                    NowPlayingScreen(
                        currentTrack,
                        Modifier
                            .zIndex(2f)
                            .graphicsLayer { translationY = (1f - sheet) * heightPx }
                            .draggable(dragState, Orientation.Vertical, onDragStopped = onDragStopped),
                        showQueue = showQueue,
                        onToggleQueue = { showQueue = !showQueue },
                        onMinimize = { settleSheet(0f) },
                    )
                }
            }

            AnimatedVisibility(
                visible = messages.text != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
                    .padding(bottom = bottomInset + 14.dp, start = 16.dp, end = 20.dp),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                var last by remember { mutableStateOf("") }
                messages.text?.let { last = it }
                MessageBanner(last)
            }
        }

        // ---- Dialogs
        when (val d = dialog) {
            is DialogState.AddToPlaylist -> {
                val playlists by app.playlists.playlists.collectAsStateWithLifecycle()
                AddToPlaylistDialog(
                    playlists, library, d.tracks.size,
                    onDismiss = { dialog = null },
                    onPick = { p ->
                        val n = app.playlists.addTracks(p.id, d.tracks.map { it.id })
                        dialog = null
                        actions.toast(
                            when {
                                n == 0 -> "Already in \"${p.name}\""
                                n == 1 -> "Added to \"${p.name}\""
                                else -> "Added $n tracks to \"${p.name}\""
                            }
                        )
                    },
                    onCreateNew = { dialog = DialogState.CreatePlaylist(d.tracks, openAfter = false) },
                )
            }
            is DialogState.CreatePlaylist -> TextInputDialog(
                title = "New playlist",
                initial = "",
                confirmLabel = "Create",
                placeholder = "Playlist name",
                onDismiss = { dialog = null },
                onConfirm = { name ->
                    val id = app.playlists.create(name, d.tracks.map { it.id })
                    dialog = null
                    if (d.tracks.isNotEmpty()) actions.toast("Added to \"$name\"")
                    if (d.openAfter) actions.openPlaylist(id)
                },
            )
            is DialogState.RenamePlaylist -> TextInputDialog(
                title = "Rename playlist",
                initial = d.current,
                confirmLabel = "Save",
                placeholder = "Playlist name",
                onDismiss = { dialog = null },
                onConfirm = { name -> app.playlists.rename(d.id, name); dialog = null },
            )
            is DialogState.DeletePlaylist -> ConfirmDialog(
                title = "Delete playlist?",
                message = "\"${d.name}\" will be removed. Your music files stay on the device.",
                confirmLabel = "Delete",
                onDismiss = { dialog = null },
                onConfirm = {
                    app.playlists.delete(d.id); dialog = null
                    actions.toast("Playlist deleted")
                },
            )
            is DialogState.DeleteTracks -> ConfirmDialog(
                title = if (d.tracks.size == 1) "Delete track?" else "Delete ${d.tracks.size} tracks?",
                message = if (d.tracks.size == 1)
                    "\"${d.tracks[0].title}\" will be permanently deleted from this device."
                else "These files will be permanently deleted from this device.",
                confirmLabel = "Delete",
                onDismiss = { dialog = null },
                onConfirm = { dialog = null; performDelete(d.tracks) },
            )
            is DialogState.PlaylistCover -> {
                val playlists by app.playlists.playlists.collectAsStateWithLifecycle()
                val p = playlists.firstOrNull { it.id == d.id }
                if (p == null) LaunchedEffect(Unit) { dialog = null } // playlist was deleted
                else {
                    val albums = remember(p.trackIds, library) {
                        p.trackIds.mapNotNull { library.trackById[it]?.albumId }.distinct()
                            .mapNotNull { library.albumById[it] }
                    }
                    PlaylistCoverDialog(
                        p, albums,
                        onDismiss = { dialog = null },
                        onGallery = {
                            dialog = null
                            pendingCoverId = p.id
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onAlbum = { albumId -> app.playlists.setCoverAlbum(p.id, albumId); dialog = null },
                        onAutomatic = { app.playlists.resetCover(p.id); dialog = null },
                    )
                }
            }
            null -> Unit
        }
    }
}

/** Short-lived neubrutal toast. */
private class MessageState(private val scope: kotlinx.coroutines.CoroutineScope) {
    var text by mutableStateOf<String?>(null)
        private set
    private var job: Job? = null

    fun show(message: String) {
        job?.cancel()
        text = message
        job = scope.launch {
            delay(2200)
            text = null
        }
    }
}
