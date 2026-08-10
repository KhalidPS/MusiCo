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
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver
import com.k.sekiro.musico.playmusic.domain.model.INDEX_KEY
import com.k.sekiro.musico.playmusic.domain.model.PATH_KEY
import com.k.sekiro.musico.playmusic.domain.model.PROGRESS_KEY
import com.k.sekiro.musico.playmusic.domain.model.PlayMode_KEY
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.model.RecentSongsIds_KEY
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistSongRepository
import com.k.sekiro.musico.playmusic.presenation.PlayType
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.player.notification.CUSTOM_COMMAND_REPEAT_ALL_ACTION
import com.k.sekiro.musico.playmusic.presenation.player.notification.MusiCoNotificationManager
import com.k.sekiro.musico.playmusic.presenation.player.notification.NotificationPlayerCustomCommand
import com.k.sekiro.musico.playmusic.presenation.player.startProgressUpdate
import com.k.sekiro.musico.playmusic.presenation.player.stopProgressUpdate
import com.k.sekiro.musico.playmusic.presenation.widget.WidgetUpdater
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
import kotlinx.coroutines.withContext
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

    private var isFavorite: Boolean = false

    private val notificationPlayerCustomCommandButtons =
        NotificationPlayerCustomCommand.entries.map { it.commandButton }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            val songId = mediaItem?.mediaMetadata?.discNumber?.let { it.toLong() } ?: return

            scope.launch {
                isFavorite = isFavorite(songId)
                Log.e("ks","isFavorite in service: $isFavorite")
                    withContext(Dispatchers.Main){
                        if (isFavorite){
                            updateNotificationToFavorite()
                        }else{
                            updateNotificationToUnFavorite()
                        }
                    }

                // Pushed here (after isFavorite resolves), not from a separate listener
                // reacting to the same onMediaItemTransition event - a separate listener
                // would race this async lookup and could push the *previous* song's
                // isFavorite value alongside the new song's title/artist/cover.
                pushWidgetState()

            }
            Log.e("ks","service onMediaItemTransition")
            super.onMediaItemTransition(mediaItem, reason)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.e("ks","service onIsPlayingChanged")
            pushWidgetState()

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

            val songId = session.player.mediaMetadata.discNumber?.let { it.toLong() }
            val favoritePlaylistId = 1L

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


                    updateNotificationToUnFavorite()
                    if (songId != null){
                        scope.launch {
                            playlistSongRepo.deletePlaylistSongRef(PlaylistSong(favoritePlaylistId,songId))
                            isFavorite = false
                            pushWidgetState()
                        }
                    }
                }


                NotificationPlayerCustomCommand.UNFAVORITE.customAction ->{
                    updateNotificationToFavorite()
                    if (songId != null){
                        scope.launch {
                            playlistSongRepo.addPlaylistSongRef(PlaylistSong(favoritePlaylistId,songId))
                            isFavorite = true
                            pushWidgetState()
                        }
                    }
                }

                NotificationPlayerCustomCommand.REPEAT_ONE.customAction -> {

                    mediaSession!!.setCustomLayout(
                        ImmutableList.of(
                            favoriteOrUnFavoriteCommand(isFavorite),
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
                            favoriteOrUnFavoriteCommand(isFavorite),
                            notificationPlayerCustomCommandButtons[2]
                            )
                    )


                    session.player.shuffleModeEnabled = false
                    session.player.repeatMode = Player.REPEAT_MODE_ONE
                }

                NotificationPlayerCustomCommand.SHUFFLE.customAction -> {
                    session.setCustomLayout(
                        ImmutableList.of(
                            favoriteOrUnFavoriteCommand(isFavorite),
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



    }

    override fun onDestroy() {
        mediaSession?.run {
            // Snapshot before releasing anything - once the session/player are released
            // there's nothing left to read the last-playing song's info from.
            val teardownMediaItem = player.currentMediaItem
            val teardownCoverUri = teardownMediaItem?.mediaMetadata?.artworkUri
            val teardownTitle = teardownMediaItem?.mediaMetadata?.displayTitle?.toString().orEmpty()
            val teardownArtist = teardownMediaItem?.mediaMetadata?.artist?.toString().orEmpty()
            val teardownIsFavorite = isFavorite

            release()
            player.removeListener(listener)
            player.release()
            Log.e("ks", "Service Destroyed ^_^")

            // scope.cancel() below would cancel a push launched on it before it ever runs
            // (launch only schedules, it doesn't run synchronously) - this teardown push
            // needs its own short-lived scope, independent of the one about to be cancelled.
            if (teardownCoverUri != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    WidgetUpdater.push(
                        context = this@PlayerSessionService,
                        title = teardownTitle,
                        artist = teardownArtist,
                        isPlaying = false,
                        isFavorite = teardownIsFavorite,
                        coverUri = teardownCoverUri
                    )
                }
            }

            scope.cancel()
            //mediaSession = null
        }

        super.onDestroy()
    }


    /** Reads current title/artist/cover straight off the player's currentMediaItem rather
    than threading them through as parameters - every call site (onIsPlayingChanged, the
    resolved-isFavorite branch of onMediaItemTransition, the two favorite custom-command
    branches) already has a live mediaSession to read from. No-ops if there's no media item
    or artwork yet (nothing meaningful to show).

    Some call sites (the two above that are themselves inside a scope.launch on `scope`,
    which defaults to Dispatchers.IO) invoke this from a background thread, but Player must
    only ever be touched on its application thread (main, here) - hence the explicit hop to
    Dispatchers.Main before reading anything off the player, then back to Dispatchers.IO for
    WidgetUpdater.push's actual file/bitmap/DataStore work.**/
    private fun pushWidgetState() {
        scope.launch(Dispatchers.Main) {
            val player = mediaSession?.player ?: return@launch
            val mediaItem = player.currentMediaItem ?: return@launch
            val artworkUri = mediaItem.mediaMetadata.artworkUri ?: return@launch
            // mediaMetadata.title actually holds the artist (see setMediaItemsList in
            // PlaybackExtenstions.kt) - displayTitle/artist are the correct fields to read.
            val title = mediaItem.mediaMetadata.displayTitle?.toString().orEmpty()
            val artist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
            val playing = player.isPlaying
            val favorite = isFavorite

            withContext(Dispatchers.IO) {
                WidgetUpdater.push(
                    context = this@PlayerSessionService,
                    title = title,
                    artist = artist,
                    isPlaying = playing,
                    isFavorite = favorite,
                    coverUri = artworkUri
                )
            }
        }
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
            val stringList = Json.encodeToString(recentPlaylistSongIds)
            dataSaver.suspendSave(RecentSongsIds_KEY, stringList)

        }
    }

    companion object{
        private var recentPlaylistSongIds: List<Long> = emptyList()
        private var isSelectedFromPlaylist = false
        var isAlive = false

        fun syncRecentPlaylist(songs:List<SongUi>,isSelected:Boolean){
            recentPlaylistSongIds = songs.map { it.id }
            isSelectedFromPlaylist = isSelected
        }

    }

    private fun isShuffle(): Boolean{
        if (mediaSession == null) return false
        return mediaSession!!.player.shuffleModeEnabled
    }

    private fun isRepeatAll(): Boolean{
        if (mediaSession == null) return false
        return !isShuffle() && mediaSession!!.player.repeatMode == Player.REPEAT_MODE_ALL
    }


    private fun updateNotificationToFavorite(){
        if (isShuffle()){
            mediaSession!!.setCustomLayout(
                ImmutableList.of(
                    notificationPlayerCustomCommandButtons[0],
                    notificationPlayerCustomCommandButtons[3]
                )
            )
        }else if (isRepeatAll()){
            mediaSession!!.setCustomLayout(
                ImmutableList.of(
                    notificationPlayerCustomCommandButtons[0],
                    notificationPlayerCustomCommandButtons[1]
                )
            )
        }else{
            mediaSession!!.setCustomLayout(
                ImmutableList.of(
                    notificationPlayerCustomCommandButtons[0],
                    notificationPlayerCustomCommandButtons[2]
                )
            )
        }
    }


    private fun updateNotificationToUnFavorite(){
        if (isShuffle()){
            mediaSession!!.setCustomLayout(
                ImmutableList.of(
                    notificationPlayerCustomCommandButtons[4],
                    notificationPlayerCustomCommandButtons[3]
                )
            )
        }else if (isRepeatAll()){
            mediaSession!!.setCustomLayout(
                ImmutableList.of(
                    notificationPlayerCustomCommandButtons[4],
                    notificationPlayerCustomCommandButtons[1]
                )
            )
        }else{
            mediaSession!!.setCustomLayout(
                ImmutableList.of(
                    notificationPlayerCustomCommandButtons[4],
                    notificationPlayerCustomCommandButtons[2]
                )
            )
        }
    }

    private fun favoriteOrUnFavoriteCommand(isFavorite: Boolean): CommandButton{
        return if (isFavorite) notificationPlayerCustomCommandButtons[0]
        else notificationPlayerCustomCommandButtons[4]
    }

    private suspend fun isFavorite(songId: Long): Boolean {
       return playlistSongRepo.getPlaylistSongRef(1,songId) != null
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
