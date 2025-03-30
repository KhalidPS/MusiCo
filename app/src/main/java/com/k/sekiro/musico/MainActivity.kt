package com.k.sekiro.musico

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.LruCache
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.palette.graphics.Palette
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.player.service.PlayerSessionService
import com.k.sekiro.musico.playmusic.presenation.loading_screen.LoadingScreen
import com.k.sekiro.musico.playmusic.presenation.model.CustomNavType
import com.k.sekiro.musico.playmusic.presenation.model.DisplayableDuration
import com.k.sekiro.musico.playmusic.presenation.model.Home
import com.k.sekiro.musico.playmusic.presenation.model.PlayedSong
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongAction
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongScreen
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongState
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongViewModel
import com.k.sekiro.musico.playmusic.presenation.songs_list.SongsList
import com.k.sekiro.musico.ui.theme.MusiCoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.reflect.typeOf

class MainActivity : ComponentActivity() {
    private var isServiceRunning: Boolean = false
    var controller: MediaController? = null
    lateinit var controllerFuture: ListenableFuture<MediaController>

    private val viewModel: PlayedSongViewModel by viewModel()
    private val sharedPreferences: SharedPreferences by inject()
    //private lateinit var viewModel: PlayedSongViewModel


    private fun startingService() {
        var intent = Intent(this, PlayerSessionService::class.java).apply {
        }

        if (!isServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            isServiceRunning = true
        }
    }


    override fun onStart() {
        super.onStart()

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
                    viewModel.initController(controller!!)
                    val currentSong = sharedPreferences.getInt("currentIndex",0)
                    val currentProgress = sharedPreferences.getLong("currentProgress",0)
                    val isResumeable = sharedPreferences.getBoolean("isResumeable",false)

                    Log.e("ks","""
                        activity currentIndex:$currentSong
                        activity currentProgress:$currentProgress
                        activity isResumeable:$isResumeable
                    """.trimIndent())

                    lifecycleScope.launch {
                        delay(1000)
                        withContext(Dispatchers.Main) {

                            controller!!.seekTo(currentSong,currentProgress)
                            if (isResumeable){
                                controller!!.prepare()
                                controller!!.play()
                            }
                        }
                    }

                }
            }, MoreExecutors.directExecutor()
        )
    }


    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        if (intent.action != null && intent.action == Intent.ACTION_RUN) {

            val index = intent.getIntExtra("currentPlayingIndex", 0)
            val position = intent.getLongExtra("currentPlayingPosition", 0L)
            val name = intent.getStringExtra("name")

            Log.e("ks", "index:$index, name:$name, position:$position")

            Log.e("ks", "geting the intent ")
        }

        val lruCache: LruCache<String, Palette> by inject()
        val repo: SongsRepository by inject()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 0)
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions(arrayOf(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK), 1)

        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusiCoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val navController = rememberNavController()


                    val playedSongViewModel: PlayedSongViewModel = koinViewModel()
                    //viewModel = playedSongViewModel


                    val state = playedSongViewModel.state.collectAsState(PlayedSongState()).value
                    val songs = state.songs
                    var isBottomBarClicked by remember { mutableStateOf(false) }


                    /*                    val requestPermissionScreenViewModel: RequestPermissionScreenViewModel = koinViewModel()
                                        val showDialog = requestPermissionScreenViewModel.showDialog.collectAsState(false).value
                                        val goToSettings = requestPermissionScreenViewModel.goToSettings.collectAsState(false).value

                                        RequestPermissionScreen(
                                            context = this,
                                            showDialog = showDialog,
                                            goToSettings = goToSettings,
                                            updateGoToSettings = requestPermissionScreenViewModel::updateGoToSettings,
                                            updateShowDialog = requestPermissionScreenViewModel::updateShowDialog
                                        ){*/

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
                                            navController.navigate(PlayedSong(index))
                                        },
                                        state = state,
                                        onPlayClicked = { playedSongViewModel.onAction(PlayedSongAction.PlayPause) },
                                        onBottomBarClicked = {
                                            isBottomBarClicked = true
                                            val index = if (state.playedSong != null) songs.indexOf(state.playedSong) else return@SongsList
                                            navController.navigate(PlayedSong(index))
                                        },
                                        animatedVisibilityScope = this,
                                        onAction = playedSongViewModel::onAction
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

                                // viewModel.onAction(PlayedSongAction.ChangeToOtherSong(index))

                                PlayedSongScreen(
                                    lurCache = lruCache,
                                    state = state,
                                    onAction = playedSongViewModel::onAction,
                                    index = index,
                                    animatedVisibilityScope = this,
                                    isBottomBarClicked = isBottomBarClicked
                                )

                                //viewModel.onAction(PlayedSongAction.PlayPause)
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
        MediaController.releaseFuture(controllerFuture)
        /* Intent(this, PlayerSessionService::class.java).apply {
     stopService(this)
 }*/

        //unbindService(serviceConnection)
    }
}





