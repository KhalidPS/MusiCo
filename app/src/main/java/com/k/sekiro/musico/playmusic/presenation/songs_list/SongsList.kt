package com.k.sekiro.musico.playmusic.presenation.songs_list

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.UiAction
import com.k.sekiro.musico.playmusic.presenation.model.DeletionType
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistWithSongsUi
import com.k.sekiro.musico.playmusic.presenation.util.component.DeleteDialog
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.PlaylistShelfCard
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.PlayedSongBottomBar
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.SelectedSongsBar
import com.k.sekiro.musico.playmusic.presenation.util.component.Song
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.SongsSearchBar
import com.k.sekiro.musico.playmusic.presenation.util.component.PlaylistSelectionBottomSheet
import com.k.sekiro.musico.playmusic.presenation.util.shareAudioFile
import com.k.sekiro.musico.ui.theme.Blue
import com.k.sekiro.musico.ui.theme.FavoritePlaylistColor
import com.k.sekiro.musico.ui.theme.RecentPlayListColor

@SuppressLint("RememberReturnType")
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
    onAddToNewPlaylist: (String) -> Unit = {},
    onAddToExistPlaylist: (Playlist) -> Unit = {},
    onAddSingleSong: (Long, Playlist?, String?) -> Unit,
    onShowcasePlaylists: () -> Unit = {},
    onClickFavOrRecent: (Long) -> Unit = {},
    onConfirmDeletion: () -> Unit = {},
    playlists: List<Playlist> = emptyList(),
    playlistWithSongs: List<PlaylistWithSongsUi> = emptyList(),
    recentPlaylistSongs: List<SongUi> = emptyList(),
    bottomClicked: Boolean = false
) {

    var songToDelete: SongUi? = null
    var songToAddFromMenu: SongUi? by remember { mutableStateOf(null) }
    val context = LocalContext.current


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
                animatedVisibilityScope = animatedVisibilityScope,
                bottomClicked = bottomClicked
            )
        }
    ) {
        BackHandler(enabled = selectModeEnabled) {
            onCancelSelectedSongs()
        }


        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(it)
        ) {

            var isShowDialog by remember { mutableStateOf(false) }
            var isShowSheet by remember { mutableStateOf(false) }

            var isShowDeleteDialog by remember { mutableStateOf(false) }


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
                    onDelete = {
                        isShowDialog = true
                    },
                    playlists = playlists,
                    onAddToNewPlaylist = onAddToNewPlaylist,
                    onAddToExistPlaylist = onAddToExistPlaylist,
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

            if (isShowDialog) {
                DeleteDialog(
                    onDismissRequest = { isShowDialog = false },
                    onConfirm = {
                        isShowDialog = false
                        onConfirmDeletion()
                    },
                    onIgnore = { isShowDialog = false }
                )
            }




            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = with(animatedVisibilityScope){
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .renderInSharedTransitionScopeOverlay(1f)
                        .animateEnterExit(
                            enter = fadeIn() + slideInVertically { startOffset -> -startOffset },
                            exit = fadeOut() +  slideOutVertically { target -> -target }
                        )
                }
            ) {

                if (playlistWithSongs.isEmpty()) return@Row

                val favoritePlaylist = playlistWithSongs[0]
                val otherPlaylist = remember(playlistWithSongs) {
                    if (playlistWithSongs.size > 2) {
                        playlistWithSongs.first { it.playlist.name != "Favorite" && it.playlist.name != "Recent" }
                    } else playlistWithSongs[0]
                }
                val otherPlaylistsCount = (playlistWithSongs.size - 2).coerceAtLeast(0)
                val recentPlaylistId = playlistWithSongs.getOrNull(1)?.playlist?.id ?: 2L

                PlaylistShelfCard(
                    modifier = Modifier.weight(1f),
                    accentColor = FavoritePlaylistColor,
                    icon = Icons.Default.Favorite,
                    title = "Favorite",
                    subtitle = "${favoritePlaylist.songs.size} songs",
                    coverUrl = favoritePlaylist.songs.firstOrNull()?.cover.orEmpty(),
                    onClick = { onClickFavOrRecent(favoritePlaylist.playlist.id) }
                )

                PlaylistShelfCard(
                    modifier = Modifier.weight(1f),
                    accentColor = Blue,
                    icon = Icons.AutoMirrored.Default.List,
                    title = "Playlists",
                    subtitle = if (otherPlaylistsCount == 0) "None yet" else "$otherPlaylistsCount playlists",
                    coverUrl = otherPlaylist.songs.firstOrNull()?.cover.orEmpty(),
                    onClick = onShowcasePlaylists
                )

                PlaylistShelfCard(
                    modifier = Modifier.weight(1f),
                    accentColor = RecentPlayListColor,
                    icon = Icons.TwoTone.Refresh,
                    title = "Recent",
                    subtitle = "${recentPlaylistSongs.size} songs",
                    coverUrl = recentPlaylistSongs.firstOrNull()?.cover.orEmpty(),
                    onClick = { onClickFavOrRecent(recentPlaylistId) }
                )

            }



            if (isShowDeleteDialog) {
                DeleteDialog(
                    title = "Deleting Audio",
                    description = "Are you sure from deleting this audio permanently ?!",
                    onDismissRequest = { isShowDeleteDialog = false },
                    onConfirm = {
                        onAction(
                            UiAction.DeletionConfirmClicked(
                                DeletionType.StorageDeletion(
                                    songToDelete
                                )
                            )
                        )
                        isShowDeleteDialog = false
                    },
                    onIgnore = { isShowDeleteDialog = false }
                )
            }


            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    songs,
                    key = { index, item -> item.id }
                ) { index, song ->

                    Song(
                        song = song,
                        onClick = {
                            if (selectModeEnabled) {
                                onSelectSong(song)
                            } else {
                                // val index2 = songs.indexOf(it)
                                onSongClicked(it, index)
                            }
                        },
                        onLongClicked = onSelectSong,
                        selectedSongs = selectedSongs,
                        onAddToPlaylistClicked = {
                            songToAddFromMenu = song
                            isShowSheet = true
                        },
                        onDeleteClicked = {
                            songToDelete = song
                            isShowDeleteDialog = true
                        },
                        onShareClick = { context.shareAudioFile(song.dataUri.toUri()) },
                        selectModeEnabled = selectModeEnabled,
                        sharedTransitionScope = this@SongsList,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }

            }

            if (isShowSheet) {
                PlaylistSelectionBottomSheet(
                    playlists = playlists,
                    onConfirm = {
                        if (songToAddFromMenu != null){
                            onAddSingleSong(songToAddFromMenu!!.id,it,null)
                        }
                    },
                    isShowAddPlaylistIcon = false,
                    onDismiss = {
                        isShowSheet = false
                    },
                )
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
            SongsList(
                songs = mockSongs.map { it.toSongUi() },
                animatedVisibilityScope = this,
                onAddSingleSong = {_,_,_ -> }
            )

        }
    }
}
