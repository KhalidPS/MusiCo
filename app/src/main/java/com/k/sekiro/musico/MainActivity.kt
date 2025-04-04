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
    val dataStore: DataStore<Preferences> by inject()
    val sharedPref: SharedPreferences by inject()
    var progressJob: Job? = null
    var isNewCreation = false



    private val listener = object : Player.Listener{
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
            viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
            if (isPlaying) {
                lifecycleScope.launch {
                    controller!!.startProgressUpdate(viewModel)
                }
            } else {
                stopProgressUpdate(viewModel)
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)

            viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
        }



    }



    @ExperimentalSharedTransitionApi
    @OptIn(ExperimentalSharedTransitionApi::class, UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 0)
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(arrayOf(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK), 1)

        }

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
                    Log.e("ks","MediaController : $controller")
                    Log.e("ks", "MediaController : $controller")

                    if (controller!!.isPlaying) {
                        viewModel.updateIsPlaying(true)
                        viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)

                        lifecycleScope.launch(Dispatchers.IO) {
                            while (viewModel.getPlayedSong() == null) {
                                delay(500)
                                withContext(Dispatchers.Main) {
                                    viewModel.updatePlayedSong(controller!!.currentMediaItemIndex)
                                }
                            }
                            Log.e("ks", "Played song not null: ${viewModel.getPlayedSong()}")

                            withContext(Dispatchers.Main){
                                controller!!.startProgressUpdate(viewModel)
                            }


                        }


                    } else {

                        val index = sharedPref.getInt("index",0)
                        val progress = sharedPref.getLong("progress",0)

                        lifecycleScope.launch(Dispatchers.IO) {
                            while (viewModel.getPlayedSong() == null) {
                                delay(500)
                                withContext(Dispatchers.Main) {
                                    viewModel.updatePlayedSong(index)
                                }
                            }
                            Log.e(
                                "ks",
                                "Played song not null: ${viewModel.getPlayedSong()}"
                            )
                            withContext(Dispatchers.Main) {
                                controller!!.setMediaItemsList(viewModel.getSongs())
                                controller!!.seekTo(index,progress)
                                //controller!!.startProgressUpdate(viewModel)
                                viewModel.calculateProgressValue(progress)
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


                    val state = viewModel.state.collectAsState(PlayedSongState()).value
                    val songs = state.songs
                    var isBottomBarClicked by remember { mutableStateOf(false) }



                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = Home::class,
                            modifier = Modifier.padding(innerPadding)
                        ) {


                            composable<Home> {
                                if (!songs.isEmpty()) {
                                    SongsList(
                                        songs = songs,
                                        onSongClicked = { song ,index ->
                                            isBottomBarClicked = false
                                            if (controller!!.currentMediaItemIndex != index && isNewCreation){
                                                controller!!.setMediaItemsList(songs)
                                                isNewCreation = false
                                            }
                                            navController.navigate(PlayedSong(index))
                                        },
                                        state = state,
                                        onPlayClicked = { onAction(PlayedSongAction.PlayPause) },
                                        onBottomBarClicked = {
                                            isBottomBarClicked = true
                                            val index = if (state.playedSong != null) songs.indexOf(state.playedSong) else return@SongsList
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
                                enterTransition = if (isBottomBarClicked){
                                    {
                                        slideInVertically(
                                            animationSpec = tween(
                                                durationMillis = 1000,
                                                easing = FastOutSlowInEasing
                                            ),
                                            initialOffsetY = {it}
                                        )
                                    }
                                } else null,
                                exitTransition = if (isBottomBarClicked){
                                    {
                                        slideOutVertically(
                                            animationSpec = tween(
                                                durationMillis = 1000,
                                                easing = FastOutSlowInEasing
                                            ),
                                            targetOffsetY = { it }
                                        )
                                    }
                                } else null
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
                                    isBottomBarClicked = isBottomBarClicked
                                )

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

        lifecycleScope.launch {
            Log.e("ks","currentSong >>>>>>>> $currentSong")
            Log.e("ks","currentProgress >>>>>>>> $currentProgress")

            sharedPref.edit().apply{
                putInt("index",currentSong)
                putLong("progress",currentProgress)
                apply()
            }
        }

        MediaController.releaseFuture(controllerFuture)
        controller?.removeListener(listener)
        controller?.release()

    }




    suspend fun onPlayerEvents(
        playerEvent: PlayerEvent,
        selectedAudioIndex: Int = -1,
        seekPosition: Long = 0,
    ) {
        when (playerEvent) {
            PlayerEvent.Backward -> controller!!.seekBack()
            PlayerEvent.Forward -> controller!!.seekForward()
            PlayerEvent.SeekToNext -> controller!!.seekToNext()
            PlayerEvent.SeekToPrevious -> controller!!.seekToPrevious()
            PlayerEvent.PlayPause -> controller!!.playOrPause(viewModel)
            PlayerEvent.SeekTo -> controller!!.seekTo(seekPosition)
            PlayerEvent.SelectedAudioChange -> {
                when (selectedAudioIndex) {
                    controller!!.currentMediaItemIndex -> {
                        controller!!.playOrPause(viewModel)
                    }

                    else -> {
                        controller!!.seekToDefaultPosition(selectedAudioIndex)
                        viewModel.updateIsPlaying(true)
                        controller!!.playWhenReady = true
                        controller!!.startProgressUpdate(viewModel)
                    }
                }
            }

            PlayerEvent.Stop -> stopProgressUpdate(viewModel)
            is PlayerEvent.UpdateProgress -> {
                controller!!.seekTo(
                    (controller!!.duration * playerEvent.newProgress).toLong()
                )
            }

            is PlayerEvent.ChangePlayType -> {
                controller!!.onChangPlayType(playerEvent.type,viewModel)
            }

            is PlayerEvent.ClickNotification -> {
                val song = controller!!.getSongPlayedInBackground()

                if (song != null){
                    viewModel.updatePlayedSong(song.first)


                    controller!!.seekTo(song.second)

                }

            }
        }
    }














    @OptIn(UnstableApi::class)
    fun onAction(action: PlayedSongAction) {
        lifecycleScope.launch{
            when (action) {
                is PlayedSongAction.ChangePlayType -> {
                    onPlayerEvents(PlayerEvent.ChangePlayType(action.playType))
                }
                is PlayedSongAction.ChangeToOtherSong -> {
                    onPlayerEvents(
                        PlayerEvent.SelectedAudioChange,
                        selectedAudioIndex = action.index
                    )
                }

                PlayedSongAction.OnDownArrowClicked -> TODO()
                PlayedSongAction.OnMoreActionClicked -> TODO()
                PlayedSongAction.PlayPause -> onPlayerEvents(PlayerEvent.PlayPause)
                PlayedSongAction.SeekBackward -> TODO()
                PlayedSongAction.SeekForward -> TODO()
                is PlayedSongAction.SeekTo -> {
                    onPlayerEvents(
                        PlayerEvent.SeekTo,
                        seekPosition = ((viewModel.getPlayedSong()!!.displayableDuration.durationMillis * action.position / 100f)).toLong()
                    )
                }

                PlayedSongAction.SeekToNext -> {
                    onPlayerEvents(
                        PlayerEvent.SeekToNext
                    )
                }
                PlayedSongAction.SeekToPrevious -> {
                    onPlayerEvents(
                        PlayerEvent.SeekToPrevious
                    )
                }
                is PlayedSongAction.UpdateProgress -> {
                    onPlayerEvents(
                        PlayerEvent.UpdateProgress(action.newProgress)
                    )
                    viewModel.updateProgress(action.newProgress)
                }

                PlayedSongAction.ClickNotification -> onPlayerEvents(
                    PlayerEvent.ClickNotification
                )
            }
        }

    }

}








