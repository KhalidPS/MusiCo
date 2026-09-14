package com.k.sekiro.musico.playmusic.presenation.player

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver
import com.k.sekiro.musico.playmusic.domain.model.PATH_KEY
import com.k.sekiro.musico.playmusic.domain.model.PlayMode_KEY
import com.k.sekiro.musico.playmusic.presenation.PlayType
import com.k.sekiro.musico.playmusic.presenation.ViewModel
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.player.service.PlayerSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class MediaControllerManager(
    private val context: Context,
    private val dataSaver: SimpleDataSaver,
) : LifecycleEventObserver {

    private lateinit var viewModel: ViewModel

    private lateinit var coroutineScope: CoroutineScope
    private var controller: MediaController? = null
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var isNewCreation = true



    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> viewModel.calculateProgressValue(controller!!.currentPosition)
            }
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

    fun initialize() {
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


    /** Suspends until [controller] is connected instead of relying on whatever [initialize]'s
     * own listener has set by the time this is called. [controllerFuture] resolves once, on a
     * cold app start binding to [PlayerSessionService] for the first time - so a caller that
     * checked the bare field instead of awaiting this could easily run before that completed and
     * silently skip setup (see [controllerAndLastPlayedSongSetup]'s former bug). */
    private suspend fun awaitController(): MediaController? {
        controller?.let { return it }
        return suspendCancellableCoroutine { cont ->
            controllerFuture.addListener(
                {
                    val result = runCatching { controllerFuture.get() }.getOrNull()
                    if (result != null) controller = result
                    if (cont.isActive) cont.resume(result, onCancellation = null)
                },
                MoreExecutors.directExecutor()
            )
        }
    }

    suspend fun controllerAndLastPlayedSongSetup(allSongs: List<SongUi>) {
        Log.e("ks","enter controllerManager controllerAndLastPlayedSongSetup block")
        // Was `this.controller ?: return`: on a fresh install PlayerSessionService starts cold,
        // so MediaController.Builder(...).buildAsync() can still be pending by the time the first
        // non-empty song list arrives. That silently skipped the whole setup - no media items on
        // the controller, playedSong left null forever (this call site's caller never re-fires
        // for the same song-list content) - which is what let UiAction.SeekTo's
        // getPlayedSong()!! crash on first launch. Now waits for the connection instead of
        // giving up on it.
        val controller = awaitController() ?: return
        Log.e("ks","after controller check")

        val songs = getRelevantSongsList(allSongs)

        if (songs.isEmpty()) return
        Log.e("ks","after songs check")


        coroutineScope {

            when {
                isNewCreation -> handleNewCreationSetup(controller, songs)
                shouldUpdateMediaItemsForNewSongs(controller, songs) -> handleNewSongsAdded(
                    controller,
                    songs
                )
            }
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
        val savedPath = dataSaver.suspendGet(PATH_KEY, "")
        Log.e("ks", "path in handleNewCreationSetup : $savedPath")

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

       // isNewCreation = false
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
        coroutineScope {
            val playMode = async { dataSaver.suspendGet(PlayMode_KEY, PlayType.RepeatAll.name) }
            controller.setupRecentPlayedSongWhenServiceNotActive(
                dataSaver, songs, viewModel, path
            )


            val playType = PlayType.valueOf(playMode.await())
            Log.e("ks","playType : $playType")
            controller.onChangPlayType(playType,viewModel::updatePlayType)
        }

    }

    /**`such a Scenario:` if the player is paused but the service is still active <<(e.g
    when remove the app from recent task while player is playing
    and this will destroy the activity by calling onDestroy and save
    index and progress in preferences but if we change the progress using notification
    slider and then pause player from notification then click notification the service is still active
    cuz when I removed the app from recent task the player was playing not paused.)>>
    then there's no need to get saved value from preferences as I did in the previous if, instead
    I should get values from controller that connect to mediaSessionService**/
    private fun handleActiveServicePausedSetup(
        controller: MediaController,
        songs: List<SongUi>,
    ) {

        val currentPath = controller.currentMediaItem?.mediaId ?: return
        // coerced: the fallback is an index into the *controller's* list, which can be longer than
        // the songs list the library just shrank to.
        val index = songs.indexOfFirst { it.path == currentPath }.takeIf { it != -1 }
            ?: controller.currentMediaItemIndex.coerceIn(0, songs.lastIndex)

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
            controller.setMediaItemsList(songs)
        } else if (songs.size != controller.mediaItemCount) {
            /** but this else block will execute every time the parent condition is true
             * to synchronize the controller mediaItems with songs cuz may new songs come from downloading and so on=*/
            controller.setMediaItemsList(
                songs = songs,
                startIndex = index,
                startProgress = controller.currentPosition,
            )
        }
        controller.syncUiPlayModeWithController(viewModel)
        viewModel.calculateProgressValue(controller.currentPosition)
    }

    private fun handleFirstTimeAppLaunch(
        controller: MediaController,
        songs: List<SongUi>
    ) {
        viewModel.updatePlayedSong(0)
        controller.setMediaItemsList(songs)
    }

    private fun shouldUpdateMediaItemsForNewSongs(
        controller: MediaController,
        songs: List<SongUi>
    ): Boolean {
        return !isNewCreation &&
                songs.isNotEmpty() &&
                controller.mediaItemCount != songs.size
    }

    /** Runs whenever the library's song count changed under a live player - songs downloaded or
     * transferred in, and equally songs the user just deleted, possibly the one that is playing.
     * See [resolveQueueAnchor] for why `songs.indexOf(playedSong)` can't be used directly. */
    private fun handleNewSongsAdded(controller: MediaController, songs: List<SongUi>) {
        val anchor = resolveQueueAnchor(
            paths = songs.map { it.path },
            playingPath = controller.currentMediaItem?.mediaId,
            uiPath = viewModel.getPlayedSong()?.path,
            previousIndex = controller.currentMediaItemIndex,
        ) ?: return

        controller.setMediaItemsList(
            songs = songs,
            startIndex = anchor.index,
            // The song that was playing is gone - the one that took its place starts from the
            // top, rather than resuming at the dead song's position part-way through.
            startProgress = if (anchor.keepPosition) controller.currentPosition else 0L,
        )
        if (!anchor.keepPosition) viewModel.updatePlayedSong(anchor.index)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY -> {
                controller?.let { controller ->
                    val currentSong = controller.currentMediaItemIndex
                    val currentProgress = controller.currentPosition

                    val manufacturer = Build.MANUFACTURER
                    val model = Build.MODEL

                    Log.e("ks", "manufacturer >>>>>>>> $manufacturer")
                    Log.e("ks", "model >>>>>>>> $model")

                    Log.e("ks", "Yoooooooo the activity truly destroyed")

                    Log.e("ks", "currentSong >>>>>>>> $currentSong")
                    Log.e("ks", "currentProgress >>>>>>>> $currentProgress")
                    Log.e("ks", "onActivity Destroy")

                    MediaController.releaseFuture(controllerFuture)
                    controller.removeListener(listener)
                    controller.release()

                    Log.e("ks", "Yooo the cleaning for res done")
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

    fun setViewModel(viewModel: ViewModel) {
        this.viewModel = viewModel
    }

    fun setCoroutineScope(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }



}