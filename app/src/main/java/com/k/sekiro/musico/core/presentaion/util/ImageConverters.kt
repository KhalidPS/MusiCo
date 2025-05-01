package com.k.sekiro.musico.core.presentaion.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes

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
suspend fun isValidUri(context: Context,uri: Uri): Boolean{
    return try {
        context.contentResolver.openInputStream(uri)?.close()
        true
    }catch (e: Exception){
        false
    }
}

fun getUriFromDrawable(context: Context,drawableId: Int): Uri{
    return Uri.parse("android.resource://${context.packageName}/$drawableId")
}