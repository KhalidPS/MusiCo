package com.k.sekiro.musico

import android.content.ComponentName
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.collection.LruCache
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.palette.graphics.Palette
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.player.onChangPlayType
import com.k.sekiro.musico.playmusic.player.playOrPause
import com.k.sekiro.musico.playmusic.player.service.PlayerSessionService
import com.k.sekiro.musico.playmusic.player.setMediaItemsList
import com.k.sekiro.musico.playmusic.player.setupRecentPlayedSongWhenPlayerRunning
import com.k.sekiro.musico.playmusic.player.setupRecentPlayedSongWhenServiceNotActive
import com.k.sekiro.musico.playmusic.player.startProgressUpdate
import com.k.sekiro.musico.playmusic.player.stopProgressUpdate
import com.k.sekiro.musico.playmusic.presenation.PlayType
import com.k.sekiro.musico.playmusic.presenation.UiAction
import com.k.sekiro.musico.playmusic.presenation.UiEvents
import com.k.sekiro.musico.playmusic.presenation.ViewModel
import com.k.sekiro.musico.playmusic.presenation.loading_screen.LoadingScreen
import com.k.sekiro.musico.playmusic.presenation.model.Home
import com.k.sekiro.musico.playmusic.presenation.model.PlayedSong
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistScreen
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistShowcase
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistWithSongsUi
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongScreen
import com.k.sekiro.musico.playmusic.presenation.playlist.PlaylistCollapsingScreen
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.PermissionGate
import com.k.sekiro.musico.playmusic.presenation.showcase_playlists.ShowcasePlaylists
import com.k.sekiro.musico.playmusic.presenation.showcase_playlists.mockPlaylists
import com.k.sekiro.musico.playmusic.presenation.songs_list.SongsList
import com.k.sekiro.musico.playmusic.presenation.util.ObserveAsEvent
import com.k.sekiro.musico.ui.theme.MusiCoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    var controller: MediaController? = null
    lateinit var controllerFuture: ListenableFuture<MediaController>
    val lruCache: LruCache<String, Palette> by inject()
    private val viewModel: ViewModel by viewModel()
    val sharedPref: SharedPreferences by inject()
    var isNewCreation = false


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
                lifecycleScope.launch {
                    controller!!.startProgressUpdate(viewModel::calculateProgressValue)
                }
            } else {
                stopProgressUpdate(viewModel::updateIsPlaying)
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)

            Log.e("ks", "indx onMediaMetadataChanged :${controller!!.currentMediaItemIndex}")
            viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
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


    @ExperimentalSharedTransitionApi
    @OptIn(ExperimentalSharedTransitionApi::class, UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        Log.e("ks", "onActivity create")

        super.onCreate(savedInstanceState)

        isNewCreation = true


        val sessionToken = SessionToken(
            this, ComponentName(
                this,
                PlayerSessionService::class.java
            )
        )
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
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



        enableEdgeToEdge()
        setContent {
            MusiCoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->


                    ObserveAsEvent(viewModel.events) { event ->
                        when (event) {
                            is UiEvents.Message -> {
                                Toast.makeText(
                                    this, event.msg, Toast.LENGTH_SHORT
                                ).show()
                            }


                            is UiEvents.Error -> {
                                Toast.makeText(
                                    this, event.error.message, Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }


                    val navController = rememberNavController()

                    PermissionGate {
                        SharedTransitionLayout {

                            val state = viewModel.state.collectAsStateWithLifecycle()


                            LaunchedEffect(Unit) {
                                viewModel.state
                                    .map { it.songs }
                                    .distinctUntilChanged { old, new -> old == new } // to ensure that the content of
                                    // new collecting songs list is different from previous collecting songs list if not then
                                    //eliminates the new songs list and as a result the collect body won't execute
                                    .collect {
                                        controllerAndLastPlayedSongSetup(it)
                                    }
                            }


                            NavHost(
                                navController = navController,
                                startDestination = Home::class,
                                modifier = Modifier.padding(innerPadding)
                            ) {


                                composable<Home> {

                                    val progress by rememberUpdatedState(state.value.sliderProgress)
                                    val currentPosition by rememberUpdatedState(state.value.currentPosition)
                                    val songs = state.value.songs
                                    val isPlaying = state.value.isPlaying
                                    val playedSong = state.value.playedSong
                                    val selectModeEnabled = state.value.selectModeEnabled
                                    val selectedSongs = state.value.selectedSongs
                                    val playlists = state.value.playlists
                                    val playlistWithSongs = state.value.playlistsWithSongs
                                    val recentPlaylistSongs = state.value.recentPlaylistSongs
                                    if (!songs.isEmpty()) {
                                        SongsList(
                                            songs = songs,
                                            onSongClicked = { song, index ->
                                                if (controller!!.currentMediaItemIndex != index && isNewCreation) {
                                                    /** if the creation for activity is new and for first time then
                                                    set new mediaItems then reset the isNewCreation to false cuz if the
                                                    user click the item again there is no need to set mediaItems again since we did that before
                                                    and the activity is already created but if we remove the checking for new creation, then
                                                    every time the user click the item the playing for item will start again from scratch
                                                    instead of continue playing due to setMediaItems every click**/

                                                    lifecycleScope.launch {
                                                        controller!!.setMediaItemsList(
                                                            songs,
                                                            this@MainActivity
                                                        )
                                                    }
                                                    isNewCreation = false
                                                    viewModel.addToRecent(song.id)
                                                }

                                                if (!viewModel.isSelectedSongFromPlaylist() && controller!!.currentMediaItemIndex != index) {
                                                    viewModel.updateIsSelectedSongFromPlaylist(value = false, songs = songs)
                                                    viewModel.updatePlayedSong(index)
                                                    viewModel.addToRecent(song.id)
                                                } else if (viewModel.isSelectedSongFromPlaylist()) {
                                                    lifecycleScope.launch {
                                                        controller!!.setMediaItemsList(
                                                            songs,
                                                            startIndex = index,
                                                            startProgress = 0L,
                                                            this@MainActivity
                                                        )
                                                    }
                                                    viewModel.updateIsSelectedSongFromPlaylist(value = false, songs = songs)
                                                    viewModel.updatePlayedSong(index)
                                                    viewModel.addToRecent(song.id)
                                                }
                                                navController.navigate(PlayedSong(index))
                                            },
                                            progress = { progress },
                                            isPlaying = isPlaying,
                                            currentPosition = { currentPosition },
                                            song = playedSong ?: songs[0],
                                            selectModeEnabled = selectModeEnabled,
                                            onPlayClicked = {
                                                onAction(UiAction.PlayPause)
                                            },
                                            onSelectSong = viewModel::onSelectSong,
                                            selectedSongs = selectedSongs,
                                            onCancelSelectedSongs = viewModel::onCancelAllSelectedSongs,
                                            onBottomBarClicked = {
                                                val index =
                                                    if (playedSong != null && !viewModel.isSelectedSongFromPlaylist()) {
                                                        songs.indexOf(playedSong)
                                                    } else if (playedSong != null && viewModel.isSelectedSongFromPlaylist()) {
                                                        viewModel.currentPlaylistSongs().indexOf(playedSong)
                                                    } else return@SongsList
                                                navController.navigate(
                                                    PlayedSong(
                                                        index = index,
                                                        isFromPlaylist = viewModel.isSelectedSongFromPlaylist(),
                                                        playlistId = viewModel.currentPlaylistId()
                                                    )
                                                )


                                                Log.e(
                                                    "ks",
                                                    "index : $index, isFromPlaylist: ${viewModel.isSelectedSongFromPlaylist()}, playlistId: ${viewModel.currentPlaylistId()}"
                                                )
                                            },
                                            animatedVisibilityScope = this,
                                            onAction = ::onAction,
                                            playlists = playlists,
                                            onAddToNewPlaylist = viewModel::onAddToNewPlaylist,
                                            onAddToExistPlaylist = viewModel::onAddToExistPlaylist,
                                            playlistWithSongs = playlistWithSongs,
                                            onShowcasePlaylists = {
                                                navController.navigate(PlaylistShowcase)
                                            },
                                            onClickFavOrRecent = {
                                                navController.navigate(
                                                    PlaylistScreen(it)
                                                )
                                            },
                                            recentPlaylistSongs = recentPlaylistSongs
                                        )
                                    } else {
                                        LoadingScreen()
                                    }
                                }


                                composable<PlayedSong>(
                                    popEnterTransition = {
                                        fadeIn(tween(1000, easing = LinearEasing))
                                    },
                                    enterTransition = {
                                        fadeIn(tween(1000, easing = LinearEasing))
                                    },
                                    exitTransition = {
                                        fadeOut(tween(1000, easing = LinearEasing))
                                    },
                                    popExitTransition = {
                                        fadeOut(tween(1000, easing = LinearEasing))
                                    }
                                    /*  typeMap = mapOf(
                                          typeOf<DisplayableDuration>() to CustomNavType.DisplayableDurationType
                                      )*/
                                ) {

                                    /*  val song = it.toRoute<SongUi>()
                                      val index = rememberedState.songs.indexOf(song)*/

                                    val index = it.toRoute<PlayedSong>().index
                                    val isFromPlaylist = it.toRoute<PlayedSong>().isFromPlaylist
                                    val playlistId = it.toRoute<PlayedSong>().playlistId

                                    val progress by rememberUpdatedState(state.value.sliderProgress)
                                    val passedTime by rememberUpdatedState(state.value.passedTimeDuration)
                                    val songs = remember(isFromPlaylist) {
                                        if (isFromPlaylist) {
                                            /*if (playlistId == 2L) state.value.recentPlaylistSongs
                                            else state.value.playlistsWithSongs.find { it.playlist.id == playlistId }!!.songs*/
                                            viewModel.currentPlaylistSongs()
                                        } else {
                                            state.value.songs
                                        }
                                    }


                                    val isPlaying = state.value.isPlaying
                                    val playedSong = state.value.playedSong

                                    PlayedSongScreen(
                                        lurCache = lruCache,
                                        sliderProgress = { progress },
                                        isPlaying = isPlaying,
                                        playType = state.value.playType,
                                        passedTimeDuration = { passedTime },
                                        playedSong = playedSong,
                                        songs = songs,
                                        onAction = ::onAction,
                                        index = index,
                                        animatedVisibilityScope = this,
                                        onDownArrowClicked = { navController.popBackStack() },
                                        onSettledPageChanged = viewModel::addToRecent
                                    )

                                }

                                composable<PlaylistShowcase> {
                                    val playlistsWithSongs = state.value.playlistsWithSongs
                                    ShowcasePlaylists(
                                        playlists = playlistsWithSongs,
                                        onBackButtonClicked = { navController.popBackStack() },
                                        onAddPlaylistClicked = viewModel::addNewPlaylist,
                                        onPlaylistItemClicked = {
                                            navController.navigate(PlaylistScreen(it))
                                        }
                                    )
                                }


                                composable<PlaylistScreen> {
                                    val playlistId = it.toRoute<PlaylistScreen>().id
                                    val currentPlaylistId = viewModel.currentPlaylistId()
                                    val playlist = remember(
                                        state.value.recentPlaylistSongs,
                                        state.value.playlistsWithSongs
                                    ) {
                                        if (playlistId != 2L) {
                                            state.value.playlistsWithSongs.find { it.playlist.id == playlistId }
                                        } else {
                                            PlaylistWithSongsUi(
                                                playlist = Playlist("Recent", 2L),
                                                songs = state.value.recentPlaylistSongs
                                            )
                                        }
                                    }

                                    PlaylistCollapsingScreen(
                                        playlistWithSongsUi = playlist ?: return@composable,
                                        onBackButtonClicked = { navController.popBackStack() },
                                        onSongClicked = { index, song ->
                                            //val currentPlayedIndex = playlist.songs.indexOf(state.value.playedSong)
                                            //if (song.path != controller?.currentMediaItem?.mediaId || index != controller!!.currentMediaItemIndex) {

                                            if (playlistId != currentPlaylistId){
                                                viewModel.updateIsSelectedSongFromPlaylist(
                                                    true,
                                                    playlist.songs,
                                                    playlistId
                                                )

                                                lifecycleScope.launch {
                                                    controller?.setMediaItemsList(
                                                        startIndex = index,
                                                        songs = playlist.songs,
                                                        context = this@MainActivity,
                                                        startProgress = 0L
                                                    )
                                                }
                                            }else if (song.path != controller?.currentMediaItem?.mediaId) {
                                                viewModel.updateIsSelectedSongFromPlaylist(
                                                    true,
                                                    playlist.songs,
                                                    playlistId
                                                )

                                                Log.e("ks","the index : $index , playlistId: $playlistId")
                                                lifecycleScope.launch {
                                                    controller?.setMediaItemsList(
                                                        startIndex = index,
                                                        songs = playlist.songs,
                                                        context = this@MainActivity,
                                                        startProgress = 0L
                                                    )
                                                }
                                            }
                                            navController.navigate(
                                                PlayedSong(
                                                    index,
                                                    isFromPlaylist = true,
                                                    playlistId = playlist.playlist.id
                                                )
                                            )
                                        }
                                    )

                                }

                            }
                        }

                    }


                }


                //}


            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.e("ks", "onActivity start")

    }

    override fun onDestroy() {
        super.onDestroy()

        if (controller != null) {
            val currentSong = controller!!.currentMediaItemIndex
            val currentProgress = controller!!.currentPosition

            Log.e("ks", "currentSong >>>>>>>> $currentSong")
            Log.e("ks", "currentProgress >>>>>>>> $currentProgress")
            Log.e("ks","onActivity Destroy")
            sharedPref.edit().apply {
                putInt("index", currentSong)
                putLong("progress", currentProgress)
                putString("path", controller!!.currentMediaItem!!.mediaId)
                Log.e("ks", "mediaId ; ${controller!!.currentMediaItem!!.mediaId}")
                apply()
            }

            MediaController.releaseFuture(controllerFuture)
            controller?.removeListener(listener)
            controller?.release()
        }

        viewModel.onCleared()

    }


    @OptIn(UnstableApi::class)
    fun onAction(action: UiAction) {
        lifecycleScope.launch {
            when (action) {
                is UiAction.ChangePlayType -> {

                    controller!!.onChangPlayType(action.playType, viewModel::updatePlayType)

                }

                is UiAction.ChangeToOtherSong -> {

                    when (action.index) {
                        controller!!.currentMediaItemIndex -> {
                            controller!!.playOrPause(
                                viewModel::calculateProgressValue,
                                viewModel::updateIsPlaying
                            )
                        }

                        else -> {
                            controller!!.seekToDefaultPosition(action.index)
                            viewModel.updateIsPlaying(true)
                            controller!!.playWhenReady = true
                            controller!!.startProgressUpdate(viewModel::calculateProgressValue)
                        }
                    }


                }

                UiAction.OnDownArrowClicked -> TODO()
                UiAction.OnMoreActionClicked -> TODO()
                UiAction.PlayPause -> controller!!.playOrPause(
                    viewModel::calculateProgressValue,
                    viewModel::updateIsPlaying
                )

                UiAction.SeekBackward -> controller!!.seekBack()
                UiAction.SeekForward -> controller!!.seekForward()
                is UiAction.SeekTo -> {

                    val seekPosition =
                        ((viewModel.getPlayedSong()!!.displayableDuration.durationMillis * action.position / 100f)).toLong()

                    controller!!.seekTo(seekPosition)
                }

                UiAction.SeekToNext -> {
                    if (controller!!.repeatMode == Player.REPEAT_MODE_ONE && controller!!.currentMediaItemIndex == controller!!.mediaItemCount - 1) {
                        controller!!.seekTo(0, 0L)
                    } else {
                        controller!!.seekToNextMediaItem()

                    }
                }

                UiAction.SeekToPrevious -> {
                    if (controller!!.repeatMode == Player.REPEAT_MODE_ONE && controller!!.currentMediaItemIndex == 0) {
                        controller!!.seekTo(controller!!.mediaItemCount - 1, 0L)
                    } else {
                        controller!!.seekToPreviousMediaItem()

                    }
                }

                is UiAction.UpdateProgress -> {
                    viewModel.updateProgress(action.newProgress)
                }

            }
        }

    }


    suspend fun controllerAndLastPlayedSongSetup(songs1: List<SongUi>) {
        val isFromPlaylist = viewModel.isSelectedSongFromPlaylist()
        val songs = if (isFromPlaylist) viewModel.currentPlaylistSongs() else songs1
        if (songs.isNotEmpty() && controller != null && isNewCreation) {

            val path = sharedPref.getString("path", "")

            if (controller!!.isPlaying) {
                controller!!.setupRecentPlayedSongWhenPlayerRunning(songs, viewModel)

            } else if (!PlayerSessionService.isAlive && path != null && path.isNotBlank() && path.isNotEmpty()) {
                /** if the player is paused and the service is not active (new creation for service)
                then get the saved value from preferences then update the state and setMediaItems **/

                controller!!.setupRecentPlayedSongWhenServiceNotActive(sharedPref,songs,viewModel,path,this)


            } else if (PlayerSessionService.isAlive && !controller!!.isPlaying) {
                /** if the player is paused but the service is still active <<(e.g
                when remove the app from recent task while player is playing
                and this will destroy the activity by calling onDestroy and save
                index and progress in preferences but if we change the progress using notification
                slider and then pause player from notification then click notification the service is still active
                cuz when I removed the app from recent task the player was playing not paused.)>>
                then there's no need to get saved value from preferences as I did in the previous if, instead
                I should get values from controller that connect to mediaSessionService**/

                val currentPath = controller!!.currentMediaItem!!.mediaId
                val index = songs.indexOfFirst { it.path == currentPath}.takeIf { it != -1 }
                    ?: controller!!.currentMediaItemIndex

                viewModel.updatePlayedSong(index)


                /*if (songs[controller!!.currentMediaItemIndex].path == path) {
                    viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
                } else {
                    viewModel.updatePlayedSong(songs.indexOfFirst { it.path == path })*/

                    /** this condition is important for one case which is the first time u open
                    app and the permission screen appear , so imagine the user stay in permission
                    screen more than or equal to 2 seconds in this case the PlayerSessionService.isAlive
                    would be true and the controller is already not playing cuz it's first time
                    and no mediaItems added to controller so this condition to check this case
                    and then added media Items.
                    but in case the permissions are already granted no permission screen to appear
                    so the app is opened for first time and no service active(isActive flag is false  until passing 2 seconds we set it true) so the
                    last else block would be executed **/
                    if (controller!!.mediaItemCount == 0) {
                        controller!!.setMediaItemsList(songs, this@MainActivity)
                    }else if (songs.size != controller!!.mediaItemCount){
                        /** but this else block will execute every time the parent condition is true
                         * to synchronize the controller mediaItems with songs cuz may new songs come from downloading and so on=*/
                        controller!!.setMediaItemsList(
                            songs = songs,
                            startIndex = index,
                            startProgress = controller!!.currentPosition,
                            context = this
                        )
                    }
               // }



                Log.e(
                    "ks",
                    "Played song not null: ${viewModel.getPlayedSong()}"
                )
                //controller!!.startProgressUpdate(viewModel)
                viewModel.calculateProgressValue(controller!!.currentPosition)

            } /*else if (PlayerSessionService.isAlive && controller!!.isPlaying){
                Log.e("ks","here!!!!!!!!!!!1")

                if (viewModel.isSelectedSongFromPlaylist()){
                    viewModel.updateCurrentPlaylist(PlayerSessionService.getCurrentPlaylistSongs()!!)
                    viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
                }
            }*/else {
                /** Here the app is opened for first time**/
                viewModel.updatePlayedSong(0)
                controller.setMediaItemsList(songs, this@MainActivity)

            }

            isNewCreation = false

        } else if (!isNewCreation && songs.isNotEmpty() && controller!!.mediaItemCount != songs.size) {
            /** This in case the player is playing and the user new songs are coming either by downloading them
            or by received them from other device using share apps **/
            val index = songs.indexOf(viewModel.getPlayedSong())
            Log.e("ks", "index for after new added songs : $index")
            controller.setMediaItemsList(
                songs,
                startIndex = index,
                startProgress = controller!!.currentPosition
            )
        }
    }


}










