package com.k.sekiro.musico.playmusic.presenation.util.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.k.sekiro.musico.ui.theme.Red3

@Composable
fun OptionMenu(
    isExpanded: Boolean,
    onDismissRequest:() -> Unit,
    onAddToPlaylistClicked:() -> Unit,
    onShareClicked:() -> Unit,
    onDeleteClicked:() -> Unit,
) {


    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = onDismissRequest,
    ) {

        DropdownMenuItem(
            text = {
                Text("Add to playlist")
            },
            onClick = {
                onDismissRequest()
                onAddToPlaylistClicked()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AddToPhotos,
                    contentDescription = ""
                )
            }
        )



        DropdownMenuItem(
            text = {
                Text("Share")
            },
            onClick = {
                onDismissRequest()
                onShareClicked()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = ""
                )
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    "Delete",
                    color = Red3
                )
            },
            onClick = {
                onDismissRequest()
                onDeleteClicked()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "",
                    tint = Red3
                )
            }
        )

    }
}


@Preview
@Composable
private fun OptionMenuPrev() {
    OptionMenu(true,{},{},{},{})
}