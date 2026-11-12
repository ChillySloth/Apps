package com.chillysloth.neomusic.ui.screens

import android.content.Context
import android.media.MediaMetadataRetriever
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chillysloth.neomusic.data.Track
import com.chillysloth.neomusic.data.formatDuration
import com.chillysloth.neomusic.ui.LocalActions
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.LocalBottomInset
import com.chillysloth.neomusic.ui.components.Artwork
import com.chillysloth.neomusic.ui.components.EmptyState
import com.chillysloth.neomusic.ui.components.NeoBox
import com.chillysloth.neomusic.ui.components.NeoButton
import com.chillysloth.neomusic.ui.components.NeoTag
import com.chillysloth.neomusic.ui.theme.Neo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Extra tags read from the file itself (not stored in MediaStore). */
private data class FileTags(
    val recordDate: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val genre: String? = null,
    val composer: String? = null,
    val disc: String? = null,
)

private suspend fun readTags(context: Context, track: Track): FileTags = withContext(Dispatchers.IO) {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(context, track.uri)
        FileTags(
            recordDate = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.let(::prettyDate),
            bitrateKbps = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.div(1000),
            sampleRateHz = if (android.os.Build.VERSION.SDK_INT >= 31)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() else null,
            genre = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
            composer = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
            disc = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER),
        )
    } catch (_: Exception) {
        FileTags()
    } finally {
        runCatching { r.release() }
    }
}

/** MediaMetadataRetriever returns dates like "20190312T000000.000Z". */
private fun prettyDate(raw: String): String? {
    if (raw.isBlank() || raw.startsWith("1904") || raw.startsWith("0000")) return null
    val patterns = listOf("yyyyMMdd'T'HHmmss.SSS'Z'", "yyyyMMdd'T'HHmmss", "yyyy-MM-dd", "yyyy")
    for (p in patterns) {
        val parsed = runCatching {
            SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(raw)
        }.getOrNull() ?: continue
        return if (p == "yyyy") raw else DateFormat.getDateInstance(DateFormat.LONG).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(parsed)
    }
    return raw
}

@Composable
fun TrackDetailsScreen(trackId: Long) {
    val app = LocalApp.current
    val actions = LocalActions.current
    val context = LocalContext.current
    val library by app.repository.library.collectAsStateWithLifecycle()
    val track = library.trackById[trackId]
    val listState = rememberLazyListState()

    DetailScaffold(track?.title ?: "Track details", listState) {
        if (track == null) {
            EmptyState("Track not found", "It may have been deleted from this device.", Icons.Rounded.Info)
            return@DetailScaffold
        }
        val tags by produceState(FileTags(), track.id) { value = readTags(context, track) }

        val recordDate = tags.recordDate ?: track.year.takeIf { it > 0 }?.toString() ?: "Unknown"
        val location = track.path ?: listOfNotNull(track.relativePath, track.displayName).joinToString("")
        val folder = track.path?.substringBeforeLast('/') ?: track.relativePath

        val rows = buildList {
            add("Title" to track.title)
            add("Artist" to track.artist)
            add("Album" to track.album)
            track.albumArtist?.takeIf { it != track.artist }?.let { add("Album artist" to it) }
            add("Duration" to formatDuration(track.durationMs))
            add("Record date" to recordDate)
            (track.trackNumber % 1000).takeIf { it > 0 }?.let { add("Track number" to it.toString()) }
            tags.disc?.let { add("Disc" to it) }
            tags.genre?.takeIf { it.isNotBlank() }?.let { add("Genre" to it) }
            tags.composer?.takeIf { it.isNotBlank() }?.let { add("Composer" to it) }
        }
        val fileRows = buildList {
            add("File location" to location.ifBlank { "Unknown" })
            folder?.let { add("Folder" to it) }
            track.displayName?.let { add("File name" to it) }
            add("Format" to (track.mimeType?.substringAfter('/')?.uppercase() ?: "Unknown"))
            add("Size" to Formatter.formatFileSize(context, track.sizeBytes))
            tags.bitrateKbps?.let { add("Bitrate" to "$it kbps") }
            tags.sampleRateHz?.let { add("Sample rate" to "%.1f kHz".format(it / 1000f)) }
            add("Date added" to DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(track.dateAddedSec * 1000)))
        }

        LazyColumn(
            Modifier.fillMaxSize(), state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 22.dp, bottom = LocalBottomInset.current + 24.dp),
        ) {
            item(key = "hero") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Box {
                        NeoBox(Modifier.size(180.dp), shadow = 7.dp, borderWidth = 3.dp) {
                            Artwork(track.albumId, Modifier.fillMaxSize(), sizeHint = 200.dp)
                        }
                        NeoTag(
                            "DETAILS", Neo.colors.orange,
                            Modifier.align(Alignment.TopEnd).graphicsLayer {
                                rotationZ = 10f; translationX = 22.dp.toPx(); translationY = (-8).dp.toPx()
                            },
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(track.title, style = Neo.type.h1, color = Neo.colors.text, textAlign = TextAlign.Center)
                    Text(track.artist, style = Neo.type.label, color = Neo.colors.textMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NeoButton("Play", { actions.play(listOf(track), shuffle = false) }, icon = Icons.Rounded.PlayArrow, color = Neo.colors.yellow)
                        NeoButton("Add to playlist", { actions.addToPlaylist(listOf(track)) }, icon = Icons.AutoMirrored.Rounded.PlaylistAdd, color = Neo.colors.green)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            item(key = "info") { DetailsCard("Track", Neo.colors.yellow, rows) }
            item(key = "spacer") { Spacer(Modifier.height(22.dp)) }
            item(key = "file") { DetailsCard("File", Neo.colors.blue, fileRows) }
        }
    }
}

@Composable
private fun DetailsCard(title: String, accent: androidx.compose.ui.graphics.Color, rows: List<Pair<String, String>>) {
    NeoBox(Modifier.fillMaxWidth(), shadow = Neo.dimens.shadow) {
        Column {
            NeoBox(
                Modifier.fillMaxWidth(), color = accent, shadow = 0.dp,
                shape = RoundedCornerShape(topStart = Neo.dimens.radius, topEnd = Neo.dimens.radius),
            ) {
                Text(title.uppercase(), style = Neo.type.h3, color = Neo.colors.onAccent, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
            SelectionContainer {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    rows.forEachIndexed { i, (label, value) ->
                        if (i > 0) HorizontalDivider(thickness = 1.5.dp, color = Neo.colors.ink.copy(alpha = 0.18f))
                        Row(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
                            Text(label, style = Neo.type.label, color = Neo.colors.textMuted, modifier = Modifier.width(112.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(value, style = Neo.type.body, color = Neo.colors.text, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
