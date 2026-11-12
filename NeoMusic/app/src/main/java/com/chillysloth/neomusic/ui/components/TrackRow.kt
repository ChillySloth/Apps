package com.chillysloth.neomusic.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chillysloth.neomusic.data.Track
import com.chillysloth.neomusic.data.formatDuration
import com.chillysloth.neomusic.ui.LocalActions
import com.chillysloth.neomusic.ui.theme.Neo

data class ExtraMenuItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * One track in any list. The current track gets a highlighted neubrutal card,
 * everything else stays flat so long lists read cleanly.
 */
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingNumber: Int? = null,
    showArt: Boolean = true,
    subtitle: String = "${track.artist} · ${track.album}",
    extraMenuItems: List<ExtraMenuItem> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
    highlight: Boolean = false,
) {
    val carded = isCurrent || highlight
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingNumber != null) {
                Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                    if (isCurrent) EqualizerBars(isPlaying, Neo.colors.onAccent)
                    else Text(
                        leadingNumber.toString(), style = Neo.type.h3,
                        color = Neo.colors.textMuted,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            if (showArt) {
                Box {
                    NeoBox(Modifier.size(52.dp), shape = RoundedCornerShape(Neo.dimens.radiusSmall), shadow = 2.dp, borderWidth = 2.dp) {
                        Artwork(track.albumId, Modifier.size(52.dp), sizeHint = 52.dp)
                    }
                    if (isCurrent && leadingNumber == null) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .background(Color(0x66000000), RoundedCornerShape(Neo.dimens.radiusSmall)),
                            contentAlignment = Alignment.Center,
                        ) { EqualizerBars(isPlaying, Color.White) }
                    }
                }
                Spacer(Modifier.width(14.dp))
            }
            val textColor = if (carded) Neo.colors.onAccent else Neo.colors.text
            Column(Modifier.weight(1f)) {
                Text(
                    track.title, style = Neo.type.title, color = textColor,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle, style = Neo.type.caption,
                    color = if (carded) Neo.colors.onAccent.copy(alpha = 0.75f) else Neo.colors.textMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            if (trailing != null) trailing()
            else Text(
                formatDuration(track.durationMs), style = Neo.type.caption,
                color = if (carded) Neo.colors.onAccent else Neo.colors.textMuted,
            )
            TrackMenuButton(track, extraMenuItems, tint = textColor)
        }
    }

    if (carded) {
        NeoBox(
            modifier.padding(start = 10.dp, end = 14.dp, top = 3.dp, bottom = 6.dp),
            color = if (isCurrent) Neo.colors.yellow else Neo.colors.blue,
            shadow = if (highlight) 7.dp else Neo.dimens.shadowSmall,
            onClick = onClick,
        ) { content() }
    } else {
        Box(
            modifier
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(Neo.dimens.radius))
                .clickable(onClick = onClick)
        ) { content() }
    }
}

@Composable
fun TrackMenuButton(
    track: Track,
    extraMenuItems: List<ExtraMenuItem> = emptyList(),
    tint: Color = Neo.colors.text,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        PlainIconButton(Icons.Rounded.MoreVert, "More options for ${track.title}", { open = true }, tint = tint)
        TrackMenu(track, open, { open = false }, extraMenuItems)
    }
}

/** The five standard track options (plus any context-specific extras). */
@Composable
fun TrackMenu(
    track: Track,
    expanded: Boolean,
    onDismiss: () -> Unit,
    extraMenuItems: List<ExtraMenuItem> = emptyList(),
    beforeNavigate: () -> Unit = {},
) {
    val actions = LocalActions.current
    NeoMenu(expanded, onDismiss) {
        NeoMenuItem("Add to playlist", Icons.AutoMirrored.Rounded.PlaylistAdd, {
            onDismiss(); actions.addToPlaylist(listOf(track))
        })
        NeoMenuItem("Album", Icons.Rounded.Album, {
            onDismiss(); beforeNavigate(); actions.openAlbum(track.albumId)
        })
        NeoMenuItem("Artist", Icons.Rounded.Person, {
            onDismiss(); beforeNavigate(); actions.openArtist(track.artistId)
        })
        NeoMenuItem("Delete", Icons.Rounded.DeleteOutline, {
            onDismiss(); actions.delete(listOf(track))
        })
        NeoMenuItem("Track details", Icons.Rounded.Info, {
            onDismiss(); beforeNavigate(); actions.openTrackDetails(track.id)
        })
        if (extraMenuItems.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 2.dp, color = Neo.colors.ink)
            extraMenuItems.forEach { item ->
                NeoMenuItem(item.label, item.icon, { onDismiss(); item.onClick() })
            }
        }
    }
}

fun removeFromPlaylistItem(onClick: () -> Unit) =
    ExtraMenuItem("Remove from playlist", Icons.Rounded.RemoveCircleOutline, onClick)

/** Tiny animated "now playing" bars. */
@Composable
fun EqualizerBars(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "eq")
    val heights = listOf(0, 180, 360).map { delay ->
        transition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                androidx.compose.animation.core.tween(420, delayMillis = delay),
                androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "bar",
        )
    }
    Row(modifier.height(18.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        heights.forEachIndexed { i, h ->
            val frac = if (playing) h.value else listOf(0.45f, 0.8f, 0.6f)[i]
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(frac)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
    }
}
