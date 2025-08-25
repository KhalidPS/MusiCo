package com.k.sekiro.musico.playmusic.player.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.k.sekiro.musico.MainActivity
import com.k.sekiro.musico.MusicoApp
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistSongRepository
import com.k.sekiro.musico.playmusic.player.notification.CUSTOM_COMMAND_REPEAT_ALL_ACTION
import com.k.sekiro.musico.playmusic.player.notification.CUSTOM_COMMAND_REPEAT_ONE_ACTION
import com.k.sekiro.musico.playmusic.player.notification.MusiCoNotificationManager
import com.k.sekiro.musico.playmusic.player.notification.NotificationPlayerCustomCommand
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


class PlayerSessionService : MediaSessionService() {
    var mediaSession: MediaSession? = null
    var musiCoNotificationManager: MusiCoNotificationManager? = null
    val sharedPref: SharedPreferences by inject()

    val playlistSongRepo: PlaylistSongRepository by inject()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val notificationPlayerCustomCommandButtons =
        NotificationPlayerCustomCommand.entries.map { it.commandButton }


    val sessionCallback = object : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommand = connectionResult.availableSessionCommands.buildUpon()
            notificationPlayerCustomCommandButtons.forEach { commandButton ->
                commandButton.sessionCommand?.let(availableSessionCommand::add)
            }
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommand.build())
                .build()
            /* return MediaSession.ConnectionResult.accept(
                 availableSessionCommand.build(),
                 connectionResult.availablePlayerCommands
             )*/
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            super.onPostConnect(session, controller)
        }


        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {

            when (customCommand.customAction) {

                /*                NotificationPlayerCustomCommand.REWIND.customAction -> {
                                    session.player.seekBack()
                                    Log.e("ks", "Rewind clicked")
                                }*/

                /*
                                NotificationPlayerCustomCommand.FORWARD.customAction -> {
                                    session.player.seekForward()
                                    Log.e("ks", "Forward clicked")
                                }
                */

                NotificationPlayerCustomCommand.FAVORITE.customAction -> {

                    Log.e("ks", "Favorite clicked")
                }

                NotificationPlayerCustomCommand.REPEAT_ONE.customAction -> {

                    mediaSession!!.setCustomLayout(
                        ImmutableList.of(
                            notificationPlayerCustomCommandButtons[0],
                            notificationPlayerCustomCommandButtons[3]
                        )
                    )
                    
                    Log.e("ks","Repeat one clicked >>> now repeat all")

                    session.player.repeatMode = Player.REPEAT_MODE_OFF
                    session.player.shuffleModeEnabled = true
                }


                CUSTOM_COMMAND_REPEAT_ALL_ACTION -> {
                    session.setCustomLayout(
                        ImmutableList.of(
                            notificationPlayerCustomCommandButtons[0],
                            notificationPlayerCustomCommandButtons[2]
                            )
                    )


                    session.player.shuffleModeEnabled = false
                    session.player.repeatMode = Player.REPEAT_MODE_ONE
                }

                NotificationPlayerCustomCommand.SHUFFLE.customAction -> {
                    session.setCustomLayout(
                        ImmutableList.of(
                            notificationPlayerCustomCommandButtons[0],
                            notificationPlayerCustomCommandButtons[1]
                        )
                    )


                    session.player.shuffleModeEnabled = false
                    session.player.repeatMode = Player.REPEAT_MODE_ALL
                }

            }

            Log.e("ks","custom Action is ${customCommand.customAction}")

            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        isAlive = false


        scope.launch {
            delay(2000)
            isAlive = true
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            MusicoApp.NOTIFICATION_ID,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_RUN
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(DefaultTrackSelector(this))
            .build()

        val forwardingPlayer = object : ForwardingSimpleBasePlayer(player){
            override fun handleSeek(
                mediaItemIndex: Int,
                positionMs: Long,
                seekCommand: Int
            ): ListenableFuture<*> {

                var internalSeekCommand = seekCommand
                val mediaItem = getMediaItemAt(mediaItemIndex)
                val songId = mediaItem.mediaMetadata.discNumber!!.toLong()

                when(seekCommand){

                    COMMAND_SEEK_TO_PREVIOUS -> {
                        internalSeekCommand = COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                    }

                    COMMAND_SEEK_TO_NEXT -> {
                        internalSeekCommand = COMMAND_SEEK_TO_NEXT_MEDIA_ITEM

                    }
                }

                scope.launch { playlistSongRepo.addPlaylistSongRef(PlaylistSong(2,songId)) }

                return super.handleSeek(mediaItemIndex, positionMs, internalSeekCommand)
            }
        }

        musiCoNotificationManager = MusiCoNotificationManager(this, forwardingPlayer)

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .setCustomLayout(notificationPlayerCustomCommandButtons)
            .build()

  /*      musiCoNotificationManager!!.startNotificationService(
            mediaSession = mediaSession!!,
            mediaSessionService = this
        )*/
        Log.e("ks", "create service......")
    }


    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.e("ks", "start command with intent : $intent")



        return super.onStartCommand(intent, flags, startId)

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.e("ks","onGetSession....")
        return mediaSession
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession!!.player
        if (
            !player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED
        ) {

            // Stop the service if not playing, continue playing in the background otherwise.
            //stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.e("ks", "remove app from background")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val currentSong = mediaSession!!.player.currentMediaItemIndex
        val currentProgress = mediaSession!!.player.currentPosition
        val path = mediaSession!!.player.currentMediaItem!!.mediaId
        sharedPref.edit().apply {
            putInt("index", currentSong)
            putLong("progress", currentProgress)
            putString("path",path)
            
            Log.e("ks","mediaId in service :${path}")
            Log.e("ks","uri in service : ${mediaSession!!.player!!.currentMediaItem?.localConfiguration?.uri.toString()}")
            Log.e("ks","is uri equal mediaId : ${path == mediaSession!!.player!!.currentMediaItem?.localConfiguration?.uri.toString()}")
            apply()
        }

        mediaSession?.run {
            release()
            player.release()
            Log.e("ks", "Service Destroyed ^_^")

            scope.cancel()
            //mediaSession = null
        }
    }


    companion object{
        var isAlive = false
        private var currentPlaylistSongs: List<SongUi>? = null

        fun setCurrentPlaylistSong(songs: List<SongUi>){
            currentPlaylistSongs = songs
        }


        fun getCurrentPlaylistSongs() = currentPlaylistSongs
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
