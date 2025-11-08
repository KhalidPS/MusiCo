package com.k.sekiro.musico.playmusic.domain

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import com.k.sekiro.musico.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import okio.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.core.net.toUri

fun convertResToBitmap(context:Context,@DrawableRes resId: Int): Bitmap{
    return BitmapFactory.decodeResource(context.resources,resId)
}


/** make this suspend to take aware of main safety principle even I'm not using suspension functions
inside it or wrapping it with withContext to switch to IO dispatcher cuz I'm calling this fun
in coroutine that run in IO dispatcher already, so this fun will inherit the context from parent coroutine, but the
reason for making this suspending cuz openInputStream is blocking call and if this is not suspended fun
even if I'm calling it inside coroutine it may block the whole thread that the coroutines are running on
then block all siblings coroutines inside that thread, but by making it suspend it would only block
the coroutine that this function has been called inside it**/
suspend inline fun isValidUri(context: Context,uri: Uri) = suspendCoroutine { continuation ->
    try {
        context.contentResolver.openInputStream(uri)?.close()
        continuation.resume(true)
    }catch (e: Exception){
        continuation.resume(false)
    }

    }

fun getUriFromDrawable(context: Context,drawableId: Int): Uri{
    return "android.resource://${context.packageName}/$drawableId".toUri()
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




suspend fun convertUriToBitmap(uri: Uri, contentResolver: ContentResolver,resources: Resources): Bitmap
= withContext(Dispatchers.Default){
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

    bitmap!!
}