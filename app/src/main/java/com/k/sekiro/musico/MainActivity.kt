package com.k.sekiro.musico

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.LruCache
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import com.google.common.util.concurrent.MoreExecutors
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.player.service.PlayerSessionService
import com.k.sekiro.musico.playmusic.presenation.loading_screen.LoadingScreen
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongScreen
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongState
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongViewModel
import com.k.sekiro.musico.ui.theme.MusiCoTheme
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private var isServiceRunning: Boolean = false
    private lateinit var controller: MediaController



    private fun startingService(){
        var intent = Intent(this, PlayerSessionService::class.java).apply {
        }

        if (!isServiceRunning){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                startForegroundService(intent)
            }else{
                startService(intent)
            }

            isServiceRunning = true
        }
    }


    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this,
            PlayerSessionService::class.java))

        val controllerFuture = MediaController.Builder(this,sessionToken).buildAsync()
        controllerFuture.addListener({
            if (controllerFuture.isDone){
                controller = controllerFuture.get()
            }
        }, MoreExecutors.directExecutor()
        )

        startingService()

    }


    override fun onCreate(savedInstanceState: Bundle?) {

        val lruCache: LruCache<String, Palette> by inject()
        val repo: SongsRepository by inject()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO),0)
        }else{
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),0)

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            requestPermissions(arrayOf(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK),1)

        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusiCoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val playedSongViewModel: PlayedSongViewModel = koinViewModel()
                    val state = playedSongViewModel.state.collectAsState(PlayedSongState()).value
                    val songs = state.songs

                    Log.e("ks","$playedSongViewModel.")

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
                        Column(modifier = Modifier.padding(innerPadding)) {
                            if (!songs.isEmpty()){
                                PlayedSongScreen(
                                    lurCache = lruCache,
                                    state = state,
                                    onAction = playedSongViewModel::onAction
                                )
                                //SongsList(repositoryImpl = repo)
                            }else{
                                LoadingScreen()
                            }

                        }


                    //}



                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
       /* Intent(this, PlayerSessionService::class.java).apply {
            stopService(this)
        }*/

        //unbindService(serviceConnection)
    }
}

