package com.chillysloth.neomusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chillysloth.neomusic.data.PlaylistTrackSort
import com.chillysloth.neomusic.data.formatLongDuration
import com.chillysloth.neomusic.data.pluralAlbums
import com.chillysloth.neomusic.data.pluralTracks
import com.chillysloth.neomusic.data.sortedPlaylistTracks
import com.chillysloth.neomusic.ui.LocalActions
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.LocalBottomInset
import com.chillysloth.neomusic.ui.components.Artwork
import com.chillysloth.neomusic.ui.components.EmptyState
import com.chillysloth.neomusic.ui.components.NeoBox
import com.chillysloth.neomusic.ui.components.NeoButton
import com.chillysloth.neomusic.ui.components.NeoIconButton
import com.chillysloth.neomusic.ui.components.NeoMenu
import com.chillysloth.neomusic.ui.components.NeoMenuItem
import com.chillysloth.neomusic.ui.components.NeoTag
import com.chillysloth.neomusic.ui.components.PlaylistCover
import com.chillysloth.neomusic.ui.components.ScrollbarBox
import com.chillysloth.neomusic.ui.components.SectionTitle
import com.chillysloth.neomusic.ui.components.SortButton
import com.chillysloth.neomusic.ui.components.TrackRow
import com.chillysloth.neomusic.ui.components.removeFromPlaylistItem
import com.chillysloth.neomusic.ui.theme.Neo
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Shared page frame: back button, a title that fades in once the hero scrolls away, and actions. */
@Composable
fun DetailScaffold(
    title: String,
    listState: LazyListState,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val app = LocalActions.current
    val showTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Column(Modifier.fillMaxSize().background(Neo.colors.paper).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", app::back)
            Spacer(Modifier.width(14.dp))
            Text(
                title, style = Neo.type.h3, color = Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = if (showTitle) 1f else 0f },
            )
            actions?.invoke()
        }
        ScrollbarBox(listState, Modifier.weight(1f)) { content() }
    }
}

/** Big cover + title + Play / Shuffle buttons. */
@Composable
fun CollectionHero(
    tag: String,
    tagColor: Color,
    title: String,
    subtitle: String,
    coverShape: Shape,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    enabled: Boolean,
    cover: @Composable (Modifier) -> Unit,
    extra: (@Composable () -> Unit)? = null,
    onEditCover: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Box {
            NeoBox(
                Modifier.size(232.dp), shape = coverShape, shadow = 8.dp, borderWidth = 3.dp,
                onClick = onEditCover, onClickLabel = onEditCover?.let { "Change cover" },
            ) {
                cover(Modifier.fillMaxSize())
            }
            if (onEditCover != null) NeoIconButton(
                Icons.Rounded.AddPhotoAlternate, "Change cover", onEditCover,
                Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer { translationX = 14.dp.toPx(); translationY = 14.dp.toPx() },
                color = Neo.colors.yellow,
            )
            NeoTag(
                tag, tagColor,
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 0.dp)
                    .graphicsLayer { rotationZ = 10f; translationX = 22.dp.toPx(); translationY = (-8).dp.toPx() },
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = Neo.type.h1, color = Neo.colors.text, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = Neo.type.label, color = Neo.colors.textMuted, textAlign = TextAlign.Center)
        extra?.invoke()
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NeoButton(
                "Play", { if (enabled) onPlay() }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow,
                color = if (enabled) Neo.colors.yellow else Neo.colors.surfaceAlt,
                shape = RoundedCornerShape(Neo.dimens.radius), style = Neo.type.h3,
                contentPadding = PaddingValues(vertical = 14.dp),
            )
            NeoButton(
                "Shuffle", { if (enabled) onShuffle() }, Modifier.weight(1f), icon = Icons.Rounded.Shuffle,
                color = if (enabled) Neo.colors.pink else Neo.colors.surfaceAlt,
                shape = RoundedCornerShape(Neo.dimens.radius), style = Neo.type.h3,
                contentPadding = PaddingValues(vertical = 14.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

private fun LazyListScope.bottomSpacer(height: androidx.compose.ui.unit.Dp) {
    item(key = "bottom-space") { Spacer(Modifier.height(height)) }
}

// ---------------------------------------------------------------- Album

@Composable
fun AlbumScreen(albumId: Long) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val library by app.repository.library.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val album = library.albumById[albumId]
    val listState = rememberLazyListState()
    if (album == null) {
        MissingPage(listState, "Album not found"); return
    }
    val tracks = album.tracks
    val artistId = tracks.firstOrNull()?.artistId
    val multiDisc = tracks.any { it.trackNumber >= 2000 }

    val bottomInset = LocalBottomInset.current
    DetailScaffold(album.title, listState) {
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            item(key = "hero") {
                CollectionHero(
                    tag = "ALBUM", tagColor = Neo.colors.green,
                    title = album.title,
                    subtitle = listOfNotNull(
                        album.year.takeIf { it > 0 }?.toString(),
                        pluralTracks(tracks.size),
                        formatLongDuration(album.durationMs),
                    ).joinToString(" · "),
                    coverShape = RoundedCornerShape(Neo.dimens.radius),
                    onPlay = { actions.play(tracks, shuffle = false) },
                    onShuffle = { actions.play(tracks, shuffle = true) },
                    enabled = tracks.isNotEmpty(),
                    cover = { Artwork(album.id, it, sizeHint = 300.dp) },
                    extra = {
                        Spacer(Modifier.height(12.dp))
                        NeoButton(
                            album.artist, { artistId?.let(actions::openArtist) },
                            icon = Icons.Rounded.Person, color = Neo.colors.surface, textColor = Neo.colors.text,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        )
                    },
                )
            }
            itemsIndexed(tracks, key = { _, t -> t.id }) { i, t ->
                val num = t.trackNumber % 1000
                TrackRow(
                    t,
                    isCurrent = t.id == player.currentTrackId,
                    isPlaying = player.isPlaying,
                    onClick = { actions.play(tracks, i) },
                    leadingNumber = if (num > 0) num else i + 1,
                    showArt = false,
                    subtitle = if (multiDisc) "Disc ${t.trackNumber / 1000} · ${t.artist}" else t.artist,
                )
            }
            bottomSpacer(bottomInset + 16.dp)
        }
    }
}

// ---------------------------------------------------------------- Artist

@Composable
fun ArtistScreen(artistId: Long) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val library by app.repository.library.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val artist = library.artistById[artistId]
    val listState = rememberLazyListState()
    if (artist == null) {
        MissingPage(listState, "Artist not found"); return
    }
    val tracks = artist.tracks

    val bottomInset = LocalBottomInset.current
    DetailScaffold(artist.name, listState) {
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            item(key = "hero") {
                CollectionHero(
                    tag = "ARTIST", tagColor = Neo.colors.blue,
                    title = artist.name,
                    subtitle = "${pluralAlbums(artist.albums.size)} · ${pluralTracks(tracks.size)}",
                    coverShape = CircleShape,
                    onPlay = { actions.play(tracks, shuffle = false) },
                    onShuffle = { actions.play(tracks, shuffle = true) },
                    enabled = tracks.isNotEmpty(),
                    cover = { Artwork(artist.coverAlbumId, it, sizeHint = 300.dp) },
                )
            }
            if (artist.albums.isNotEmpty()) {
                item(key = "albums-title") { SectionTitle("Albums") }
                item(key = "albums") {
                    LazyRow(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(artist.albums, key = { it.id }) { album ->
                            NeoBox(Modifier.width(148.dp), onClick = { actions.openAlbum(album.id) }) {
                                Column {
                                    Artwork(album.id, Modifier.fillMaxWidth().aspectRatio(1f), sizeHint = 150.dp)
                                    HorizontalDivider(thickness = Neo.dimens.border, color = Neo.colors.ink)
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text(album.title, style = Neo.type.label, color = Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            listOfNotNull(album.year.takeIf { it > 0 }?.toString(), pluralTracks(album.tracks.size)).joinToString(" · "),
                                            style = Neo.type.caption, color = Neo.colors.textMuted, maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "tracks-title") { SectionTitle("Tracks") }
            itemsIndexed(tracks, key = { _, t -> t.id }) { i, t ->
                TrackRow(
                    t,
                    isCurrent = t.id == player.currentTrackId,
                    isPlaying = player.isPlaying,
                    onClick = { actions.play(tracks, i) },
                    subtitle = t.album,
                )
            }
            bottomSpacer(bottomInset + 16.dp)
        }
    }
}

// ---------------------------------------------------------------- Playlist

@Composable
fun PlaylistScreen(playlistId: Long) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val library by app.repository.library.collectAsStateWithLifecycle()
    val playlists by app.playlists.playlists.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    val listState = rememberLazyListState()

    if (playlist == null) {
        // Deleted while open.
        LaunchedEffect(Unit) { actions.back() }
        return
    }
    var sort by remember(playlistId) { mutableStateOf(app.prefs.playlistTrackSort(playlistId)) }
    val tracks = remember(playlist.trackIds, library) { playlist.trackIds.mapNotNull { library.trackById[it] } }
    val sorted = remember(tracks, sort) { tracks.sortedPlaylistTracks(sort) }
    val canReorder = sort.key == PlaylistTrackSort.CUSTOM && !sort.descending

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val ids = playlist.trackIds.toMutableList()
        val f = ids.indexOf(from.key as? Long ?: return@rememberReorderableLazyListState)
        val t = ids.indexOf(to.key as? Long ?: return@rememberReorderableLazyListState)
        if (f >= 0 && t >= 0) {
            ids.add(t, ids.removeAt(f))
            app.playlists.setTrackOrder(playlistId, ids)
        }
    }

    val bottomInset = LocalBottomInset.current
    DetailScaffold(playlist.name, listState, actions = {
        var open by remember { mutableStateOf(false) }
        Box {
            NeoIconButton(Icons.Rounded.MoreVert, "Playlist options", { open = true })
            NeoMenu(open, { open = false }) {
                NeoMenuItem("Add tracks", Icons.AutoMirrored.Rounded.PlaylistAdd, { open = false; actions.openTrackPicker(playlistId) })
                NeoMenuItem("Rename", Icons.Rounded.Edit, { open = false; actions.renamePlaylist(playlistId) })
                NeoMenuItem("Change cover", Icons.Rounded.AddPhotoAlternate, { open = false; actions.changePlaylistCover(playlistId) })
                NeoMenuItem("Delete playlist", Icons.Rounded.DeleteOutline, { open = false; actions.deletePlaylist(playlistId) })
            }
        }
    }) {
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            item(key = "hero") {
                CollectionHero(
                    tag = "PLAYLIST", tagColor = Neo.colors.pink,
                    title = playlist.name,
                    subtitle = pluralTracks(tracks.size) + if (tracks.isNotEmpty()) " · ${formatLongDuration(tracks.sumOf { it.durationMs })}" else "",
                    coverShape = RoundedCornerShape(Neo.dimens.radius),
                    onPlay = { actions.play(sorted, shuffle = false) },
                    onShuffle = { actions.play(sorted, shuffle = true) },
                    enabled = tracks.isNotEmpty(),
                    cover = { PlaylistCover(playlist, tracks.map { t -> t.albumId }, it, sizeHint = 300.dp) },
                    onEditCover = { actions.changePlaylistCover(playlistId) },
                )
            }
            item(key = "controls") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeoButton(
                        "Add", { actions.openTrackPicker(playlistId) }, icon = Icons.Rounded.Add,
                        color = Neo.colors.green,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    SortButton(sort, PlaylistTrackSort.entries, { it.label }) {
                        sort = it; app.prefs.setPlaylistTrackSort(playlistId, it)
                    }
                }
                if (canReorder && tracks.size > 1) {
                    Text(
                        "Custom order — hold ≡ and drag to rearrange.",
                        style = Neo.type.caption, color = Neo.colors.textMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }
            }
            if (tracks.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        "Empty playlist", "Add tracks here, or from any list with ⋮ → Add to playlist.",
                        Icons.AutoMirrored.Rounded.QueueMusic, color = Neo.colors.pink,
                    )
                }
            }
            itemsIndexed(sorted, key = { _, t -> t.id }) { i, t ->
                ReorderableItem(reorderState, key = t.id, enabled = canReorder) { dragging ->
                    Box(
                        Modifier.graphicsLayer {
                            if (dragging) { scaleX = 1.02f; scaleY = 1.02f }
                        }.background(if (dragging) Neo.colors.paper else Color.Transparent)
                    ) {
                        TrackRow(
                            t,
                            isCurrent = t.id == player.currentTrackId,
                            isPlaying = player.isPlaying,
                            highlight = dragging,
                            onClick = { actions.play(sorted, i) },
                            extraMenuItems = listOf(removeFromPlaylistItem { app.playlists.removeTrack(playlistId, t.id) }),
                            trailing = if (canReorder) ({
                                Box(Modifier.draggableHandle().size(36.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.DragHandle, "Drag to reorder", tint = Neo.colors.text)
                                }
                            }) else null,
                        )
                    }
                }
            }
            bottomSpacer(bottomInset + 16.dp)
        }
    }
}

@Composable
private fun MissingPage(listState: LazyListState, text: String) {
    DetailScaffold("", listState) {
        EmptyState(text, "It may have been deleted from this device.", Icons.AutoMirrored.Rounded.QueueMusic)
    }
}

