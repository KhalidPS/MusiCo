package com.k.sekiro.musico.playmusic.presenation.player.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import coil3.Image
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap

@UnstableApi
class MusiCoNotificationAdapter(
    private val context: Context,
    private val pendingIntent: PendingIntent?
) : PlayerNotificationManager.MediaDescriptionAdapter{
    override fun getCurrentContentTitle(player: Player): CharSequence {
        return player.mediaMetadata.albumArtist?:"Unknown"
    }

    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        return pendingIntent
    }

    override fun getCurrentContentText(player: Player): CharSequence? {
        return player.mediaMetadata.displayTitle?:"Unknown"
    }

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        var bitmap: Bitmap? = null
        // Reuse the app's shared Coil ImageLoader (and its memory/disk cache) instead of
        // constructing a fresh one - a new ImageLoader on every notification update has an
        // empty cache and can't benefit from artwork already decoded elsewhere in the app.
        val imageLoader = context.imageLoader
        val request = ImageRequest.Builder(
            context
        )
            .data(player.mediaMetadata.artworkUri)
            //.allowHardware(false)
            .target(
                onSuccess = {
                    val bitmapResult = it.toBitmap()
                    callback.onBitmap(bitmapResult)
                   bitmap =  bitmapResult
                },
                onError = {
                   bitmap = null
                },
            )
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()

        val disposable = imageLoader.enqueue(request)

        return bitmap
    }
}