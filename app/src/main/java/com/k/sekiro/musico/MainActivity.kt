package com.k.sekiro.musico

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.collection.LruCache
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.FileDataSource
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
import com.k.sekiro.musico.playmusic.player.getSongPlayedInBackground
import com.k.sekiro.musico.playmusic.player.onChangPlayType
import com.k.sekiro.musico.playmusic.player.playOrPause
import com.k.sekiro.musico.playmusic.player.service.PlayerEvent
import com.k.sekiro.musico.playmusic.player.service.PlayerSessionService
import com.k.sekiro.musico.playmusic.player.service.PlayerState
import com.k.sekiro.musico.playmusic.player.setMediaItemsList
import com.k.sekiro.musico.playmusic.player.startProgressUpdate
import com.k.sekiro.musico.playmusic.player.stopProgressUpdate
import com.k.sekiro.musico.playmusic.presenation.loading_screen.LoadingScreen
import com.k.sekiro.musico.playmusic.presenation.model.Home
import com.k.sekiro.musico.playmusic.presenation.model.PlayedSong
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toUri
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayType
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongAction
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongScreen
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongState
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongViewModel
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.PermissionGate
import com.k.sekiro.musico.playmusic.presenation.songs_list.SongsList
import com.k.sekiro.musico.ui.theme.MusiCoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.properties.ReadOnlyProperty

class MainActivity : ComponentActivity() {
    var controller: MediaController? = null
    lateinit var controllerFuture: ListenableFuture<MediaController>
    val lruCache: LruCache<String, Palette> by inject()
    private val viewModel: PlayedSongViewModel by viewModel()
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

                    if (controller!!.isPlaying) {
                        viewModel.updateIsPlaying(true)
                        val path = sharedPref.getString("path","")
                        var songs = viewModel.getSongs()

                        lifecycleScope.launch(Dispatchers.IO) {
                            /** The checking for getPlayedSong is important cuz at the first time it would be null
                             * until get songs then get played song by index and this happen when whe collect
                             * the state cuz the initial loading for data happen in onStart flow lifecycle fun
                             * we can use an alternative approach by call initial loading data in init block inside
                             * viewModel but this anti-pattern way **/
                            while (viewModel.getPlayedSong() == null) {
                                delay(500)
                                withContext(Dispatchers.Main) {
                                    songs = viewModel.getSongs()
                                    if (songs.isNotEmpty()){
                                        if (songs[controller!!.currentMediaItemIndex].path == path){
                                            viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
                                        }else{
                                            viewModel.updatePlayedSong(songs.indexOf(songs.find { it.path == path }))
                                        }

                                    }

                                }
                            }
                            Log.e("ks", "Played song not null: ${viewModel.getPlayedSong()}")

                            withContext(Dispatchers.Main) {
                                controller!!.startProgressUpdate(viewModel::calculateProgressValue)
                            }


                        }


                    } else if (!PlayerSessionService.isAlive) {
                        /** if the player is paused and the service is not active (new creation for service)
                        then get the saved value from preferences then update the state and setMediaItems **/
                        val progress = sharedPref.getLong("progress", 0)
                        val path = sharedPref.getString("path", "")
                        var index = sharedPref.getInt("index", 0)

                        /** Previously I was get the saved index from pref and then get the last played song
                        but this would not be good solution in some scenarios (e.g if pause the player
                        and close the app this will save the index then u downloaded new song then u open app
                        again the problem is that the played song would be now not the song u expected to be which is the
                        last one played before u close the app, instead it would be the previous song for the song u expected to be
                        due to adding new songs to storage)**/
                        var songs = viewModel.getSongs()


                        lifecycleScope.launch(Dispatchers.IO) {
                            while (songs.isEmpty()) {
                                songs = viewModel.getSongs()
                            }

                            Log.e("ks", "path: $path")

                            for (i in 0 until songs.size) {
                                Log.e("ks", "inside for size of List: ${songs.size}")
                                Log.e("ks", "path inside for loop :${songs[i].path}")
                                if (songs[i].path == path) {
                                    Log.e("ks", "path inside if :${songs[i].path}")
                                    index = i
                                    viewModel.updatePlayedSong(i)
                                    Log.e("ks", "catch the index :$i")
                                    Log.e("ks", "catch the index 2 :$index")
                                    break
                                }
                            }


                            while (viewModel.getPlayedSong() == null) {
                                delay(500)
                                viewModel.updatePlayedSong(index)
                            }
                            Log.e(
                                "ks",
                                "Played song not null: ${viewModel.getPlayedSong()}"
                            )
                            withContext(Dispatchers.Main) {
                                controller!!.setMediaItemsList(songs, index, progress,this@MainActivity)
                                //controller!!.startProgressUpdate(viewModel)
                                viewModel.calculateProgressValue(progress)
                            }
                        }
                    } else {
                        /** if the player is paused but the service is still active <<(e.g
                        when remove the app from recent task while player is playing
                        and this will destroy the activity by calling onDestroy and save
                        index and progress in preferences but if we change the progress using notification
                        slider and then pause player from notification then click notification the service is still active
                        cuz when I removed the app from recent task the player was playing not paused.)>>
                        then there's no need to get saved value from preferences as I did in the previous if, instead
                        I should get values from controller that connect to mediaSessionService**/

                        val path = sharedPref.getString("path","")
                        var songs = viewModel.getSongs()
                        lifecycleScope.launch(Dispatchers.IO) {
                            while (viewModel.getPlayedSong() == null) {
                                delay(500)
                                withContext(Dispatchers.Main) {
                                    songs = viewModel.getSongs()
                                    if (songs.isNotEmpty()){
                                        if (songs[controller!!.currentMediaItemIndex].path == path){
                                            viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
                                        }else{
                                            viewModel.updatePlayedSong(songs.indexOf(songs.find { it.path == path }))
                                        }

                                    }
                                }
                            }
                            Log.e(
                                "ks",
                                "Played song not null: ${viewModel.getPlayedSong()}"
                            )
                            withContext(Dispatchers.Main) {
                                //controller!!.startProgressUpdate(viewModel)
                                viewModel.calculateProgressValue(controller!!.currentPosition)
                            }
                        }
                    }
                }
            }, MoreExecutors.directExecutor()
        )



        enableEdgeToEdge()
        setContent {
            MusiCoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val navController = rememberNavController()


                    PermissionGate {
                        SharedTransitionLayout {

                            val state = viewModel.state.collectAsStateWithLifecycle().value
                            val songs = state.songs
                            NavHost(
                                navController = navController,
                                startDestination = Home::class,
                                modifier = Modifier.padding(innerPadding)
                            ) {


                                composable<Home> {
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
                                                }
                                                navController.navigate(PlayedSong(index))
                                            },
                                            state = state,
                                            onPlayClicked = {
                                                onAction(PlayedSongAction.PlayPause)
                                                Log.e("ks","PlayedSong when play clicked : ${state.playedSong}")

                                            },
                                            onBottomBarClicked = {
                                                val index =
                                                    if (state.playedSong != null) songs.indexOf(
                                                        state.playedSong
                                                    ) else return@SongsList
                                                navController.navigate(PlayedSong(index))
                                            },
                                            animatedVisibilityScope = this,
                                            onAction = ::onAction
                                        )
                                    } else {
                                        LoadingScreen()
                                    }
                                }


                                composable<PlayedSong>(
                                    /*  typeMap = mapOf(
                                          typeOf<DisplayableDuration>() to CustomNavType.DisplayableDurationType
                                      )*/
                                ) {

                                    /*  val song = it.toRoute<SongUi>()
                                      val index = state.songs.indexOf(song)*/

                                    val index = it.toRoute<PlayedSong>().index


                                    PlayedSongScreen(
                                        lurCache = lruCache,
                                        state = state,
                                        onAction = ::onAction,
                                        index = index,
                                        animatedVisibilityScope = this,
                                        onDownArrowClicked = { navController.popBackStack() }
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

    override fun onDestroy() {
        super.onDestroy()

        val currentSong = controller!!.currentMediaItemIndex
        val currentProgress = controller!!.currentPosition

        Log.e("ks", "currentSong >>>>>>>> $currentSong")
        Log.e("ks", "currentProgress >>>>>>>> $currentProgress")

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


    @OptIn(UnstableApi::class)
    fun onAction(action: PlayedSongAction) {
        lifecycleScope.launch {
            when (action) {
                is PlayedSongAction.ChangePlayType -> {

                    controller!!.onChangPlayType(action.playType, viewModel::updatePlayType)

                }

                is PlayedSongAction.ChangeToOtherSong -> {

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

                PlayedSongAction.OnDownArrowClicked -> TODO()
                PlayedSongAction.OnMoreActionClicked -> TODO()
                PlayedSongAction.PlayPause -> controller!!.playOrPause(
                    viewModel::calculateProgressValue,
                    viewModel::updateIsPlaying
                )

                PlayedSongAction.SeekBackward -> controller!!.seekBack()
                PlayedSongAction.SeekForward -> controller!!.seekForward()
                is PlayedSongAction.SeekTo -> {

                    val seekPosition =
                        ((viewModel.getPlayedSong()!!.displayableDuration.durationMillis * action.position / 100f)).toLong()

                    controller!!.seekTo(seekPosition)
                }

                PlayedSongAction.SeekToNext -> {
                    if (controller!!.repeatMode == Player.REPEAT_MODE_ONE && controller!!.currentMediaItemIndex == controller!!.mediaItemCount - 1) {
                        controller!!.seekTo(0, 0L)
                    } else {
                        controller!!.seekToNextMediaItem()

                    }
                }

                PlayedSongAction.SeekToPrevious -> {
                    if (controller!!.repeatMode == Player.REPEAT_MODE_ONE && controller!!.currentMediaItemIndex == 0) {
                        controller!!.seekTo(controller!!.mediaItemCount - 1, 0L)
                    } else {
                        controller!!.seekToPreviousMediaItem()

                    }
                }

                is PlayedSongAction.UpdateProgress -> {
                    viewModel.updateProgress(action.newProgress)
                }

            }
        }

    }

}








