package com.k.sekiro.musico.playmusic.player.service

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.k.sekiro.musico.playmusic.player.notification.MusiCoNotificationManager
import org.koin.android.ext.android.inject


class PlayerSessionService: MediaSessionService() {
    val mediaSession: MediaSession by inject()
    val musiCoNotificationManager: MusiCoNotificationManager by inject()


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        musiCoNotificationManager.startNotificationService(
            mediaSession = mediaSession,
            mediaSessionService = this
        )
    }


    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)

        Log.e("ks","enter service")

        musiCoNotificationManager.startNotificationService(
            mediaSession = mediaSession,
            mediaSessionService = this
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaSession.apply {
            release()
            if (player.playbackState != Player.STATE_IDLE){
                player.seekTo(0)
                player.playWhenReady = false
                player.stop()
            }
        }
    }
}