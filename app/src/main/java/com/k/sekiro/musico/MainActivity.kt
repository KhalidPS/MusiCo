package com.k.sekiro.musico

import android.Manifest
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.palette.graphics.Palette
import com.k.sekiro.musico.playmusic.data.repository.SongsRepositoryImpl
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.presenation.loading_screen.LoadingScreen
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongScreen
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
    override fun onCreate(savedInstanceState: Bundle?) {


        val lruCache: LruCache<String, Palette> by inject()
        val repo: SongsRepository by inject()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO),0)
        }else{
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),0)

        }





        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusiCoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val playedSongViewModel: PlayedSongViewModel = koinViewModel()
                    val songs = playedSongViewModel.songList.collectAsState(emptyList())

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
                            if (!songs.value.isEmpty()){
                                PlayedSongScreen(lurCache = lruCache, songs = songs.value)
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
}

