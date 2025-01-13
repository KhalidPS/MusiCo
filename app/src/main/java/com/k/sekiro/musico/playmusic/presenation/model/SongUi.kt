package com.k.sekiro.musico.playmusic.presenation.model

import android.content.ContentResolver
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Stable
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.Song
import kotlinx.parcelize.Parcelize
import okio.FileNotFoundException
import okio.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

@Stable
data class SongUi(
    val name: String,
    val title: String = "",
    val artist: String = "",
    val cover: Uri,
    val dataUri: Uri,
    val album: String = "",
    val path: String = "",
    val displayableDuration: DisplayableDuration
)

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
        cover = Uri.parse(cover),
        dataUri = Uri.parse(dataUri)
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

fun String.toBitmap(resources: Resources): Bitmap {
    val mmr = MediaMetadataRetriever().apply {
        setDataSource(this@toBitmap)
    }

    Log.e("ks", "before embeddedPicture")
    val data = mmr.embeddedPicture
    Log.e("ks", "after embeddedPicture")

    if (data != null) {
        Log.e("ks", "after embeddedPicture inside if block")
       val bitmap =  BitmapFactory.decodeByteArray(data, 0, data.size)
        mmr.release()
        return bitmap

    } else {
        Log.e("ks", "after embeddedPicture inside else block")

       return BitmapFactory.decodeResource(resources, R.drawable.logo_2)
    }
}


fun convertUriToBitmap(uri: Uri, contentResolver: ContentResolver,resources: Resources): Bitmap {
    var bitmap: Bitmap? = null
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val imageDecoder = ImageDecoder.createSource(contentResolver, uri)

            bitmap = ImageDecoder.decodeBitmap(
                imageDecoder,
                ImageDecoder.OnHeaderDecodedListener{ decoder, info, source ->
                    /**This recommended from official documentation if you want
                       to access the pixels from the final result for example
                       our case, The palette class access the pixels to get the main
                       color for photo so, here without (ImageDecoder.OnHeaderDecodedListener)
                       and these 2 property decoder.allocator, decoder.isMutableRequired
                       we will get exception when the execution arrive Palette code in the PlayedSongScreen
                       isMutableRequired is important here cuz the default returned type is immutable bitmap
                       so it won't be able to access the pixels from immutable one
                       see "https://developer.android.com/reference/kotlin/android/graphics/ImageDecoder"

                     another option without ImageDecoder.OnHeaderDecodedListener is to copy the resul bitmap
                     and change the config and set true for isMutable like this: copy(Bitmap.Config.RGBA_F16,true)
                     but first option is better
                     **/

                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            )/**.copy(Bitmap.Config.RGBA_F16,true)*/
        } else {

            val inputStream = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
            }

            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
            }
        }


        } catch (ex: FileNotFoundException){
            Log.e("ks", "${ex.message}")
            bitmap = BitmapFactory.decodeResource(resources,R.drawable.logo_2)
        } catch (ex: IOException){
            Log.e("ks", "${ex.message}")
        bitmap = BitmapFactory.decodeResource(resources,R.drawable.logo_2)

    } catch (ex: Exception) {
            Log.e("ks", "${ex.message}")
        bitmap = BitmapFactory.decodeResource(resources,R.drawable.logo_2)


    }

    return bitmap!!
}