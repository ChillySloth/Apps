package com.chillysloth.neomusic.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chillysloth.neomusic.data.sortKey
import com.chillysloth.neomusic.ui.LocalBottomInset
import com.chillysloth.neomusic.ui.theme.Display
import com.chillysloth.neomusic.ui.theme.Neo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Where a lazy list currently is, in item units. */
private data class ScrollInfo(
    val total: Int,
    val first: Float,      // index of the first visible item, plus how far it is scrolled off
    val visible: Int,
    val scrollable: Boolean,
) {
    val progress: Float
        get() = if (total - visible <= 0) 0f else (first / (total - visible)).coerceIn(0f, 1f)
}

/** First letter for the drag bubble ("#" for numbers and symbols). */
fun initialOf(s: String): String {
    val c = sortKey(s).firstOrNull { !it.isWhitespace() } ?: return "#"
    return if (c.isLetter()) c.uppercaseChar().toString() else "#"
}

/**
 * Wraps a [androidx.compose.foundation.lazy.LazyColumn] with a draggable fast scroller on its right edge.
 * [label] maps a list index to the text shown in the bubble while dragging (null = no bubble).
 */
@Composable
fun ScrollbarBox(
    state: LazyListState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = LocalBottomInset.current,
    label: ((Int) -> String?)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val info by remember(state) {
        derivedStateOf {
            val li = state.layoutInfo
            val items = li.visibleItemsInfo
            if (items.isEmpty() || li.totalItemsCount == 0) ScrollInfo(0, 0f, 0, false)
            else {
                val head = items.first()
                val off = (li.viewportStartOffset - head.offset).toFloat() / head.size.coerceAtLeast(1)
                ScrollInfo(
                    li.totalItemsCount, head.index + off.coerceIn(0f, 1f), items.size,
                    state.canScrollForward || state.canScrollBackward,
                )
            }
        }
    }
    Box(modifier) {
        content()
        Scrollbar(
            info = { info },
            isScrolling = state.isScrollInProgress,
            scrollToItem = { state.scrollToItem(it) },
            label = label,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, bottom = bottomPadding + 8.dp),
        )
    }
}

/** Grid version of [ScrollbarBox]. */
@Composable
fun ScrollbarBox(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = LocalBottomInset.current,
    label: ((Int) -> String?)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val info by remember(state) {
        derivedStateOf {
            val li = state.layoutInfo
            val items = li.visibleItemsInfo
            if (items.isEmpty() || li.totalItemsCount == 0) ScrollInfo(0, 0f, 0, false)
            else {
                val head = items.first()
                val off = (li.viewportStartOffset - head.offset.y).toFloat() / head.size.height.coerceAtLeast(1)
                ScrollInfo(
                    li.totalItemsCount, head.index + off.coerceIn(0f, 1f), items.size,
                    state.canScrollForward || state.canScrollBackward,
                )
            }
        }
    }
    Box(modifier) {
        content()
        Scrollbar(
            info = { info },
            isScrolling = state.isScrollInProgress,
            scrollToItem = { state.scrollToItem(it) },
            label = label,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, bottom = bottomPadding + 8.dp),
        )
    }
}

/**
 * Samsung-style fast scroller: a slim pill while idle that grows into a chunky
 * neubrutal handle while you scroll or drag it. Drag it to jump through the list.
 */
@Composable
private fun Scrollbar(
    info: () -> ScrollInfo,
    isScrolling: Boolean,
    scrollToItem: suspend (Int) -> Unit,
    label: ((Int) -> String?)?,
    modifier: Modifier,
) {
    val current = info()
    if (!current.scrollable) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    var recentlyActive by remember { mutableStateOf(false) }
    LaunchedEffect(isScrolling, dragging) {
        if (isScrolling || dragging) recentlyActive = true
        else {
            delay(1500); recentlyActive = false
        }
    }
    val active = dragging || recentlyActive
    val thumbWidth by animateDpAsState(if (active) 14.dp else 6.dp, label = "thumbW")
    val thumbAlpha by animateFloatAsState(if (active) 1f else 0.45f, label = "thumbA")
    val thumbHeight = 52.dp
    val touchWidth = 30.dp
    val colors = Neo.colors

    BoxWithConstraints(modifier.fillMaxHeight().width(touchWidth + 80.dp)) {
        val trackPx = with(density) { (maxHeight - thumbHeight).toPx() }.coerceAtLeast(1f)
        val progress = if (dragging) dragProgress else current.progress
        val thumbY = with(density) { (progress * trackPx).toDp() }

        fun jumpTo(p: Float) {
            dragProgress = p.coerceIn(0f, 1f)
            val i = info()
            val target = (dragProgress * (i.total - i.visible).coerceAtLeast(0)).roundToInt()
            scope.launch { scrollToItem(target.coerceIn(0, (i.total - 1).coerceAtLeast(0))) }
        }

        // Touch target: only the handle itself, so row buttons near the edge still work.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(y = thumbY)
                .size(touchWidth, thumbHeight)
                .pointerInput(trackPx) {
                    var startProgress = 0f
                    var accumulated = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            startProgress = info().progress
                            accumulated = 0f
                            dragProgress = startProgress
                            dragging = true
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, dy ->
                        change.consume()
                        accumulated += dy
                        jumpTo(startProgress + accumulated / trackPx)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .width(thumbWidth)
                    .height(thumbHeight - 4.dp)
                    .graphicsLayer { alpha = thumbAlpha }
                    .then(
                        if (active) Modifier
                            .drawBehind {
                                val s = 2.dp.toPx()
                                drawRoundRect(
                                    colors.ink, topLeft = Offset(s, s), size = Size(size.width, size.height),
                                    cornerRadius = CornerRadius(size.width / 2),
                                )
                            }
                            .background(colors.yellow, RoundedCornerShape(50))
                            .border(2.dp, colors.ink, RoundedCornerShape(50))
                        else Modifier.background(colors.ink, RoundedCornerShape(50))
                    )
            )
        }

        // Letter bubble next to the handle while dragging.
        if (dragging && label != null) {
            val firstIndex = info().first.toInt()
            val text = label(firstIndex)
            if (!text.isNullOrEmpty()) {
                NeoBox(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = -(touchWidth + 6.dp), y = thumbY)
                        .sizeIn(minWidth = 52.dp, minHeight = 52.dp),
                    color = colors.yellow,
                    shape = RoundedCornerShape(14.dp),
                    shadow = 3.dp,
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text,
                        style = Neo.type.h2.copy(fontFamily = Display, fontSize = if (text.length <= 2) 26.sp else 16.sp),
                        color = colors.onAccent,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
