package com.k.sekiro.musico

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.LruCache
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.palette.graphics.Palette
import com.k.sekiro.musico.playmusic.data.repository.SongsRepositoryImpl
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.player.service.PlayerSessionService
import com.k.sekiro.musico.playmusic.presenation.loading_screen.LoadingScreen
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongScreen
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongState
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongViewModel
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.RequestPermissionScreenViewModel
import com.k.sekiro.musico.playmusic.presenation.songs_list.SongsList
import com.k.sekiro.musico.ui.theme.MusiCoTheme
import com.k.sekiro.taskmanagementapp.task_management_feature.presentation.request_permission_screen.RequestPermissionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private var isServiceRunning: Boolean = false


    private fun startService(){
        if (!isServiceRunning){
            var intent = Intent(this, PlayerSessionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                startForegroundService(intent)
            }else{
                startService(intent)
            }

            isServiceRunning = true
        }
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



        startService()



        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusiCoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val playedSongViewModel: PlayedSongViewModel = koinViewModel()
                    val state = playedSongViewModel.state.collectAsState(PlayedSongState())
                    val songs = state.value.songs

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
                                    songs = { songs },
                                    sliderValue = {state.value.sliderProgress},
                                    onAction = playedSongViewModel::onAction,
                                    onStart = { startService() }
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
        Intent(this, PlayerSessionService::class.java).apply {
            stopService(this)
        }
    }
}

