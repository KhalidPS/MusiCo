package com.k.sekiro.musico.playmusic.player.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
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
        Log.e("ks","create service......")
    }


    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.e("ks", "start command with intent : $intent")



        return super.onStartCommand(intent, flags, startId)

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }




    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession.player
        if (
            !player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED
        ) {
            // Stop the service if not playing, continue playing in the background otherwise.
            //stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.e("ks","remove app from background")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.run {
            release()
            player.release()
            Log.e("ks","Service Destroyed ^_^")
            //mediaSession = null
        }
    }

    /*
    override fun onDestroy() {
        Log.e("ks","service destroyed")
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
}*/

}
