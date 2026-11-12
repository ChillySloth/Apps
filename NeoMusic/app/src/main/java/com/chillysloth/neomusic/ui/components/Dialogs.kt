package com.chillysloth.neomusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chillysloth.neomusic.data.Library
import com.chillysloth.neomusic.data.Playlist
import com.chillysloth.neomusic.data.pluralTracks
import com.chillysloth.neomusic.ui.theme.Neo

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    library: Library,
    trackCount: Int,
    onDismiss: () -> Unit,
    onPick: (Playlist) -> Unit,
    onCreateNew: () -> Unit,
) {
    NeoDialog(
        title = if (trackCount == 1) "Add to playlist" else "Add $trackCount tracks to playlist",
        onDismiss = onDismiss,
        accent = Neo.colors.green,
    ) {
        NeoButton(
            "New playlist", onCreateNew, icon = Icons.Rounded.Add, color = Neo.colors.yellow,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Neo.dimens.radiusSmall),
        )
        Spacer(Modifier.height(14.dp))
        if (playlists.isEmpty()) {
            Text("No playlists yet.", style = Neo.type.body, color = Neo.colors.textMuted)
        } else {
            LazyColumn(Modifier.heightIn(max = 340.dp)) {
                items(playlists, key = { it.id }) { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Neo.dimens.radiusSmall))
                            .clickable { onPick(p) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NeoBox(Modifier.size(44.dp), shape = RoundedCornerShape(8.dp), shadow = 2.dp, borderWidth = 2.dp) {
                            PlaylistCover(
                                p, p.trackIds.mapNotNull { library.trackById[it]?.albumId },
                                Modifier.size(44.dp), sizeHint = 44.dp,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(p.name, style = Neo.type.title, color = Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pluralTracks(p.trackIds.size), style = Neo.type.caption, color = Neo.colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(initial, TextRange(initial.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val submit = { if (value.text.isNotBlank()) onConfirm(value.text.trim()) }
    NeoDialog(title, onDismiss, accent = Neo.colors.yellow) {
        NeoBox(color = Neo.colors.surface, shape = RoundedCornerShape(Neo.dimens.radiusSmall), shadow = 0.dp) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = Neo.type.title.copy(color = Neo.colors.text),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Neo.colors.text),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    if (value.text.isEmpty()) Text(placeholder, style = Neo.type.title, color = Neo.colors.textMuted)
                    inner()
                },
            )
        }
        Spacer(Modifier.height(20.dp))
        DialogButtons(confirmLabel, onDismiss, submit, confirmEnabled = value.text.isNotBlank())
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    accent: androidx.compose.ui.graphics.Color = Neo.colors.orange,
) {
    NeoDialog(title, onDismiss, accent = accent) {
        Text(message, style = Neo.type.body, color = Neo.colors.text)
        Spacer(Modifier.height(22.dp))
        DialogButtons(confirmLabel, onDismiss, onConfirm, confirmColor = accent)
    }
}

@Composable
private fun DialogButtons(
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmColor: androidx.compose.ui.graphics.Color = Neo.colors.yellow,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        NeoButton("Cancel", onDismiss, color = Neo.colors.surface, textColor = Neo.colors.text)
        Spacer(Modifier.width(12.dp))
        NeoButton(
            confirmLabel, { if (confirmEnabled) onConfirm() },
            color = if (confirmEnabled) confirmColor else Neo.colors.surfaceAlt,
        )
    }
}

@Composable
fun MessageBanner(text: String, modifier: Modifier = Modifier) {
    NeoBox(modifier, color = Neo.colors.green, shape = RoundedCornerShape(Neo.dimens.radiusSmall), shadow = Neo.dimens.shadowSmall) {
        Text(text, style = Neo.type.label, color = Neo.colors.onAccent, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}


/** Choose a playlist cover: a gallery photo, one of the playlist's album covers, or automatic. */
@Composable
fun PlaylistCoverDialog(
    playlist: Playlist,
    albums: List<com.chillysloth.neomusic.data.Album>,
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onAlbum: (Long) -> Unit,
    onAutomatic: () -> Unit,
) {
    val isAuto = playlist.coverImage == null && playlist.coverAlbumId == null
    NeoDialog("Playlist cover", onDismiss, accent = Neo.colors.pink) {
        NeoButton(
            "Choose from gallery", onGallery,
            icon = Icons.Rounded.AddPhotoAlternate, color = Neo.colors.yellow,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Neo.dimens.radiusSmall),
        )
        Spacer(Modifier.height(12.dp))
        NeoButton(
            if (isAuto) "Automatic (current)" else "Automatic", onAutomatic,
            icon = if (isAuto) Icons.Rounded.Check else Icons.Rounded.GridView,
            color = if (isAuto) Neo.colors.green else Neo.colors.surface,
            textColor = if (isAuto) Neo.colors.onAccent else Neo.colors.text,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Neo.dimens.radiusSmall),
        )
        if (albums.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("OR USE AN ALBUM COVER", style = Neo.type.caption, color = Neo.colors.textMuted)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp, bottom = 4.dp),
            ) {
                gridItems(albums, key = { it.id }) { album ->
                    val selected = playlist.coverAlbumId == album.id && playlist.coverImage == null
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(if (selected) 4.dp else 2.dp, Neo.colors.ink, RoundedCornerShape(10.dp))
                            .clickable(onClickLabel = album.title) { onAlbum(album.id) }
                    ) {
                        Artwork(album.id, Modifier.fillMaxSize(), sizeHint = 90.dp)
                        if (selected) Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Neo.colors.green)
                                .border(2.dp, Neo.colors.ink, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Check, "Selected", tint = Neo.colors.onAccent, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
        }
    }
}
