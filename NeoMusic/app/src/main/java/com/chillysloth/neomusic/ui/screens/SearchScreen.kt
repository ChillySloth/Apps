package com.chillysloth.neomusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chillysloth.neomusic.data.pluralTracks
import com.chillysloth.neomusic.ui.LocalActions
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.LocalBottomInset
import com.chillysloth.neomusic.ui.components.Artwork
import com.chillysloth.neomusic.ui.components.EmptyState
import com.chillysloth.neomusic.ui.components.NeoBox
import com.chillysloth.neomusic.ui.components.NeoIconButton
import com.chillysloth.neomusic.ui.components.NeoTextField
import com.chillysloth.neomusic.ui.components.PlainIconButton
import com.chillysloth.neomusic.ui.components.ScrollbarBox
import com.chillysloth.neomusic.ui.components.SectionTitle
import com.chillysloth.neomusic.ui.components.TrackRow
import com.chillysloth.neomusic.ui.theme.Neo
import java.text.Normalizer

/** Lowercase and strip accents so "beyonce" finds "Beyoncé". */
fun normalizeForSearch(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

@Composable
fun SearchScreen() {
    val app = LocalApp.current
    val actions = LocalActions.current
    val library by app.repository.library.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    val searchState = rememberLazyListState()

    val q = remember(query) { normalizeForSearch(query.trim()) }
    val tracks = remember(q, library) {
        if (q.isEmpty()) emptyList() else library.tracks.filter {
            normalizeForSearch(it.title).contains(q) || normalizeForSearch(it.artist).contains(q) || normalizeForSearch(it.album).contains(q)
        }
    }
    val albums = remember(q, library) {
        if (q.isEmpty()) emptyList() else library.albums.filter {
            normalizeForSearch(it.title).contains(q) || normalizeForSearch(it.artist).contains(q)
        }
    }
    val artists = remember(q, library) {
        if (q.isEmpty()) emptyList() else library.artists.filter { normalizeForSearch(it.name).contains(q) }
    }

    Column(Modifier.fillMaxSize().background(Neo.colors.paper).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 18.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", actions::back, size = 50.dp, shape = RoundedCornerShape(Neo.dimens.radius))
            Spacer(Modifier.width(12.dp))
            NeoTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Tracks, albums, artists",
                leading = Icons.Rounded.Search,
                shadow = Neo.dimens.shadow,
                modifier = Modifier.weight(1f),
                textFieldModifier = Modifier.focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                trailing = if (query.isNotEmpty()) ({
                    PlainIconButton(Icons.Rounded.Close, "Clear", { query = "" }, size = 36.dp)
                }) else null,
            )
        }

        when {
            q.isEmpty() -> EmptyState(
                "Search everything", "Find tracks, albums and artists in your library.",
                Icons.Rounded.Search, color = Neo.colors.yellow,
            )
            tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() -> EmptyState(
                "Nothing found", "No matches for \"${query.trim()}\".", Icons.Rounded.SearchOff, color = Neo.colors.orange,
            )
            else -> ScrollbarBox(searchState, Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = searchState,
                contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
            ) {
                if (artists.isNotEmpty()) {
                    item(key = "artists-title") { SectionTitle("Artists") }
                    item(key = "artists") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            items(artists, key = { it.id }) { a ->
                                Column(
                                    Modifier
                                        .width(92.dp)
                                        .clip(RoundedCornerShape(Neo.dimens.radius))
                                        .clickable { keyboard?.hide(); actions.openArtist(a.id) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    NeoBox(Modifier.size(84.dp), shape = CircleShape, shadow = Neo.dimens.shadowSmall) {
                                        Artwork(a.coverAlbumId, Modifier.size(84.dp), sizeHint = 84.dp)
                                    }
                                    Text(
                                        a.name, style = Neo.type.label, color = Neo.colors.text, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                if (albums.isNotEmpty()) {
                    item(key = "albums-title") { SectionTitle("Albums") }
                    items(albums.take(30), key = { "al" + it.id }) { album ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(Neo.dimens.radius))
                                .clickable { keyboard?.hide(); actions.openAlbum(album.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NeoBox(Modifier.size(52.dp), shape = RoundedCornerShape(Neo.dimens.radiusSmall), shadow = 2.dp, borderWidth = 2.dp) {
                                Artwork(album.id, Modifier.size(52.dp), sizeHint = 52.dp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(album.title, style = Neo.type.title, color = Neo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${album.artist} · ${pluralTracks(album.tracks.size)}", style = Neo.type.caption, color = Neo.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (tracks.isNotEmpty()) {
                    item(key = "tracks-title") { SectionTitle("Tracks") }
                    itemsIndexed(tracks, key = { _, t -> "tr" + t.id }) { i, t ->
                        TrackRow(
                            t,
                            isCurrent = t.id == player.currentTrackId,
                            isPlaying = player.isPlaying,
                            onClick = { keyboard?.hide(); actions.play(tracks, i) },
                        )
                    }
                }
            }
            }
        }
    }
}
