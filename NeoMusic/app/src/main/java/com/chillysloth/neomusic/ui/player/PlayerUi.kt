package com.chillysloth.neomusic.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.chillysloth.neomusic.data.Track
import com.chillysloth.neomusic.data.formatDuration
import com.chillysloth.neomusic.ui.LocalActions
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.components.Artwork
import com.chillysloth.neomusic.ui.components.EqualizerBars
import com.chillysloth.neomusic.ui.components.NeoBox
import com.chillysloth.neomusic.ui.components.NeoIconButton
import com.chillysloth.neomusic.ui.components.NeoTag
import com.chillysloth.neomusic.ui.components.PlainIconButton
import com.chillysloth.neomusic.ui.components.ScrollbarBox
import com.chillysloth.neomusic.ui.components.TrackMenu
import com.chillysloth.neomusic.ui.theme.Neo
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

val MiniPlayerHeight = 72.dp

/** Collapsed player bar. Tap or swipe up to expand, swipe sideways to change track. */
@Composable
fun MiniPlayer(track: Track, modifier: Modifier = Modifier, onExpand: () -> Unit) {
    val app = LocalApp.current
    val state by app.player.state.collectAsStateWithLifecycle()
    val position by app.player.position.collectAsStateWithLifecycle()
    val frac = if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f

    var dragX by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 80.dp.toPx() }

    NeoBox(
        modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragX < -threshold) app.player.next() else if (dragX > threshold) app.player.previous()
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                ) { _, dx -> dragX += dx }
            },
        color = Neo.colors.surface,
        shape = RoundedCornerShape(Neo.dimens.radius + 4.dp),
        shadow = Neo.dimens.shadow + 1.dp,
        onClick = onExpand,
        onClickLabel = "Open player",
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 8.dp)
                .offset { IntOffset((dragX * 0.35f).roundToInt(), 0) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoBox(Modifier.size(50.dp), shape = RoundedCornerShape(Neo.dimens.radiusSmall), shadow = 0.dp, borderWidth = 2.dp) {
                Artwork(track.albumId, Modifier.fillMaxSize(), sizeHint = 50.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = Neo.type.title, color = Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = Neo.type.caption, color = Neo.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            NeoIconButton(
                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (state.isPlaying) "Pause" else "Play",
                app.player::togglePlay,
                color = Neo.colors.yellow, size = 44.dp, iconSize = 26.dp,
            )
            Spacer(Modifier.width(4.dp))
            PlainIconButton(Icons.Rounded.SkipNext, "Next", app.player::next, size = 44.dp)
        }
        // Progress strip along the bottom edge.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(frac)
                .height(4.dp)
                .background(Neo.colors.pink)
        )
    }
}

/** Full-screen Now Playing page. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    track: Track,
    modifier: Modifier = Modifier,
    showQueue: Boolean,
    onToggleQueue: () -> Unit,
    onMinimize: () -> Unit,
) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val state by app.player.state.collectAsStateWithLifecycle()
    val position by app.player.position.collectAsStateWithLifecycle()

    val colors = Neo.colors
    // Offset by half the palette so the background never matches the cover placeholder.
    val accent = colors.accentFor(track.albumId + 3)
    val bg = if (colors.isDark) colors.paper else accent
    val fg = if (colors.isDark) colors.text else colors.onAccent
    val fgMuted = if (colors.isDark) colors.textMuted else colors.onAccent.copy(alpha = 0.7f)

    Column(
        modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        // Top bar: minimize on the left, queue + more on the right.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            NeoIconButton(Icons.Rounded.KeyboardArrowDown, "Minimize player", onMinimize, iconSize = 28.dp)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NOW PLAYING", style = Neo.type.caption.copy(fontFamily = com.chillysloth.neomusic.ui.theme.Display), color = fg)
                if (state.queue.isNotEmpty()) Text(
                    "${state.currentIndex + 1} of ${state.queue.size}", style = Neo.type.caption, color = fgMuted,
                )
            }
            NeoIconButton(
                Icons.AutoMirrored.Rounded.QueueMusic, if (showQueue) "Hide queue" else "Show queue", onToggleQueue,
                color = if (showQueue) colors.yellow else colors.surface,
            )
            Spacer(Modifier.width(10.dp))
            var menu by remember { mutableStateOf(false) }
            Box {
                NeoIconButton(Icons.Rounded.MoreVert, "More options", { menu = true })
                TrackMenu(track, menu, { menu = false }, beforeNavigate = onMinimize)
            }
        }

        // Artwork or queue
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedContent(showQueue, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "queue") { queue ->
                if (queue) QueueList(Modifier.fillMaxSize())
                else BigArtwork(track, fg)
            }
        }

        // Title / artist + quick add
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    track.title, style = Neo.type.h1, color = fg, maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                )
                Text(
                    track.artist, style = Neo.type.title, color = fgMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onMinimize(); actions.openArtist(track.artistId) },
                )
            }
            Spacer(Modifier.width(12.dp))
            NeoIconButton(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist", { actions.addToPlaylist(listOf(track)) }, color = colors.green)
        }

        Spacer(Modifier.height(18.dp))
        var scrub by remember { mutableStateOf<Float?>(null) }
        NeoSeekBar(
            fraction = scrub ?: if (state.durationMs > 0) position.toFloat() / state.durationMs else 0f,
            onScrub = { scrub = it },
            onSeek = { f -> app.player.seekTo((f * state.durationMs).toLong()); scrub = null },
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            val shown = scrub?.let { (it * state.durationMs).toLong() } ?: position
            Text(formatDuration(shown), style = Neo.type.label, color = fg)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(state.durationMs.takeIf { it > 0 } ?: track.durationMs), style = Neo.type.label, color = fg)
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NeoIconButton(
                Icons.Rounded.Shuffle, if (state.shuffle) "Shuffle on" else "Shuffle off", app.player::toggleShuffle,
                color = if (state.shuffle) colors.pink else colors.surface, size = 48.dp,
            )
            NeoIconButton(Icons.Rounded.SkipPrevious, "Previous", app.player::previous, size = 56.dp, iconSize = 30.dp)
            NeoIconButton(
                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (state.isPlaying) "Pause" else "Play",
                app.player::togglePlay,
                color = colors.yellow, size = 78.dp, iconSize = 42.dp,
                shape = RoundedCornerShape(24.dp), shadow = 6.dp,
            )
            NeoIconButton(Icons.Rounded.SkipNext, "Next", app.player::next, size = 56.dp, iconSize = 30.dp)
            NeoIconButton(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                when (state.repeatMode) {
                    Player.REPEAT_MODE_ONE -> "Repeat one"; Player.REPEAT_MODE_ALL -> "Repeat all"; else -> "Repeat off"
                },
                app.player::cycleRepeat,
                color = if (state.repeatMode != Player.REPEAT_MODE_OFF) colors.blue else colors.surface, size = 48.dp,
            )
        }
    }
}

/** Large cover; swipe it left/right to skip, like Samsung Music. */
@Composable
private fun BigArtwork(track: Track, fg: Color) {
    val app = LocalApp.current
    var dragX by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 90.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side: Dp = minOf(maxWidth, maxHeight) - 16.dp
        Box(
            Modifier
                .size(side)
                .pointerInput(track.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragX < -threshold) app.player.next() else if (dragX > threshold) app.player.previous()
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                    ) { change, dx -> change.consume(); dragX += dx }
                }
                .graphicsLayer {
                    translationX = dragX * 0.5f
                    rotationZ = dragX / 60f
                }
        ) {
            NeoBox(Modifier.fillMaxSize(), shape = RoundedCornerShape(22.dp), shadow = 10.dp, borderWidth = 3.dp) {
                AnimatedContent(track.albumId, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "art") { id ->
                    Artwork(id, Modifier.fillMaxSize(), sizeHint = 400.dp, iconScale = 0.35f)
                }
            }
            if (track.year > 0) NeoTag(
                track.year.toString(), Neo.colors.yellow,
                Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer { rotationZ = -8f; translationX = (-10).dp.toPx(); translationY = 10.dp.toPx() },
            )
        }
    }
}

/** Chunky seek bar: outlined track, solid fill and a square thumb. */
@Composable
fun NeoSeekBar(fraction: Float, onScrub: (Float) -> Unit, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = Neo.colors
    var current by remember { mutableFloatStateOf(0f) }
    val f = fraction.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { off -> onSeek((off.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> current = (off.x / size.width).coerceIn(0f, 1f); onScrub(current) },
                    onDragEnd = { onSeek(current) },
                    onDragCancel = { onSeek(current) },
                ) { change, _ ->
                    change.consume()
                    current = (change.position.x / size.width).coerceIn(0f, 1f)
                    onScrub(current)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumb = 26.dp
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.surface)
                .border(2.5.dp, colors.ink, RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(f)
                    .background(colors.ink)
            )
        }
        NeoBox(
            Modifier
                .offset(x = (maxWidth - thumb) * f)
                .size(thumb),
            color = colors.yellow,
            shape = RoundedCornerShape(7.dp),
            shadow = 3.dp,
        ) {}
    }
}

/** Up-next list with tap-to-jump, remove, and drag-to-reorder. */
@Composable
private fun QueueList(modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val library by app.repository.library.collectAsStateWithLifecycle()
    val state by app.player.state.collectAsStateWithLifecycle()

    // Local copy so drag feedback is instant; the controller catches up right after.
    var local by remember { mutableStateOf(state.queue) }
    LaunchedEffect(state.queue) { local = state.queue }
    val unique = local.size == local.toSet().size

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentIndex.coerceAtLeast(0))
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val list = local.toMutableList()
        list.add(to.index, list.removeAt(from.index))
        local = list
        app.player.moveInQueue(from.index, to.index)
    }

    NeoBox(modifier.padding(end = 6.dp, bottom = 6.dp), color = Neo.colors.surface, shadow = 8.dp) {
        Column {
            Row(Modifier.fillMaxWidth().background(Neo.colors.yellow).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("UP NEXT", style = Neo.type.h3, color = Neo.colors.onAccent, modifier = Modifier.weight(1f))
                Text("${local.size} tracks", style = Neo.type.label, color = Neo.colors.onAccent)
            }
            Box(Modifier.fillMaxWidth().height(2.5.dp).background(Neo.colors.ink))
            ScrollbarBox(listState, Modifier.fillMaxSize(), bottomPadding = 0.dp) {
            LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 6.dp)) {
                itemsIndexed(local, key = { i, id -> if (unique) id else "$i-$id" }) { i, id ->
                    val t = library.trackById[id]
                    ReorderableItem(reorder, key = if (unique) id else "$i-$id", enabled = unique) { dragging ->
                        val current = i == state.currentIndex
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    when {
                                        dragging -> Neo.colors.blue
                                        current -> Neo.colors.yellow.copy(alpha = 0.55f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { app.player.skipToQueueIndex(i) }
                                .padding(start = 12.dp, end = 22.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, Neo.colors.ink, RoundedCornerShape(8.dp))) {
                                Artwork(t?.albumId ?: -1, Modifier.fillMaxSize(), sizeHint = 42.dp)
                                if (current) Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                                    EqualizerBars(state.isPlaying, Color.White)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                val textColor = if (dragging || (current && !Neo.colors.isDark)) Neo.colors.onAccent else Neo.colors.text
                                Text(t?.title ?: "Unknown", style = Neo.type.title, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t?.artist ?: "", style = Neo.type.caption, color = textColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (!current) PlainIconButton(Icons.Rounded.Close, "Remove from queue", { app.player.removeFromQueue(i) }, size = 38.dp)
                            if (unique) Box(Modifier.draggableHandle().size(38.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.DragHandle, "Drag to reorder", tint = Neo.colors.text)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

