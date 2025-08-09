package com.k.sekiro.musico.playmusic.presenation.songs_list

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.UiAction
import com.k.sekiro.musico.playmusic.presenation.UiState
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.PlayListBox
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.PlayedSongBottomBar
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.SelectedSongsBar
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.Song
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.SongsSearchBar
import com.k.sekiro.musico.ui.theme.Green2
import com.k.sekiro.musico.ui.theme.Red2
import com.k.sekiro.musico.ui.theme.SkyBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SongsList(
    modifier: Modifier = Modifier,
    songs: List<SongUi>,
    //state: UiState,
    song: SongUi = songs[0],
    isPlaying: Boolean = false,
    progress: () -> Float = { 180f },
    currentPosition: () -> Long = { 0L },
    selectModeEnabled: Boolean = false,
    selectedSongs: List<SongUi> = emptyList(),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSongClicked: (SongUi, index: Int) -> Unit = { _, _ -> },
    onSelectSong: (SongUi) -> Unit = {},
    onPlayClicked: () -> Unit = {},
    onBottomBarClicked: () -> Unit = {},
    onCancelSelectedSongs: () -> Unit = {},
    onAction: (UiAction) -> Unit = {},
    onAddToNewPlaylist:(String) -> Unit = {},
    onAddToExistPlaylist:(Playlist) -> Unit = {},
    onShowcasePlaylists:() -> Unit = {},
    playlists: List<Playlist> = emptyList()
) {

    Scaffold(
        bottomBar = {
            PlayedSongBottomBar(
                song = song,
                isPlaying = isPlaying,
                onPlayClicked = onPlayClicked,
                progress = progress,
                currentPosition = currentPosition,
                onClicked = onBottomBarClicked,
                onAction = onAction,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(it)
        ) {


            AnimatedVisibility(
                selectModeEnabled,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                SelectedSongsBar(
                    onCancel = onCancelSelectedSongs,
                    playlists = playlists,
                    onAddToNewPlaylist = onAddToNewPlaylist,
                    onAddToExistPlaylist = onAddToExistPlaylist
                )
            }

            AnimatedVisibility(
                !selectModeEnabled,
                exit = shrinkVertically()
            ) {
                SongsSearchBar(
                    songs = songs,
                    onSongClicked = onSongClicked
                )
            }



            Spacer(Modifier.height(16.dp))


            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {

                PlayListBox(
                    boxColor = SkyBlue,
                    latestSongImagePerPlayList = R.drawable.funk,
                    playListIconTint = Color.Red,
                    playListIcon = Icons.Default.Favorite,
                    playListName = "Favorite",
                )


                PlayListBox(
                    boxColor = Red2,
                    latestSongImagePerPlayList = R.drawable.logo_2,
                    playListIcon = Icons.AutoMirrored.Default.List,
                    playListName = "Playlists",
                    onClick = onShowcasePlaylists,
                )


                PlayListBox(
                    boxColor = Green2,
                    latestSongImagePerPlayList = R.drawable.funk,
                    playListIcon = Icons.TwoTone.Refresh,
                    playListName = "Recent",
                )

            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                itemsIndexed(songs) { index, song ->
                    Song(
                        song = song,
                        onClick = {
                            if (selectModeEnabled) {
                                onSelectSong(song)
                            } else {
                                val index2 = songs.indexOf(it)
                                onSongClicked(it, index2)
                            }
                        },
                        onLongClicked = onSelectSong,
                        selectedSongs = selectedSongs,
                        selectModeEnabled = selectModeEnabled
                    )
                }

            }


        }
    }

}


@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun SongsListPrev() {
    SharedTransitionLayout {
        AnimatedVisibility(true) {
            SongsList(songs = mockSongs.map { it.toSongUi() }, animatedVisibilityScope = this)

        }
    }
}
