package com.k.sekiro.musico.playmusic.data.local

import android.graphics.Bitmap
import android.util.LruCache
import androidx.palette.graphics.Palette
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject

class PaletteCache (
    private val lruCache: LruCache<String, Palette>
){


   /* constructor(maxSize: Int = 4*1024*1024) {
        lruCache = LruCache(maxSize)
    }*/

    fun getPalette(imageId: String): Palette? {
        return lruCache.get(imageId)
    }

    fun putPalette(imageId: String, palette: Palette) {
        lruCache.put(imageId, palette)
    }

    fun createAndCachePalette(imageId: String, bitmap: Bitmap): Palette {
        val palette = Palette.from(bitmap).generate()
        putPalette(imageId, palette)
        return palette
    }
}