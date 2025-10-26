package com.k.sekiro.musico

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
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.palette.graphics.Palette
import com.k.sekiro.musico.playmusic.domain.model.Playlist
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
import com.k.sekiro.musico.playmusic.presenation.player.setMediaItemsList
import com.k.sekiro.musico.playmusic.presenation.playlist.PlaylistCollapsingScreen
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.PermissionGate
import com.k.sekiro.musico.playmusic.presenation.showcase_playlists.ShowcasePlaylists
import com.k.sekiro.musico.playmusic.presenation.songs_list.SongsList
import com.k.sekiro.musico.playmusic.presenation.util.ObserveAsEvent
import com.k.sekiro.musico.ui.theme.MusiCoTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    val lruCache: LruCache<String, Palette> by inject()
    private val viewModel: ViewModel by viewModel()


    @ExperimentalSharedTransitionApi
    @OptIn(ExperimentalSharedTransitionApi::class, UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("ks", "onActivity create")
        super.onCreate(savedInstanceState)


        lifecycle.addObserver(viewModel.getControllerManager())

        viewModel.initController()

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
                                        viewModel.controllerAndLastPlayedSongSetup(it)
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
                                                handleSongClicked(
                                                    song,
                                                    songs,
                                                    index,
                                                    navController
                                                )
                                            },
                                            progress = { progress },
                                            isPlaying = isPlaying,
                                            currentPosition = { currentPosition },
                                            song = playedSong ?: songs[0],
                                            selectModeEnabled = selectModeEnabled,
                                            onPlayClicked = {
                                                viewModel.onAction(UiAction.PlayPause)
                                            },
                                            onSelectSong = viewModel::onSelectSong,
                                            selectedSongs = selectedSongs,
                                            onCancelSelectedSongs = viewModel::onCancelAllSelectedSongs,
                                            onBottomBarClicked = {
                                                handleBottomBarClicked(
                                                    playedSong, songs, navController
                                                )
                                            },
                                            animatedVisibilityScope = this,
                                            onAction = viewModel::onAction,
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
                                        isFavorite = viewModel::isFavorite,
                                        onAction = viewModel::onAction,
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
                                            //if (song.path != controller?.currentMediaItem?.mediaId || index != controllerManager.getController()!!.currentMediaItemIndex) {

                                            handelPlaylistSongClicked(
                                                index = index,
                                                currentPlaylistId = currentPlaylistId,
                                                playlistId = playlistId,
                                                song = song,
                                                navController = navController,
                                                playlist = playlist
                                            )

                                        }
                                    )

                                }

                            }
                        }

                    }


                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.e("ks", "onActivity start")

    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel.getControllerManager())
        super.onDestroy()
    }



    private fun handleBottomBarClicked(
        playedSong: SongUi?,
        songs: List<SongUi>,
        navController: NavHostController
    ) {
        val index =
            if (playedSong != null && !viewModel.isSelectedSongFromPlaylist()) {
                songs.indexOf(playedSong)
            } else if (playedSong != null && viewModel.isSelectedSongFromPlaylist()) {
                viewModel.currentPlaylistSongs()
                    .indexOf(playedSong)
            } else return

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
    }


    private fun handleSongClicked(
        song: SongUi,
        songs: List<SongUi>,
        index: Int,
        navController: NavHostController
    ) {

        var job: Job? = null
        if (viewModel.getController()!!.currentMediaItemIndex != index && viewModel.getIsNewCreation()) {
            /** if the creation for activity is new and for first time then
            set new mediaItems then reset the isNewCreation to false cuz if the
            user click the item again there is no need to set mediaItems again since we did that before
            and the activity is already created but if we remove the checking for new creation, then
            every time the user click the item the playing for item will start again from scratch
            instead of continue playing due to setMediaItems every click**/

            lifecycleScope.launch {
                viewModel.getController()!!
                    .setMediaItemsList(
                        songs,
                        this@MainActivity
                    )
            }
            viewModel.setIsNewCreation(false)
            viewModel.addToRecent(song.id)
        }

        if (!viewModel.isSelectedSongFromPlaylist() && viewModel.getController()!!.currentMediaItemIndex != index) {
            viewModel.updateIsSelectedSongFromPlaylist(
                value = false,
                songs = songs
            )
            viewModel.updatePlayedSong(index)
            viewModel.addToRecent(song.id)
        } else if (viewModel.isSelectedSongFromPlaylist()) {
           job =  lifecycleScope.launch {
                viewModel.getController()!!
                    .setMediaItemsList(
                        songs,
                        startIndex = index,
                        startProgress = 0L,
                        this@MainActivity
                    )
            }
            viewModel.updateIsSelectedSongFromPlaylist(
                value = false,
                songs = songs
            )
            viewModel.updatePlayedSong(index)
            viewModel.addToRecent(song.id)
        }

        lifecycleScope.launch {
            job?.join()
            navController.navigate(PlayedSong(index))
        }

    }


    private fun handelPlaylistSongClicked(
        index: Int,
        song: SongUi,
        playlist: PlaylistWithSongsUi,
        playlistId: Long,
        currentPlaylistId: Long,
        navController: NavHostController
    ) {

        val controller = viewModel.getController() ?: return

        if (playlistId != currentPlaylistId) {
            viewModel.updateIsSelectedSongFromPlaylist(
                true,
                playlist.songs,
                playlistId
            )

            lifecycleScope.launch {
                controller
                    ?.setMediaItemsList(
                        startIndex = index,
                        songs = playlist.songs,
                        context = this@MainActivity,
                        startProgress = 0L
                    )
            }
        } else if (song.path != controller?.currentMediaItem?.mediaId) {
            viewModel.updateIsSelectedSongFromPlaylist(
                true,
                playlist.songs,
                playlistId
            )

            Log.e(
                "ks",
                "the index : $index , playlistId: $playlistId"
            )
            lifecycleScope.launch {
                controller
                    ?.setMediaItemsList(
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

}










