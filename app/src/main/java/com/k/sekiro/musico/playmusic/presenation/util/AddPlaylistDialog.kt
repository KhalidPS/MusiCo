package com.k.sekiro.musico.playmusic.presenation.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.k.sekiro.musico.playmusic.domain.model.Playlist

@Composable
fun AddPlaylistDialog(
    modifier: Modifier = Modifier,
    playlists: List<Playlist>,
    isShowDialog: Boolean,
    onAddPlaylistClicked:(String) -> Unit,
    onCancelClicked:() -> Unit
) {
   // var isShowDialog by remember { mutableStateOf(false) }
    var txt by remember { mutableStateOf("") }
    val isError = remember(
        playlists,
        txt
    ) { (playlists.find { it.name == txt } != null) }



    if (isShowDialog) {
        Dialog(onDismissRequest = onCancelClicked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    "Enter playlist name",
                    fontSize = 20.sp
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = txt,
                    onValueChange = {
                        txt = it
                    },
                )

                if (isError){
                    Text(
                        "This name is used already",
                        color = Color.Red
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    Button(
                        onClick = { onAddPlaylistClicked(txt) },
                        enabled = !isError && (txt.isNotEmpty() || txt.isNotBlank())
                    ) {
                        Text("Add")
                    }


                    Button(
                        onClick = onCancelClicked,
                    ){
                        Text("Cancel")
                    }

                }


            }
        }
    }
}