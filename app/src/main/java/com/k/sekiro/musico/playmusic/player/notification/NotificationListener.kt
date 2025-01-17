package com.k.sekiro.musico.playmusic.player.notification

import android.app.Notification
import android.app.Service
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.k.sekiro.musico.playmusic.player.service.PlayerSessionService

@UnstableApi
class NotificationListener(private val service: MediaSessionService) : PlayerNotificationManager.NotificationListener{
    override fun onNotificationCancelled(
        notificationId: Int,
        dismissedByUser: Boolean
    ) {
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        service.stopSelf()
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        service.startForeground(notificationId,notification)
    }
}