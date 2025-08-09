package com.k.sekiro.musico.playmusic.presenation.showcase_playlists

import android.widget.Button
import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistWithSongs
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.PlaylistWithSongsUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.showcase_playlists.components.PlaylistItem
import com.k.sekiro.musico.playmusic.presenation.util.AddPlaylistDialog

@Composable
fun ShowcasePlaylists(
    modifier: Modifier = Modifier,
    playlists: List<PlaylistWithSongsUi> = emptyList(),
    onBackButtonClicked: () -> Unit = {},
    onAddPlaylistClicked: (String) -> Unit = {},
    onPlaylistItemClicked:(Long) -> Unit = {}
) {

    var isShowDialog by remember { mutableStateOf(false) }

    AddPlaylistDialog(
        playlists = playlists.map { it.playlist },
        isShowDialog = isShowDialog,
        onCancelClicked = { isShowDialog = false },
        onAddPlaylistClicked = {
            isShowDialog = false
            onAddPlaylistClicked(it)
        }
    )

    Column(
        modifier = modifier.fillMaxSize(),
    ) {

        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onBackButtonClicked,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBackIos,
                        contentDescription = "back button",
                    )
                }


                Text(
                    "Playlists",
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { isShowDialog = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryAdd,
                        contentDescription = "",
                    )
                }

            }

            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists) { playlist ->
                    if (playlist.playlist.name != "Favorite" && playlist.playlist.name != "Recent")
                    PlaylistItem(
                        playlist = playlist,
                        onClick = onPlaylistItemClicked
                    )

                }
            }

        }

    }

}


@Preview(
    showBackground = true
)
@Composable
private fun ShowcasePlaylistsPrev() {
    ShowcasePlaylists()
}


val mockPlaylists = listOf(
    PlaylistWithSongsUi(
        playlist = Playlist("Gym",id = 0), songs = listOf(
            mockSongs[0].toSongUi()
        )
    ), PlaylistWithSongsUi(
        playlist = Playlist("Study",id = 1), songs = listOf(
            mockSongs[0].toSongUi()
        )
    ), PlaylistWithSongsUi(
        playlist = Playlist("Favorite", id = 2), songs = listOf(
            mockSongs[0].toSongUi()
        )
    ), PlaylistWithSongsUi(
        playlist = Playlist("Mode",id = 3), songs = listOf(
            mockSongs[0].toSongUi()
        )
    ), PlaylistWithSongsUi(
        playlist = Playlist("Motivation",id = 4), songs = listOf(
            mockSongs[0].toSongUi()
        )
    )
)