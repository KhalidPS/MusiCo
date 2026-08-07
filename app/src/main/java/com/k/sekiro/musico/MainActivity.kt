package com.k.sekiro.musico

import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.k.sekiro.musico.playmusic.presenation.model.DeletionType
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    val lruCache: LruCache<String, Palette> by inject()
    private val viewModel: ViewModel by viewModel()
    private val deletePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.e("ks", "granted permission")
                viewModel.onCancelAllSelectedSongs()
                Toast.makeText(
                    this, "Deleted Successfully", Toast.LENGTH_SHORT
                ).show()
                // 2. User granted permission!
                // Tell the ViewModel to retry the delete operation.
                //viewModel.retryDelete()
            } else {
                Log.e("ks", "denied permission")

                // User denied permission.
                // showToast("Deletion permission denied.")
            }
        }


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

                            is UiEvents.IntentSender -> {

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intentSender = MediaStore.createDeleteRequest(
                                        contentResolver,
                                        event.uris
                                    )
                                    val request = IntentSenderRequest.Builder(intentSender).build()
                                    deletePermissionLauncher.launch(request)

                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val recoverableSecurityException =
                                        event.exception as? RecoverableSecurityException
                                    val intentSender =
                                        recoverableSecurityException?.let { it.userAction.actionIntent.intentSender }
                                    if (intentSender != null) {
                                        val request =
                                            IntentSenderRequest.Builder(intentSender).build()
                                        deletePermissionLauncher.launch(request)

                                    }


                                }
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
                                        Log.e("ks", "enter collect block")
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
                                    var bottomClicked by rememberSaveable { mutableStateOf(false) }
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
                                                bottomClicked = false
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
                                            onConfirmDeletion = {
                                                viewModel.onAction(
                                                    UiAction.DeletionConfirmClicked(
                                                        DeletionType.StorageDeletion()
                                                    )
                                                )
                                            },
                                            onBottomBarClicked = {
                                                bottomClicked = true
                                                handleBottomBarClicked(
                                                    playedSong, songs, navController
                                                )
                                            },
                                            animatedVisibilityScope = this,
                                            onAction = viewModel::onAction,
                                            playlists = playlists,
                                            onAddToNewPlaylist = viewModel::onAddToNewPlaylist,
                                            onAddToExistPlaylist = viewModel::onAddToExistPlaylist,
                                            onAddSingleSong = viewModel::addSingleSongToPlaylist,
                                            playlistWithSongs = playlistWithSongs,
                                            onShowcasePlaylists = {
                                                navController.navigate(PlaylistShowcase) {
                                                    launchSingleTop = true
                                                    popUpTo(Home)
                                                }
                                            },
                                            onClickFavOrRecent = {
                                                viewModel.onCancelAllSelectedSongs()
                                                navController.navigate(
                                                    PlaylistScreen(it)
                                                ) {
                                                    launchSingleTop = true
                                                    popUpTo(Home)
                                                }
                                            },
                                            recentPlaylistSongs = recentPlaylistSongs,
                                            bottomClicked = bottomClicked
                                        )
                                    } else {
                                        LoadingScreen()
                                    }
                                }


                                composable<PlayedSong>(
                                    popEnterTransition = {
                                        fadeIn(tween(350, easing = LinearEasing))
                                    },
                                    enterTransition = {
                                        fadeIn(tween(350, easing = LinearEasing))
                                    },
                                    exitTransition = {
                                        fadeOut(tween(350, easing = LinearEasing))
                                    },
                                    popExitTransition = {
                                        fadeOut(tween(350, easing = LinearEasing))
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
                                    val launchedFromBottomBar = it.toRoute<PlayedSong>().launchedFromBottomBar

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
                                    val favoriteSongs =
                                        state.value.playlistsWithSongs.find { it.playlist.id == 1L }
                                            ?: return@composable

                                    PlayedSongScreen(
                                        lurCache = lruCache,
                                        sliderProgress = { progress },
                                        isPlaying = isPlaying,
                                        playType = state.value.playType,
                                        passedTimeDuration = { passedTime },
                                        playedSong = playedSong,
                                        songs = songs,
                                        favoriteSongs = favoriteSongs.songs,
                                        onAction = viewModel::onAction,
                                        index = index,
                                        launchedFromBottomBar = launchedFromBottomBar,
                                        animatedVisibilityScope = this,
                                        onDownArrowClicked = {
                                            navController.popBackStack(
                                                route = Home,
                                                inclusive = false
                                            )
                                        },
                                        onSettledPageChanged = viewModel::addToRecent
                                    )

                                }

                                composable<PlaylistShowcase> {
                                    val playlistsWithSongs = state.value.playlistsWithSongs
                                    ShowcasePlaylists(
                                        playlists = playlistsWithSongs,
                                        onBackButtonClicked = {
                                            navController.popBackStack(
                                                route = Home,
                                                inclusive = false
                                            )
                                        },
                                        onAddPlaylistClicked = viewModel::addNewPlaylist,
                                        onPlaylistItemClicked = {
                                            viewModel.onCancelAllSelectedSongs()
                                            navController.navigate(PlaylistScreen(it)) {
                                                launchSingleTop = true
                                                popUpTo(PlaylistShowcase)
                                            }
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

                                    val selectedSongs = state.value.selectedSongs
                                    val selectModeEnabled = state.value.selectModeEnabled
                                    val playlists = state.value.playlists

                                    PlaylistCollapsingScreen(
                                        playlistWithSongsUi = playlist ?: return@composable,
                                        onBackButtonClicked = {
                                            val name = playlist.playlist.name.lowercase()
                                            if (name == "favorite" || name == "recent")
                                            navController.popBackStack(route = Home, inclusive = false)
                                            else navController.popBackStack(route = PlaylistShowcase, inclusive = false)
                                        },
                                        onSongClicked = { index, song ->
                                            //val currentPlayedIndex = playlist.songs.indexOf(state.value.playedSong)
                                            //if (song.path != controller?.currentMediaItem?.mediaId || index != controllerManager.getController()!!.currentMediaItemIndex) {

                                            handelPlaylistSongClicked(
                                                index = index,
                                                currentPlaylistId = currentPlaylistId,
                                                playlistId = playlistId,
                                                song = song,
                                                navController = navController,
                                                playlist = playlist,
                                            )

                                        },
                                        onAction = viewModel::onAction,
                                        onSelectSong = viewModel::onSelectSong,
                                        selectedSongs = selectedSongs,
                                        selectModeEnabled = selectModeEnabled,
                                        onCancelClicked = viewModel::onCancelAllSelectedSongs,
                                        playlists = playlists,
                                        onAddToExistPlaylist = viewModel::onAddToExistPlaylist,
                                        onAddToNewPlaylist = viewModel::onAddToNewPlaylist,
                                        onAddSingleSong = viewModel::addSingleSongToPlaylist,
                                        onConfirmDeletePlaylist = {
                                            viewModel.deletePlaylist(it)
                                            navController.popBackStack()
                                        },
                                        animatedVisibilityScope = this,
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

        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        Log.e("ks", "manufacturer >>>>>>>> $manufacturer")
        Log.e("ks", "model >>>>>>>> $model")
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
                playlistId = viewModel.currentPlaylistId(),
                launchedFromBottomBar = true
            )
        ) {
            launchSingleTop = true
            popUpTo(Home)
        }


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

        if (viewModel.getController()!!.currentMediaItemIndex != index && viewModel.getIsNewCreation()) {
            /** if the creation for activity is new and for first time then
            set new mediaItems then reset the isNewCreation to false cuz if the
            user click the item again there is no need to set mediaItems again since we did that before
            and the activity is already created but if we remove the checking for new creation, then
            every time the user click the item the playing for item will start again from scratch
            instead of continue playing due to setMediaItems every click**/

            viewModel.getController()!!.setMediaItemsList(songs)
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
            viewModel.getController()!!
                .setMediaItemsList(
                    songs,
                    startIndex = index,
                    startProgress = 0L,
                )
            viewModel.updateIsSelectedSongFromPlaylist(
                value = false,
                songs = songs
            )
            viewModel.updatePlayedSong(index)
            viewModel.addToRecent(song.id)
        }
        navController.navigate(PlayedSong(index)) {
            launchSingleTop = true
            popUpTo(Home)
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
        if (viewModel.getIsNewCreation()) viewModel.setIsNewCreation(false)

        /** Resync the cached playlist snapshot (currentPlayedPlaylistSong) to the playlist's
        current order before navigating, even if we don't touch the controller. Otherwise,
        re-clicking the song that's already playing (song.path == currentMediaItem and
        playlistId == currentPlaylistId) would leave the ViewModel's cached songs list stale
        (e.g. from before this song got bumped to the top of Recent), and PlayedSongScreen
        would read that stale list at the fresh `index`.

        The controller's actual media items must also be resynced whenever the playlist's
        order changed (`previousPlaylistSongs != playlist.songs`), not just when the song or
        playlistId changed - otherwise the controller keeps the song at its OLD index, and the
        pager's settle effect (ChangeToOtherSong) ends up seeking the controller to `index`
        within that stale, un-reordered list: the UI shows the right song (it's built from the
        freshly-synced snapshot) but playback comes from whatever used to sit at that index.**/
        val previousPlaylistSongs = viewModel.currentPlaylistSongs()
        viewModel.updateIsSelectedSongFromPlaylist(
            true,
            playlist.songs,
            playlistId
        )

        if (
            playlistId != currentPlaylistId ||
            song.path != controller.currentMediaItem?.mediaId ||
            previousPlaylistSongs != playlist.songs
        ) {
            Log.e(
                "ks",
                "the index : $index , playlistId: $playlistId"
            )
            controller
                .setMediaItemsList(
                    startIndex = index,
                    songs = playlist.songs,
                    startProgress = 0L
                )

        }
        navController.navigate(
            PlayedSong(
                index,
                isFromPlaylist = true,
                playlistId = playlist.playlist.id
            )
        ) {
            launchSingleTop = true
        }
    }
}










