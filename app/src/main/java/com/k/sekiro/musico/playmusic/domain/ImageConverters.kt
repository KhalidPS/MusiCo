package com.k.sekiro.musico.playmusic.domain

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.k.sekiro.musico.R
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

        return BitmapFactory.decodeResource(resources, R.drawable.logo_musico3)
    }
}




/** This is only ever used to feed androidx.palette's color extraction, which internally
resizes whatever bitmap it's given down to ~112x112 before quantizing anyway - no need to
decode anywhere near full resolution for it.**/
private const val PALETTE_TARGET_SIZE_PX = 200

/** Routed through the app's shared Coil ImageLoader (and its memory/disk cache) rather than
decoding independently: this cover art has almost always already been decoded by an AsyncImage
displaying it elsewhere on screen (song row, bottom bar, PlayedSongScreen's big cover), so this
can reuse that cached bitmap instead of paying for a second decode of the same file.**/
suspend fun convertUriToBitmap(uri: Uri, context: Context, resources: Resources): Bitmap {
    val request = ImageRequest.Builder(context)
        .data(uri)
        .allowHardware(false) // Palette reads raw pixels - HARDWARE bitmaps can't provide that
        .size(PALETTE_TARGET_SIZE_PX, PALETTE_TARGET_SIZE_PX)
        .build()

    return try {
        when (val result = context.imageLoader.execute(request)) {
            is SuccessResult -> result.image.toBitmap()
            is ErrorResult -> {
                Log.e("ks", "${result.throwable.message}")
                BitmapFactory.decodeResource(resources, R.drawable.logo_musico3)
            }
        }
    } catch (ex: Exception) {
        Log.e("ks", "${ex.message}")
        BitmapFactory.decodeResource(resources, R.drawable.logo_musico3)
    }
}