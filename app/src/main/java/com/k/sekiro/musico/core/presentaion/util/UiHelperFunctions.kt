package com.k.sekiro.musico.core.presentaion.util

import android.content.Context
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.palette.graphics.Palette
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.convertUriToBitmap
import com.k.sekiro.musico.playmusic.presenation.model.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


fun Modifier.applyIf(
    condition: Boolean,
    modifier:Modifier.() -> Modifier
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
    song: SongUi,
): Color = withContext(Dispatchers.Default) {

    val songCover = convertUriToBitmap(
        song.cover.toUri(),
        context.contentResolver,
        context.resources
    )


    val palette = if (lurCache[song.path] != null) {
        lurCache[song.path]!!
    } else {
        Palette.from(songCover).generate().apply {
            lurCache.put(song.path, this)
        }
    }

        if (palette.vibrantSwatch != null) Color(
            palette.vibrantSwatch!!.rgb
        ) else if (palette.lightVibrantSwatch != null) {
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



