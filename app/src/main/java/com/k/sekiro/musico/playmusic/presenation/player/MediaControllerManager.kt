package com.k.sekiro.musico.playmusic.presenation.player

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.k.sekiro.musico.playmusic.presenation.PlayType
import com.k.sekiro.musico.playmusic.presenation.ViewModel
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.player.service.PlayerSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.collections.indexOf

class MediaControllerManager(
    private val context: Context,
    private val viewModel: ViewModel,
    private val sharedPreferences: SharedPreferences,
    private val coroutineScope: CoroutineScope
) : LifecycleEventObserver{

    private var controller: MediaController? = null
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var isNewCreation = true


    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> viewModel.calculateProgressValue(controller!!.currentPosition)
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            viewModel.updateIsPlaying(isPlaying)
            //viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
            if (isPlaying) {
                coroutineScope.launch {
                    controller!!.startProgressUpdate(viewModel::calculateProgressValue)
                }
            } else {
                stopProgressUpdate(viewModel::updateIsPlaying)
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)

            Log.e("ks", "indx onMediaMetadataChanged :${controller!!.currentMediaItemIndex}")
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)

        }


        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)

            when (repeatMode) {
                Player.REPEAT_MODE_ONE -> viewModel.updatePlayType(PlayType.RepeatOne)
                Player.REPEAT_MODE_ALL -> viewModel.updatePlayType(PlayType.RepeatAll)
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled)
            if (shuffleModeEnabled) {
                viewModel.updatePlayType(PlayType.Shuffle)
            }
        }


    }



    fun initialize(){
        isNewCreation = true
        val sessionToken = SessionToken(
            context, ComponentName(
                context,
                PlayerSessionService::class.java
            )
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                if (controllerFuture.isDone) {
                    controller = controllerFuture.get()
                    controller?.addListener(listener)
                    Log.e("ks", "MediaController : $controller")
                    Log.e("ks", "MediaController : $controller")
                }
            }, MoreExecutors.directExecutor()
        )
    }



    suspend fun controllerAndLastPlayedSongSetup(allSongs: List<SongUi>) {
        val controller = this.controller ?: return
        val songs = getRelevantSongsList(allSongs)

        if (songs.isEmpty()) return

        when {
            isNewCreation -> handleNewCreationSetup(controller, songs)
            shouldUpdateMediaItemsForNewSongs(controller, songs) -> handleNewSongsAdded(
                controller,
                songs
            )
        }
    }

    private fun getRelevantSongsList(allSongs: List<SongUi>): List<SongUi> {
        return if (viewModel.isSelectedSongFromPlaylist()) {
            viewModel.currentPlaylistSongs()
        } else {
            allSongs
        }
    }

    private suspend fun handleNewCreationSetup(
        controller: MediaController,
        songs: List<SongUi>
    ) {
        val savedPath = sharedPreferences.getString("path", "") ?: ""

        when {
            controller.isPlaying -> handlePlayingControllerSetup(controller, songs)
            isServiceInactiveWithSavedPath(savedPath) -> handleInactiveServiceSetup(
                controller,
                songs,
                savedPath
            )

            PlayerSessionService.isAlive && !controller.isPlaying -> handleActiveServicePausedSetup(
                controller,
                songs
            )

            else -> handleFirstTimeAppLaunch(controller, songs)
        }

        isNewCreation = false
    }

    private suspend fun handlePlayingControllerSetup(
        controller: MediaController,
        songs: List<SongUi>
    ) {
        controller.setupRecentPlayedSongWhenPlayerRunning(songs, viewModel)
    }

    private fun isServiceInactiveWithSavedPath(path: String): Boolean {
        return !PlayerSessionService.isAlive && path.isNotBlank()
    }

    private suspend fun handleInactiveServiceSetup(
        controller: MediaController,
        songs: List<SongUi>,
        path: String
    ) {
        controller.setupRecentPlayedSongWhenServiceNotActive(
            sharedPreferences, songs, viewModel, path, context
        )
    }
    /**`such a Scenario:` if the player is paused but the service is still active <<(e.g
    when remove the app from recent task while player is playing
    and this will destroy the activity by calling onDestroy and save
    index and progress in preferences but if we change the progress using notification
    slider and then pause player from notification then click notification the service is still active
    cuz when I removed the app from recent task the player was playing not paused.)>>
    then there's no need to get saved value from preferences as I did in the previous if, instead
    I should get values from controller that connect to mediaSessionService**/
    private suspend fun handleActiveServicePausedSetup(
        controller: MediaController,
        songs: List<SongUi>,
    ) {

        val currentPath = controller.currentMediaItem?.mediaId ?: return
        val index = songs.indexOfFirst { it.path == currentPath}.takeIf { it != -1 }
            ?: controller.currentMediaItemIndex

        viewModel.updatePlayedSong(index)

        // Handle case where no media items are loaded (e.g., permission screen delay)
        /** this condition is important for one case which is the first time u open
        app and the permission screen appear , so imagine the user stay in permission
        screen more than or equal to 2 seconds in this case the PlayerSessionService.isAlive
        would be true and the controller is already not playing cuz it's first time
        and no mediaItems added to controller so this condition to check this case
        and then added media Items.
        but in case the permissions are already granted no permission screen to appear
        so the app is opened for first time and no service active(isActive flag is false  until passing 2 seconds we set it true) so the
        handleFirstTimeAppLaunch fun would be executed **/
        if (controller.mediaItemCount == 0) {
            controller.setMediaItemsList(songs, context)
        }else if (songs.size != controller.mediaItemCount){
            /** but this else block will execute every time the parent condition is true
             * to synchronize the controller mediaItems with songs cuz may new songs come from downloading and so on=*/
            controller.setMediaItemsList(
                songs = songs,
                startIndex = index,
                startProgress = controller.currentPosition,
                context = context
            )
        }

        viewModel.calculateProgressValue(controller.currentPosition)
    }

    private suspend fun handleFirstTimeAppLaunch(
        controller: MediaController,
        songs: List<SongUi>
    ) {
        viewModel.updatePlayedSong(0)
        controller.setMediaItemsList(songs, context)
    }

    private fun shouldUpdateMediaItemsForNewSongs(
        controller: MediaController,
        songs: List<SongUi>
    ): Boolean {
        return !isNewCreation &&
                songs.isNotEmpty() &&
                controller.mediaItemCount != songs.size
    }

    private suspend fun handleNewSongsAdded(controller: MediaController, songs: List<SongUi>) {
        val currentSong = viewModel.getPlayedSong()
        val index = songs.indexOf(currentSong)

        controller.setMediaItemsList(
            songs = songs,
            startIndex = index,
            startProgress = controller.currentPosition,
            context
        )
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY -> {
                controller?.let { controller ->
                    val currentSong = controller.currentMediaItemIndex
                    val currentProgress = controller.currentPosition

                    Log.e("ks", "currentSong >>>>>>>> $currentSong")
                    Log.e("ks", "currentProgress >>>>>>>> $currentProgress")
                    Log.e("ks", "onActivity Destroy")

                    sharedPreferences.edit().apply {
                        putInt("index", currentSong)
                        putLong("progress", currentProgress)
                        controller.currentMediaItem?.mediaId?.let { mediaId ->
                            putString("path", mediaId)
                            Log.e("ks", "mediaId ; $mediaId")
                        }
                        apply()
                    }

                    MediaController.releaseFuture(controllerFuture)
                    controller.removeListener(listener)
                    controller.release()
                }
            }
            else -> { /* Handle other lifecycle events if needed */
            }
        }
    }


    fun getIsNewCreation(): Boolean = isNewCreation
    fun setIsNewCreation(value: Boolean) {
        isNewCreation = value
    }

    fun getController(): MediaController? = controller

}