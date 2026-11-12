package com.chillysloth.neomusic.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chillysloth.neomusic.ui.LocalApp
import com.chillysloth.neomusic.ui.theme.Neo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads album covers from MediaStore with an in-memory cache (including misses). */
class ArtworkLoader(private val context: Context) {
    private object Missing

    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    private val cache = object : LruCache<String, Any>(maxKb) {
        override fun sizeOf(key: String, value: Any): Int =
            if (value is ImageBitmap) (value.width * value.height * 4 / 1024).coerceAtLeast(1) else 1
    }

    fun cached(albumId: Long, sizePx: Int): ImageBitmap? = cache.get(key(albumId, sizePx)) as? ImageBitmap

    private fun key(albumId: Long, sizePx: Int) = "$albumId@${bucket(sizePx)}"
    private fun bucket(px: Int) = when {
        px <= 160 -> 160
        px <= 400 -> 400
        else -> 900
    }

    suspend fun load(albumId: Long, sizePx: Int): ImageBitmap? {
        if (albumId < 0) return null
        val k = key(albumId, sizePx)
        cache.get(k)?.let { return it as? ImageBitmap }
        val bmp = withContext(Dispatchers.IO) { runCatching { decode(albumId, bucket(sizePx)) }.getOrNull() }
        val img = bmp?.asImageBitmap()
        cache.put(k, img ?: Missing)
        return img
    }

    fun cachedFile(file: java.io.File, sizePx: Int): ImageBitmap? = cache.get(fileKey(file, sizePx)) as? ImageBitmap

    private fun fileKey(file: java.io.File, sizePx: Int) = "file:${file.name}@${bucket(sizePx)}"

    /** Loads an image file from app storage (custom playlist covers). */
    suspend fun loadFile(file: java.io.File, sizePx: Int): ImageBitmap? {
        val k = fileKey(file, sizePx)
        cache.get(k)?.let { return it as? ImageBitmap }
        val px = bucket(sizePx)
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= px && bounds.outHeight / (sample * 2) >= px) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        }
        val img = bmp?.asImageBitmap()
        // Don't cache a miss: the file may still be being written.
        if (img != null) cache.put(k, img)
        return img
    }

    private fun decode(albumId: Long, px: Int): Bitmap? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
            return runCatching { resolver.loadThumbnail(uri, Size(px, px), null) }.getOrNull()
        }
        val uri = Uri.parse("content://media/external/audio/albumart/$albumId")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= px && bounds.outHeight / (sample * 2) >= px) sample *= 2
        return resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}

/**
 * Album cover. Falls back to a flat accent block with a music note, which suits the
 * neubrutalist look better than a grey placeholder.
 */
@Composable
fun Artwork(
    albumId: Long,
    modifier: Modifier = Modifier,
    sizeHint: Dp = 56.dp,
    iconScale: Float = 0.42f,
) {
    val loader = LocalApp.current.artwork
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { sizeHint.roundToPx() }
    val image by produceState(loader.cached(albumId, px), albumId, px) {
        if (value == null) value = loader.load(albumId, px)
    }
    val fill = Neo.colors.accentFor(albumId)
    Box(modifier.background(fill), contentAlignment = Alignment.Center) {
        val img = image
        if (img != null) {
            Image(img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color(0xFF111111),
                modifier = Modifier.fillMaxSize(iconScale),
            )
        }
    }
}
