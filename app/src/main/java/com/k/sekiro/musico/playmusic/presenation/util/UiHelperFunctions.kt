package com.k.sekiro.musico.playmusic.presenation.util

import android.content.Context
import android.net.Uri
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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



