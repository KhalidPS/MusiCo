package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k.sekiro.musico.playmusic.domain.model.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectionBottomSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit = {},
    onConfirm: (Playlist) -> Unit = {},
    onAddPlaylist:() -> Unit = {}
) {
    val defaultOption = Playlist(name = "Select a playlist")
    val fullCarList = listOf(defaultOption) + playlists
    var selectedPlaylist by remember { mutableStateOf(defaultOption) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isConfirmButtonEnabled = selectedPlaylist != defaultOption

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ){
                IconButton(onClick = onAddPlaylist) {
                    Icon(
                        imageVector = Icons.Default.AddToPhotos,
                        contentDescription = "add playlist icon"
                    )
                }
            }

            Text(
                text = "Select a playlist",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CustomWheelPicker(
                items = fullCarList,
                onItemSelected = { playlist ->
                    selectedPlaylist = playlist
                    Log.e("ks", "selected one : $playlist")
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    onConfirm(selectedPlaylist)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isConfirmButtonEnabled
            ) {
                Text(text = "Confirm")
            }
        }
    }
}