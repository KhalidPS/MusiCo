package com.k.sekiro.musico.playmusic.presenation.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.k.sekiro.musico.playmusic.domain.convertUriToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Single entry point PlayerSessionService calls to push playback state into the widget -
kept as a stateless object since it has no dependencies beyond a Context, which every call
site already has. Not Koin-wired: nothing here needs the DI graph.**/
object WidgetUpdater {

    suspend fun push(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean,
        isFavorite: Boolean,
        coverUri: Uri,
    ) {
        val coverPath = writeCoverCache(context, coverUri)

        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MusiCoWidget::class.java)
        if (glanceIds.isEmpty()) return

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[WidgetStateKeys.HasState] = true
                prefs[WidgetStateKeys.Title] = title
                prefs[WidgetStateKeys.Artist] = artist
                prefs[WidgetStateKeys.IsPlaying] = isPlaying
                prefs[WidgetStateKeys.IsFavorite] = isFavorite
                prefs[WidgetStateKeys.CoverPath] = coverPath
                prefs[WidgetStateKeys.CoverVersion] = (prefs[WidgetStateKeys.CoverVersion] ?: 0) + 1
            }
        }
        MusiCoWidget().updateAll(context)
    }

    /** Single fixed filename, overwritten each push - only one "current" cover is ever
    shown, so there's no reason to keep old ones around.**/
    private suspend fun writeCoverCache(context: Context, coverUri: Uri): String {
        return try {
            val bitmap = convertUriToBitmap(coverUri, context, context.resources)
            withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "widget").apply { mkdirs() }
                val file = File(dir, "cover.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                file.absolutePath
            }
        } catch (ex: Exception) {
            Log.e("ks", "widget cover cache write failed: ${ex.message}")
            ""
        }
    }
}
