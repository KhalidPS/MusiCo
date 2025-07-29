package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.k.sekiro.musico.playmusic.presenation.model.SongUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsSearchBar(
    onSongClicked:(SongUi,Int) -> Unit,
    songs: List<SongUi>
) {
    var isExpanded by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }

    SearchBar(
        inputField = {
            BasicTextField(
                value = value,
                onValueChange = {
                    value = it
                    isExpanded = it.isNotBlank() || it.isNotEmpty()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .padding(20.dp),
                decorationBox = {
                    Box {
                        if (value.isEmpty() || value.isBlank()) {
                            Text(
                                text = "Search",
                                color = Color.Gray
                            )
                        }
                        it()
                    }
                }
            )
        },
        expanded = isExpanded,
        onExpandedChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                isExpanded = it.isFocused && value.isNotBlank()
            }
            .padding(horizontal = 4.dp),
    ) {
        val filteredSongs = songs.filter { it.name.contains(value, true) }

        LazyColumn {
            itemsIndexed(filteredSongs) { index, song ->
                Song(
                    song = song,
                    onClick = {
                        val index2 = songs.indexOf(it)
                        onSongClicked(it, index2)

                    },
                )
            }
        }

    }
}