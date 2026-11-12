package com.chillysloth.neomusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chillysloth.neomusic.ui.LocalActions
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.components.Artwork
import com.chillysloth.neomusic.ui.components.NeoBox
import com.chillysloth.neomusic.ui.components.NeoButton
import com.chillysloth.neomusic.ui.components.NeoIconButton
import com.chillysloth.neomusic.ui.components.NeoTextField
import com.chillysloth.neomusic.ui.components.PlainIconButton
import com.chillysloth.neomusic.ui.theme.Neo

/** Multi-select list of all tracks, used for "Add tracks" on a playlist page. */
@Composable
fun TrackPickerScreen(playlistId: Long) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val library by app.repository.library.collectAsStateWithLifecycle()
    val playlists by app.playlists.playlists.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    val existing = remember(playlist) { playlist?.trackIds?.toHashSet() ?: hashSetOf() }
    val selected = remember { mutableStateListOf<Long>() }
    var query by rememberSaveable { mutableStateOf("") }
    val q = normalizeForSearch(query.trim())
    val visible = remember(library, q) {
        if (q.isEmpty()) library.tracks
        else library.tracks.filter { normalizeForSearch(it.title + " " + it.artist + " " + it.album).contains(q) }
    }

    Column(Modifier.fillMaxSize().background(Neo.colors.paper).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            NeoIconButton(Icons.Rounded.Close, "Cancel", actions::back)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Add tracks", style = Neo.type.h2, color = Neo.colors.text)
                Text("to ${playlist?.name ?: "playlist"}", style = Neo.type.caption, color = Neo.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        NeoTextField(
            query, { query = it }, "Filter tracks",
            leading = Icons.Rounded.Search,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            trailing = if (query.isNotEmpty()) ({ PlainIconButton(Icons.Rounded.Close, "Clear", { query = "" }, size = 36.dp) }) else null,
        )
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(visible, key = { it.id }) { t ->
                val already = t.id in existing
                val checked = already || t.id in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(Neo.dimens.radius))
                        .clickable(enabled = !already) {
                            if (t.id in selected) selected.remove(t.id) else selected.add(t.id)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (checked) (if (already) Neo.colors.surfaceAlt else Neo.colors.green) else Neo.colors.surface)
                            .border(2.5.dp, Neo.colors.ink, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (checked) Icon(Icons.Rounded.Check, null, tint = Neo.colors.onAccent.takeIf { !already } ?: Neo.colors.textMuted, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    NeoBox(Modifier.size(46.dp), shape = RoundedCornerShape(Neo.dimens.radiusSmall), shadow = 0.dp, borderWidth = 2.dp) {
                        Artwork(t.albumId, Modifier.size(46.dp), sizeHint = 46.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = Neo.type.title, color = if (already) Neo.colors.textMuted else Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (already) "Already in playlist" else "${t.artist} · ${t.album}",
                            style = Neo.type.caption, color = Neo.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${selected.size} selected", style = Neo.type.label, color = Neo.colors.text, modifier = Modifier.weight(1f))
            NeoButton(
                "Add to playlist",
                {
                    if (selected.isNotEmpty()) {
                        val n = app.playlists.addTracks(playlistId, selected.toList())
                        actions.toast(if (n == 1) "Added 1 track" else "Added $n tracks")
                    }
                    actions.back()
                },
                color = if (selected.isEmpty()) Neo.colors.surfaceAlt else Neo.colors.yellow,
                icon = Icons.Rounded.Check,
            )
        }
    }
}
