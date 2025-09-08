package com.k.sekiro.musico.playmusic.presenation.util

import android.content.Context
import android.net.Uri
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.palette.graphics.Palette
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.util.convertUriToBitmap
import kotlinx.coroutines.Dispatchers
import androidx.core.net.toUri
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun Modifier.applyIf(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
): Modifier {
    return then(
        if (condition) {
            modifier()
        } else {
            Modifier
        }
    )
}

@Composable
fun Modifier.applyIfComposable(
    condition: Boolean,
    modifier: @Composable Modifier.() -> Modifier
): Modifier {
    return then(
        if (condition) {
            modifier()
        } else {
            Modifier
        }
    )
}

fun Dp.toPx(density: Float) = this.value * density

suspend fun getColorFromCover(
    lurCache: LruCache<String, Palette>,
    context: Context,
    cover: String,
    path: String
): Color = coroutineScope {

    val songCover = async {
        convertUriToBitmap(
            cover.toUri(),
            context.contentResolver,
            context.resources
        )
    }


    val palette = if (lurCache[path] != null) {
        lurCache[path]!!
    } else {
        Palette.from(songCover.await()).generate().apply {
            lurCache.put(path, this)
        }
    }

    if (palette.vibrantSwatch != null) {
        Color(palette.vibrantSwatch!!.rgb)
    } else if (palette.lightVibrantSwatch != null) {
        Color(palette.lightVibrantSwatch!!.rgb)
    } else if (palette.darkVibrantSwatch != null) {
        Color(palette.darkVibrantSwatch!!.rgb)
    } else if (palette.lightMutedSwatch != null) {
        Color(palette.lightMutedSwatch!!.rgb)
    } else if (palette.mutedSwatch != null) {
        Color(palette.mutedSwatch!!.rgb)
    } else if (palette.darkMutedSwatch != null) {
        Color(palette.darkMutedSwatch!!.rgb)
    } else Color.Cyan

}


@Composable
fun rememberDominantColor(
    song: SongUi,
    lruCache: LruCache<String, Palette>,
    defaultColor: Color = Color.Cyan
): State<Color> {
    val context = LocalContext.current
    val dominantColor = remember(song.path) { mutableStateOf(defaultColor) }

    LaunchedEffect(song.path) {
        val cachedPalette = lruCache[song.path]
        if (cachedPalette != null) {
            dominantColor.value = cachedPalette.getDominantColor()
        } else {
            val songCoverBitmap = async(Dispatchers.IO) {
                convertUriToBitmap(song.cover.toUri(), context.contentResolver, context.resources)
            }
                val palette = Palette.from(songCoverBitmap.await()).generate()
                lruCache.put(song.path, palette)
                // Extract color from new palette and update dominantColor.value
                // ... (similar logic as you have)
                withContext(Dispatchers.Main.immediate) {
                    dominantColor.value = palette.getDominantColor()
                }

        }
    }
    return dominantColor
}

fun Palette.getDominantColor(defaultColor: Color = Color.Cyan): Color {
    return listOfNotNull(
        vibrantSwatch,
        lightVibrantSwatch,
        darkVibrantSwatch,
        lightMutedSwatch,
        mutedSwatch,
        darkMutedSwatch
    ).firstOrNull()?.rgb?.let { Color(it) } ?: defaultColor
}



