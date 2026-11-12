package com.chillysloth.neomusic.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chillysloth.neomusic.data.AlbumSort
import com.chillysloth.neomusic.data.ArtistSort
import com.chillysloth.neomusic.data.Library
import com.chillysloth.neomusic.data.Playlist
import com.chillysloth.neomusic.data.PlaylistSort
import com.chillysloth.neomusic.data.ThemeMode
import com.chillysloth.neomusic.data.TrackSort
import com.chillysloth.neomusic.data.formatLongDuration
import com.chillysloth.neomusic.data.pluralAlbums
import com.chillysloth.neomusic.data.pluralTracks
import com.chillysloth.neomusic.data.sortedTracks
import com.chillysloth.neomusic.data.textComparator
import com.chillysloth.neomusic.ui.LocalActions
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.LocalBottomInset
import com.chillysloth.neomusic.ui.components.Artwork
import com.chillysloth.neomusic.ui.components.EmptyState
import com.chillysloth.neomusic.ui.components.ListActionsHeader
import com.chillysloth.neomusic.ui.components.NeoBox
import com.chillysloth.neomusic.ui.components.NeoButton
import com.chillysloth.neomusic.ui.components.NeoChip
import com.chillysloth.neomusic.ui.components.NeoIconButton
import com.chillysloth.neomusic.ui.components.NeoMenu
import com.chillysloth.neomusic.ui.components.NeoMenuItem
import com.chillysloth.neomusic.ui.components.PlainIconButton
import com.chillysloth.neomusic.ui.components.PlaylistCover
import com.chillysloth.neomusic.ui.components.SortButton
import com.chillysloth.neomusic.ui.components.TrackRow
import com.chillysloth.neomusic.ui.theme.Neo
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private enum class HomeTab(val label: String) { TRACKS("Tracks"), PLAYLISTS("Playlists"), ALBUMS("Albums"), ARTISTS("Artists") }

@Composable
fun HomeScreen(initialTab: Int = 0) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val library by app.repository.library.collectAsStateWithLifecycle()
    val tabs = HomeTab.entries
    val pager = rememberPagerState(initialPage = initialTab) { tabs.size } // saveable across navigation
    val scope = rememberCoroutineScope()

    // One UI style: a big title that collapses as you scroll, with search + tabs pinned.
    val density = LocalDensity.current
    val headerMax = 104.dp
    val headerMaxPx = with(density) { headerMax.toPx() }
    var headerOffset by remember { mutableFloatStateOf(0f) }
    val connection = remember(headerMaxPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val next = (headerOffset + available.y).coerceIn(-headerMaxPx, 0f)
                val used = next - headerOffset
                headerOffset = next
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val next = (headerOffset + available.y).coerceIn(-headerMaxPx, 0f)
                val used = next - headerOffset
                headerOffset = next
                return Offset(0f, used)
            }
        }
    }
    val collapse = -headerOffset / headerMaxPx

    Column(
        Modifier
            .fillMaxSize()
            .background(Neo.colors.paper)
            .statusBarsPadding()
            .nestedScroll(connection)
    ) {
        // Collapsing title block
        Box(
            Modifier
                .fillMaxWidth()
                .height(with(density) { (headerMaxPx + headerOffset).toDp() })
                .clip(RoundedCornerShape(0.dp))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 16.dp, bottom = 12.dp)
                    .graphicsLayer { alpha = (1f - collapse * 1.6f).coerceIn(0f, 1f) },
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("MUSIC", style = Neo.type.hero, color = Neo.colors.text)
                    Text(
                        if (library.loaded) "${pluralTracks(library.tracks.size)} · ${pluralAlbums(library.albums.size)}" else "Scanning library…",
                        style = Neo.type.label, color = Neo.colors.textMuted,
                    )
                }
                Sticker()
            }
        }
        // Pinned: search + overflow
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoBox(
                Modifier.weight(1f).height(50.dp),
                color = Neo.colors.surface,
                shape = RoundedCornerShape(Neo.dimens.radius),
                shadow = Neo.dimens.shadow,
                onClick = actions::openSearch,
                onClickLabel = "Search",
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Neo.colors.text)
                    Spacer(Modifier.width(10.dp))
                    Text("Search tracks, albums, artists", style = Neo.type.body, color = Neo.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(12.dp))
            HomeMenu()
        }
        // Tabs
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val tabColors = listOf(Neo.colors.yellow, Neo.colors.pink, Neo.colors.green, Neo.colors.blue)
            tabs.forEachIndexed { i, tab ->
                NeoChip(
                    tab.label,
                    selected = pager.currentPage == i,
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    selectedColor = tabColors[i],
                    modifier = Modifier.padding(end = 3.dp, bottom = 3.dp),
                )
            }
        }
        HorizontalDivider(thickness = Neo.dimens.border, color = Neo.colors.ink)

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { tabs[it].name },
        ) { page ->
            when {
                !library.loaded -> Box(Modifier.fillMaxSize())
                else -> when (tabs[page]) {
                    HomeTab.TRACKS -> TracksTab(library)
                    HomeTab.PLAYLISTS -> PlaylistsTab(library)
                    HomeTab.ALBUMS -> AlbumsTab(library)
                    HomeTab.ARTISTS -> ArtistsTab(library)
                }
            }
        }
    }
}

@Composable
private fun Sticker() {
    NeoBox(
        Modifier
            .padding(bottom = 6.dp)
            .graphicsLayer { rotationZ = 8f },
        color = Neo.colors.pink,
        shape = RoundedCornerShape(50),
        shadow = Neo.dimens.shadowSmall,
    ) {
        Text("PLAY IT LOUD", style = Neo.type.caption.copy(fontFamily = com.chillysloth.neomusic.ui.theme.Display), color = Neo.colors.onAccent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun HomeMenu() {
    val app = LocalApp.current
    val theme by app.prefs.theme.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    Box {
        NeoIconButton(Icons.Rounded.MoreVert, "Menu", { open = true }, size = 50.dp, shape = RoundedCornerShape(Neo.dimens.radius), shadow = Neo.dimens.shadow)
        NeoMenu(open, { open = false }) {
            Text("THEME", style = Neo.type.caption, color = Neo.colors.textMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            ThemeMode.entries.forEach { mode ->
                val icon = when (mode) {
                    ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                    ThemeMode.LIGHT -> Icons.Rounded.LightMode
                    ThemeMode.DARK -> Icons.Rounded.DarkMode
                }
                NeoMenuItem(
                    mode.label, icon, { app.prefs.setTheme(mode); open = false },
                    trailing = if (mode == theme) ({ Icon(Icons.Rounded.Check, null, tint = Neo.colors.text) }) else null,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 2.dp, color = Neo.colors.ink)
            NeoMenuItem("Rescan library", Icons.Rounded.Refresh, {
                open = false
                scope.launch { app.repository.reload() }
            })
        }
    }
}

@Composable
private fun bottomPadding() = PaddingValues(bottom = LocalBottomInset.current + 16.dp)

// ---------------------------------------------------------------- Tracks

@Composable
private fun TracksTab(library: Library) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val sort by app.prefs.trackSort.flow.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val sorted = remember(library.tracks, sort) { library.tracks.sortedTracks(sort) }

    if (sorted.isEmpty()) {
        EmptyState(
            "No music yet", "Copy some audio files to your phone and they'll show up here.",
            Icons.Rounded.LibraryMusic,
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding()) {
        item(key = "header") {
            ListActionsHeader(
                pluralTracks(sorted.size),
                onShuffle = { actions.play(sorted, shuffle = true) },
                onPlay = { actions.play(sorted, shuffle = false) },
                sort = {
                    SortButton(sort, TrackSort.entries, { it.label }, app.prefs.trackSort::set)
                },
            )
        }
        itemsIndexed(sorted, key = { _, t -> t.id }) { i, t ->
            TrackRow(
                t,
                isCurrent = t.id == player.currentTrackId,
                isPlaying = player.isPlaying,
                onClick = { actions.play(sorted, i) },
            )
        }
    }
}

// ---------------------------------------------------------------- Playlists

@Composable
private fun PlaylistsTab(library: Library) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val all by app.playlists.playlists.collectAsStateWithLifecycle()
    val sort by app.prefs.playlistSort.flow.collectAsStateWithLifecycle()
    val sorted = remember(all, sort) {
        val c: Comparator<Playlist>? = when (sort.key) {
            PlaylistSort.CUSTOM -> null
            PlaylistSort.NAME -> compareBy(textComparator) { it.name }
            PlaylistSort.CREATED -> compareBy { it.createdAt }
            PlaylistSort.MODIFIED -> compareBy { it.modifiedAt }
            PlaylistSort.TRACK_COUNT -> compareBy { it.trackIds.size }
        }
        val base = if (c == null) all else all.sortedWith(c)
        if (sort.descending) base.reversed() else base
    }
    val canReorder = sort.key == PlaylistSort.CUSTOM && !sort.descending

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = all.indexOfFirst { it.id == from.key }
        val toIdx = all.indexOfFirst { it.id == to.key }
        if (fromIdx >= 0 && toIdx >= 0) app.playlists.movePlaylist(fromIdx, toIdx)
    }

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = bottomPadding()) {
        item(key = "header") {
            ListActionsHeader(
                if (all.size == 1) "1 playlist" else "${all.size} playlists",
                onShuffle = null,
                onPlay = null,
                leading = {
                    NeoButton(
                        "New", { actions.createPlaylist(openAfter = true) },
                        icon = Icons.Rounded.Add, color = Neo.colors.yellow,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    )
                },
                sort = { SortButton(sort, PlaylistSort.entries, { it.label }, app.prefs.playlistSort::set) },
            )
            if (canReorder && all.size > 1) {
                Text(
                    "Custom order — hold the ≡ handle and drag to rearrange.",
                    style = Neo.type.caption, color = Neo.colors.textMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
        }
        if (all.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    "No playlists", "Make one here, or use ⋮ → Add to playlist on any track.",
                    Icons.AutoMirrored.Rounded.QueueMusic, color = Neo.colors.pink,
                )
            }
        }
        items(sorted, key = { it.id }) { p ->
            ReorderableItem(reorderState, key = p.id, enabled = canReorder) { dragging ->
                val tracks = p.trackIds.mapNotNull { library.trackById[it] }
                PlaylistRow(
                    p, tracks.map { it.albumId }, tracks.sumOf { it.durationMs }, dragging,
                    onClick = { actions.openPlaylist(p.id) },
                    onPlay = { actions.play(tracks, shuffle = false) },
                    onShuffle = { actions.play(tracks, shuffle = true) },
                    handle = if (canReorder) ({
                        Box(
                            Modifier
                                .draggableHandle()
                                .size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.DragHandle, "Drag to reorder", tint = Neo.colors.text) }
                    }) else null,
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    p: Playlist,
    albumIds: List<Long>,
    durationMs: Long,
    dragging: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    handle: (@Composable () -> Unit)?,
) {
    val actions = LocalActions.current
    NeoBox(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 18.dp, top = 6.dp, bottom = 8.dp),
        color = if (dragging) Neo.colors.yellow else Neo.colors.surface,
        shadow = if (dragging) 8.dp else Neo.dimens.shadowSmall,
        onClick = onClick,
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            NeoBox(Modifier.size(60.dp), shape = RoundedCornerShape(Neo.dimens.radiusSmall), shadow = 0.dp, borderWidth = 2.dp) {
                PlaylistCover(albumIds, p.id, Modifier.size(60.dp), sizeHint = 60.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = Neo.type.h3, color = if (dragging) Neo.colors.onAccent else Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(
                    pluralTracks(p.trackIds.size) + if (durationMs > 0) " · ${formatLongDuration(durationMs)}" else "",
                    style = Neo.type.caption, color = if (dragging) Neo.colors.onAccent else Neo.colors.textMuted,
                )
            }
            var open by remember { mutableStateOf(false) }
            Box {
                PlainIconButton(Icons.Rounded.MoreVert, "Playlist options", { open = true }, tint = if (dragging) Neo.colors.onAccent else Neo.colors.text)
                NeoMenu(open, { open = false }) {
                    NeoMenuItem("Play", Icons.Rounded.PlayArrow, { open = false; onPlay() })
                    NeoMenuItem("Shuffle", Icons.Rounded.Shuffle, { open = false; onShuffle() })
                    NeoMenuItem("Rename", Icons.Rounded.Edit, { open = false; actions.renamePlaylist(p.id) })
                    NeoMenuItem("Delete playlist", Icons.Rounded.DeleteOutline, { open = false; actions.deletePlaylist(p.id) })
                }
            }
            handle?.invoke()
        }
    }
}

// ---------------------------------------------------------------- Albums

@Composable
private fun AlbumsTab(library: Library) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val sort by app.prefs.albumSort.flow.collectAsStateWithLifecycle()
    val sorted = remember(library.albums, sort) {
        val c: Comparator<com.chillysloth.neomusic.data.Album> = when (sort.key) {
            AlbumSort.TITLE -> compareBy(textComparator) { it.title }
            AlbumSort.ARTIST -> compareBy<com.chillysloth.neomusic.data.Album, String>(textComparator) { it.artist }.thenBy(textComparator) { it.title }
            AlbumSort.YEAR -> compareBy<com.chillysloth.neomusic.data.Album> { it.year }.thenBy(textComparator) { it.title }
            AlbumSort.TRACK_COUNT -> compareBy { it.tracks.size }
        }
        library.albums.sortedWith(if (sort.descending) c.reversed() else c)
    }
    if (sorted.isEmpty()) {
        EmptyState("No albums", "Albums appear once your music is scanned.", Icons.Rounded.Album, color = Neo.colors.green)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 18.dp, bottom = LocalBottomInset.current + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            ListActionsHeader(
                pluralAlbums(sorted.size),
                onShuffle = { actions.play(sorted.flatMap { it.tracks }, shuffle = true) },
                onPlay = { actions.play(sorted.flatMap { it.tracks }, shuffle = false) },
                sort = { SortButton(sort, AlbumSort.entries, { it.label }, app.prefs.albumSort::set) },
                modifier = Modifier.offset(x = (-14).dp),
            )
        }
        items(sorted, key = { it.id }) { album ->
            NeoBox(onClick = { actions.openAlbum(album.id) }, color = Neo.colors.surface) {
                Column {
                    Artwork(album.id, Modifier.fillMaxWidth().aspectRatio(1f), sizeHint = 180.dp)
                    HorizontalDivider(thickness = Neo.dimens.border, color = Neo.colors.ink)
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(album.title, style = Neo.type.title, color = Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(album.artist, style = Neo.type.caption, color = Neo.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Artists

@Composable
private fun ArtistsTab(library: Library) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val sort by app.prefs.artistSort.flow.collectAsStateWithLifecycle()
    val sorted = remember(library.artists, sort) {
        val c: Comparator<com.chillysloth.neomusic.data.Artist> = when (sort.key) {
            ArtistSort.NAME -> compareBy(textComparator) { it.name }
            ArtistSort.TRACK_COUNT -> compareBy { it.tracks.size }
            ArtistSort.ALBUM_COUNT -> compareBy { it.albums.size }
        }
        library.artists.sortedWith(if (sort.descending) c.reversed() else c)
    }
    if (sorted.isEmpty()) {
        EmptyState("No artists", "Artists appear once your music is scanned.", Icons.Rounded.Person, color = Neo.colors.blue)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding()) {
        item(key = "header") {
            ListActionsHeader(
                if (sorted.size == 1) "1 artist" else "${sorted.size} artists",
                onShuffle = { actions.play(sorted.flatMap { it.tracks }, shuffle = true) },
                onPlay = { actions.play(sorted.flatMap { it.tracks }, shuffle = false) },
                sort = { SortButton(sort, ArtistSort.entries, { it.label }, app.prefs.artistSort::set) },
            )
        }
        items(sorted, key = { it.id }) { artist ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(Neo.dimens.radius))
                    .clickable { actions.openArtist(artist.id) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeoBox(Modifier.size(56.dp), shape = CircleShape, shadow = Neo.dimens.shadowSmall, borderWidth = 2.dp) {
                    Artwork(artist.coverAlbumId, Modifier.size(56.dp), sizeHint = 56.dp)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(artist.name, style = Neo.type.title, color = Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${pluralAlbums(artist.albums.size)} · ${pluralTracks(artist.tracks.size)}",
                        style = Neo.type.caption, color = Neo.colors.textMuted,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Neo.colors.text)
            }
        }
    }
}

