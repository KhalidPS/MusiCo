package com.k.sekiro.musico.playmusic.presenation.model

import android.content.ContentResolver
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Stable
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.Song
import kotlinx.serialization.Serializable
import okio.FileNotFoundException
import okio.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
@Stable
data class SongUi(
    val name: String,
    val title: String = "",
    val artist: String = "",
    val cover: String,
    val dataUri: String,
    val album: String = "",
    val path: String = "",
    val addedDate: Long = 0L,
    val displayableDuration: DisplayableDuration,
    val lastModified: Long = 0L,
    val id: Long = 0
    ){

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (javaClass != other?.javaClass) false
        else this.path == (other as SongUi).path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

@Serializable
data class DisplayableDuration(
    val durationMillis: Long,
    val formatted: String,
)

fun Song.toSongUi(): SongUi {
    return SongUi(
        name = name,
        title = title,
        artist = artist,
        album = album,
        path = path,
        displayableDuration = duration.toDisplayableDuration(),
        cover = cover?:"",
        addedDate = addedDate,
        dataUri = dataUri,
        lastModified = lastModified,
        id = id
    )
}

fun Long.toDisplayableDuration(): DisplayableDuration {
    return DisplayableDuration(
        durationMillis = this,
        formatted = fromMillis(this)
    )
}

fun fromMillis(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return if (hours > 0){
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }else{
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

