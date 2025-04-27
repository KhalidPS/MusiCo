package com.k.sekiro.musico.core.presentaion.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes

fun convertResToBitmap(context:Context,@DrawableRes resId: Int): Bitmap{
    return BitmapFactory.decodeResource(context.resources,resId)
}

fun isUriValid(context: Context,uri: Uri): Boolean{
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