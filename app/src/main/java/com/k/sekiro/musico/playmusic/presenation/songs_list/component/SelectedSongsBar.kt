package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.presenation.util.component.AddPlaylistDialog
import com.k.sekiro.musico.playmusic.presenation.util.component.PlaylistSelectionBottomSheet


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedSongsBar(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    onDelete: () -> Unit = {},
    playlists: List<Playlist> = emptyList(),
    onAddToNewPlaylist: (String) -> Unit = {},
    onAddToExistPlaylist: (Playlist) -> Unit = {}
) {

    var isShowSheet by remember { mutableStateOf(false) }
    var isShowDialog by remember { mutableStateOf(false) }


    AddPlaylistDialog(
        playlists = playlists,
        isShowDialog = isShowDialog,
        onCancelClicked = { isShowDialog = false },
        onAddPlaylistClicked = {
            isShowDialog = false
            isShowSheet = false
            onAddToNewPlaylist(it)
        }
    )

    Row(modifier.fillMaxWidth()) {
        if (isShowSheet){
            PlaylistSelectionBottomSheet(
                playlists = playlists,
                onDismiss = { isShowSheet = false },
                onConfirm = onAddToExistPlaylist,
                onAddPlaylist = { isShowDialog = true }
            )
    }



    IconButton(
        onClick = onCancel
    ) {
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = "cancel icon"
        )
    }


    Spacer(Modifier.weight(1f))

    IconButton(
        onClick = { isShowSheet = true }
    ) {
        Icon(
            imageVector = Icons.Default.LibraryAdd,
            contentDescription = "add to playlist Icon"
        )
    }

    IconButton(
        onClick = onDelete
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "delete"
        )
    }


}
}

@Preview
@Composable
private fun SelectedSongsBarPrev() {
    SelectedSongsBar()
}