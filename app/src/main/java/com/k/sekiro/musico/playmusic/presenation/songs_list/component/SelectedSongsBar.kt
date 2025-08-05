package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.exoplayer.upstream.Allocator
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.presenation.util.AddPlaylistDialog
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedSongsBar(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
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
        onClick = {}
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