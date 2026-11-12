package com.chillysloth.neomusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chillysloth.neomusic.data.Playlist
import com.chillysloth.neomusic.data.SortSpec
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.theme.Neo

/**
 * Samsung-Music-style list header: a Shuffle pill + Play button on the left,
 * item count, and the sort control on the right.
 */
@Composable
fun ListActionsHeader(
    countLabel: String,
    onShuffle: (() -> Unit)?,
    onPlay: (() -> Unit)?,
    modifier: Modifier = Modifier,
    sort: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        if (onShuffle != null) {
            NeoButton(
                "Shuffle", onShuffle, icon = Icons.Rounded.Shuffle, color = Neo.colors.pink,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 9.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        if (onPlay != null) {
            NeoIconButton(Icons.Rounded.PlayArrow, "Play all", onPlay, color = Neo.colors.yellow, size = 40.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(countLabel, style = Neo.type.label, color = Neo.colors.textMuted, modifier = Modifier.weight(1f), maxLines = 1)
        sort?.invoke()
    }
}

/** Sort dropdown: pick a key, and flip ascending / descending. */
@Composable
fun <T : Enum<T>> SortButton(
    current: SortSpec<T>,
    options: List<T>,
    label: (T) -> String,
    onChange: (SortSpec<T>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        NeoBox(
            color = Neo.colors.surface,
            shape = RoundedCornerShape(50),
            shadow = Neo.dimens.shadowSmall,
            onClick = { open = true },
            onClickLabel = "Sort",
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = Neo.colors.text, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label(current.key), style = Neo.type.label, color = Neo.colors.text, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (current.descending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    null, tint = Neo.colors.text, modifier = Modifier.size(16.dp),
                )
            }
        }
        NeoMenu(open, { open = false }) {
            Text(
                "SORT BY", style = Neo.type.caption, color = Neo.colors.textMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            options.forEach { opt ->
                NeoMenuItem(
                    label(opt), null,
                    onClick = { onChange(current.copy(key = opt)); open = false },
                    trailing = if (opt == current.key) ({ Icon(Icons.Rounded.Check, null, tint = Neo.colors.text) }) else null,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 2.dp, color = Neo.colors.ink)
            NeoMenuItem(
                "Ascending", Icons.Rounded.ArrowUpward,
                onClick = { onChange(current.copy(descending = false)); open = false },
                trailing = if (!current.descending) ({ Icon(Icons.Rounded.Check, null, tint = Neo.colors.text) }) else null,
            )
            NeoMenuItem(
                "Descending", Icons.Rounded.ArrowDownward,
                onClick = { onChange(current.copy(descending = true)); open = false },
                trailing = if (current.descending) ({ Icon(Icons.Rounded.Check, null, tint = Neo.colors.text) }) else null,
            )
        }
    }
}

/**
 * Playlist cover. Uses, in order: a custom image the user picked, an album cover the
 * user picked, a 2×2 mosaic of album covers, a single cover, or a flat colored block.
 */
@Composable
fun PlaylistCover(
    playlist: Playlist,
    albumIds: List<Long>,
    modifier: Modifier = Modifier,
    sizeHint: androidx.compose.ui.unit.Dp = 56.dp,
) {
    val image = playlist.coverImage
    when {
        image != null -> CustomCover(image, modifier, sizeHint) { AutoCover(albumIds, playlist.id, modifier, sizeHint) }
        playlist.coverAlbumId != null -> Artwork(playlist.coverAlbumId, modifier, sizeHint = sizeHint)
        else -> AutoCover(albumIds, playlist.id, modifier, sizeHint)
    }
}

@Composable
private fun CustomCover(name: String, modifier: Modifier, sizeHint: androidx.compose.ui.unit.Dp, fallback: @Composable () -> Unit) {
    val app = LocalApp.current
    val file = remember(name) { app.playlists.coverFile(name) }
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { sizeHint.roundToPx() }
    var loaded by remember(name) { mutableStateOf(false) }
    val img by produceState(app.artwork.cachedFile(file, px), name, px) {
        if (value == null) value = app.artwork.loadFile(file, px)
        loaded = true
    }
    val bitmap = img
    when {
        bitmap != null -> androidx.compose.foundation.Image(
            bitmap, contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier,
        )
        loaded -> fallback() // file missing or unreadable
        else -> Box(modifier.background(Neo.colors.surfaceAlt))
    }
}

@Composable
private fun AutoCover(albumIds: List<Long>, playlistId: Long, modifier: Modifier, sizeHint: androidx.compose.ui.unit.Dp) {
    val distinct = remember(albumIds) { albumIds.distinct().take(4) }
    when {
        distinct.size >= 4 -> Column(modifier) {
            Row(Modifier.weight(1f)) {
                Artwork(distinct[0], Modifier.weight(1f).fillMaxSize(), sizeHint = sizeHint / 2)
                Artwork(distinct[1], Modifier.weight(1f).fillMaxSize(), sizeHint = sizeHint / 2)
            }
            Row(Modifier.weight(1f)) {
                Artwork(distinct[2], Modifier.weight(1f).fillMaxSize(), sizeHint = sizeHint / 2)
                Artwork(distinct[3], Modifier.weight(1f).fillMaxSize(), sizeHint = sizeHint / 2)
            }
        }
        distinct.isNotEmpty() -> Artwork(distinct[0], modifier, sizeHint = sizeHint)
        else -> Box(modifier.background(Neo.colors.accentFor(playlistId)), contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color(0xFF111111),
                modifier = Modifier.fillMaxSize(0.45f),
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = Neo.colors.green,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NeoBox(Modifier.size(96.dp), color = color, shape = RoundedCornerShape(24.dp), shadow = 6.dp, contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF111111), modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(title, style = Neo.type.h2, color = Neo.colors.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = Neo.type.body, color = Neo.colors.textMuted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = Neo.type.h2, color = Neo.colors.text)
        trailing?.invoke()
    }
}
