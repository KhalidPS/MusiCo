package com.k.sekiro.musico.core.presentaion.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize

fun convertResToBitmap(context:Context,@DrawableRes resId: Int): Bitmap{
    return BitmapFactory.decodeResource(context.resources,resId)
}

@Composable
fun BlurredImage(modifier: Modifier,bitmap: Bitmap, blurRadius: Float) {
    Canvas(modifier = modifier) {
        // Draw the blurred bitmap on the canvas
        drawImage(
            image = blurBitmap(bitmap, blurRadius).asImageBitmap(),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}

fun blurBitmap(bitmap: Bitmap, blurRadius: Float): Bitmap {
    // Create a new bitmap for the blurred image
    val blurredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

    // Create a canvas to draw on the blurred bitmap
    val canvas = Canvas(blurredBitmap)

    // Apply the blur effect using a Paint object with a BlurMaskFilter
    val paint = Paint()
    paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
    canvas.drawBitmap(bitmap, 20f, 20f, paint)

    return blurredBitmap
}

 fun legacyBlurImage(
    bitmap: Bitmap,
    blurRadio: Float,
    context: Context
) : Bitmap{

    val renderScript = RenderScript.create(context)
    val bitmapAlloc = Allocation.createFromBitmap(renderScript, bitmap)
    ScriptIntrinsicBlur.create(renderScript, bitmapAlloc.element).apply {
        setRadius(blurRadio)
        setInput(bitmapAlloc)
        forEach(bitmapAlloc)
    }
    bitmapAlloc.copyTo(bitmap)
    renderScript.destroy()

    return bitmap
}


