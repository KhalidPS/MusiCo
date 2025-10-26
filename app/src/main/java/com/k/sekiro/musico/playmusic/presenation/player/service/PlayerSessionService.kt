package com.k.sekiro.musico.playmusic.presenation.player.service

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.k.sekiro.musico.MainActivity
import com.k.sekiro.musico.MusicoApp
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver
import com.k.sekiro.musico.playmusic.domain.model.INDEX_KEY
import com.k.sekiro.musico.playmusic.domain.model.PATH_KEY
import com.k.sekiro.musico.playmusic.domain.model.PROGRESS_KEY
import com.k.sekiro.musico.playmusic.domain.model.PlayMode_KEY
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.model.RecentSongs_KEY
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistSongRepository
import com.k.sekiro.musico.playmusic.presenation.PlayType
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.player.notification.CUSTOM_COMMAND_REPEAT_ALL_ACTION
import com.k.sekiro.musico.playmusic.presenation.player.notification.MusiCoNotificationManager
import com.k.sekiro.musico.playmusic.presenation.player.notification.NotificationPlayerCustomCommand
import com.k.sekiro.musico.playmusic.presenation.player.startProgressUpdate
import com.k.sekiro.musico.playmusic.presenation.player.stopProgressUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject


class PlayerSessionService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var musiCoNotificationManager: MusiCoNotificationManager? = null
    private val dataSaver: SimpleDataSaver by inject()

    private val playlistSongRepo: PlaylistSongRepository by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var job: Job? = null
    private var playModeJob: Job? = null

    private val notificationPlayerCustomCommandButtons =
        NotificationPlayerCustomCommand.entries.map { it.commandButton }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            Log.e("ks","service onMediaItemTransition")
            super.onMediaItemTransition(mediaItem, reason)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.e("ks","service onIsPlayingChanged")

            if (!isPlaying) {
                job?.cancel()
                job = scope.launch {
                    delay(500)
                    launch (Dispatchers.Main){ saveCurrentSongData() }
                    if (isSelectedFromPlaylist){
                        launch { saveRecentPlaylistSongs() }
                    }

                }
            }

        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)

            playModeJob?.cancel()
            when (repeatMode) {
                Player.REPEAT_MODE_ONE -> {
                   playModeJob =  scope.launch {
                       delay(500)
                       dataSaver.suspendSave(PlayMode_KEY, PlayType.RepeatOne.name)
                   }
                }
                Player.REPEAT_MODE_ALL -> {
                    playModeJob = scope.launch {
                        delay(500)
                        dataSaver.suspendSave(PlayMode_KEY, PlayType.RepeatAll.name)
                    }
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled)
            playModeJob?.cancel()
            if (shuffleModeEnabled) {
               playModeJob =  scope.launch {
                   delay(500)
                   dataSaver.suspendSave(PlayMode_KEY, PlayType.Shuffle.name)
               }
            }
        }
    }



    private val sessionCallback = object : MediaSession.Callback {
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

        player.addListener(listener)

        val forwardingPlayer = object : ForwardingSimpleBasePlayer(player){
            override fun handleSeek(
                mediaItemIndex: Int,
                positionMs: Long,
                seekCommand: Int
            ): ListenableFuture<*> {

                var internalSeekCommand = seekCommand
                val mediaItem: MediaItem? = try {
                    getMediaItemAt(mediaItemIndex)
                }catch (ex: IndexOutOfBoundsException){
                    currentMediaItem
                }


                val songId = mediaItem?.mediaMetadata?.discNumber

                when(seekCommand){

                    COMMAND_SEEK_TO_PREVIOUS -> {
                        internalSeekCommand = COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                    }

                    COMMAND_SEEK_TO_NEXT -> {
                        internalSeekCommand = COMMAND_SEEK_TO_NEXT_MEDIA_ITEM

                    }
                }

                if (songId != null) {
                    scope.launch { playlistSongRepo.addPlaylistSongRef(PlaylistSong(2,songId.toLong())) }
                }

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
            Log.e("ks","kill service after remove app")
        }
        Log.e("ks", "remove app from background")


        saveCurrentSongData()
        if (isSelectedFromPlaylist){
            saveRecentPlaylistSongs()
        }



        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            player.removeListener(listener)
            player.release()
            Log.e("ks", "Service Destroyed ^_^")

            scope.cancel()
            //mediaSession = null
        }

        super.onDestroy()
    }


    private fun saveCurrentSongData(){
        val currentSong = mediaSession!!.player.currentMediaItemIndex
        val currentProgress = mediaSession!!.player.currentPosition
        val path = mediaSession!!.player.currentMediaItem!!.mediaId

        scope.launch(Dispatchers.IO) {
            dataSaver.suspendSave(
                INDEX_KEY to currentSong,
                PROGRESS_KEY to currentProgress,
                PATH_KEY to path
            )
        }


        Log.e("ks","mediaId in service :${path}")
        Log.e("ks","uri in service : ${mediaSession!!.player!!.currentMediaItem?.localConfiguration?.uri.toString()}")
        Log.e("ks","is uri equal mediaId : ${path == mediaSession!!.player!!.currentMediaItem?.localConfiguration?.uri.toString()}")
    }


    private fun saveRecentPlaylistSongs() {
        scope.launch(Dispatchers.IO){
            val stringList = Json.encodeToString(recentPlaylist)
            dataSaver.suspendSave(RecentSongs_KEY, stringList)

        }
    }

    companion object{
        private var recentPlaylist: List<SongUi> = emptyList()
        private var isSelectedFromPlaylist = false
        var isAlive = false

        fun syncRecentPlaylist(songs:List<SongUi>,isSelected:Boolean){
            recentPlaylist = songs
            isSelectedFromPlaylist = isSelected
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
