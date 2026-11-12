package com.chillysloth.neomusic.ui.components

import androidx.compose.foundation.clickable
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
                                p.trackIds.mapNotNull { library.trackById[it]?.albumId },
                                p.id, Modifier.size(44.dp), sizeHint = 44.dp,
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

