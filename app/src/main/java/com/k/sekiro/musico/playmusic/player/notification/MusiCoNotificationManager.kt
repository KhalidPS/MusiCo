package com.k.sekiro.musico.playmusic.player.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.k.sekiro.musico.MainActivity
import com.k.sekiro.musico.MusicoApp
import com.k.sekiro.musico.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MusiCoNotificationManager(
    private val context: Context,
    private val player: ExoPlayer
): KoinComponent {

    @UnstableApi
    fun startNotificationService(
        mediaSessionService: MediaSessionService,
        mediaSession: MediaSession
    ){

        //buildNotification(mediaSession,mediaSessionService)
        startForegroundNotificationService(mediaSessionService,mediaSession)
    }

    private fun startForegroundNotificationService(mediaSessionService: MediaSessionService,mediaSession: MediaSession){
        Log.e("ks","Enter startForeground fun in MusiCoNotificationManager class ")


/*        val pendingIntent = PendingIntent.getActivity(
            mediaSessionService,
            MusicoApp.NOTIFICATION_ID,
            Intent(mediaSessionService, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )*/

        val notification = NotificationCompat.Builder(context, MusicoApp.NOTIFICATION_CHANNEL_ID)
            //.setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentTitle(mediaSession.player.mediaMetadata.displayTitle)
            .setContentText(mediaSession.player.mediaMetadata.albumArtist)
            .setContentIntent(mediaSession.sessionActivity)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        mediaSessionService.startForeground(MusicoApp.NOTIFICATION_ID,notification)
    }

    @UnstableApi
    private fun buildNotification(mediaSession: MediaSession,mediaSessionService: MediaSessionService){
        PlayerNotificationManager.Builder(
            context,
            MusicoApp.NOTIFICATION_ID,
            MusicoApp.NOTIFICATION_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(
                MusiCoNotificationAdapter(context = context, pendingIntent = mediaSession.sessionActivity)
            )
           // .setNotificationListener(NotificationListener(mediaSessionService))
            .setSmallIconResourceId(R.drawable.ic_audio)
            .build()
            .also {
                it.setMediaSessionToken(mediaSession.platformToken)
                it.setUseFastForwardActionInCompactView(true)
                it.setUseNextActionInCompactView(true)
                it.setUseRewindActionInCompactView(true)
                it.setUsePreviousActionInCompactView(true)
                it.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                it.setPlayer(player)
            }
    }


}