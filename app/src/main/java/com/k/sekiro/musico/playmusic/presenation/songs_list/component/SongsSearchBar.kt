package com.k.sekiro.musico.playmusic.presenation.songs_list.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.util.component.Song

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){

                        if (isExpanded){
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.clickable(
                                    enabled = true,
                                    onClick = {
                                        isExpanded = false
                                        value = ""
                                    }
                                )
                            )
                        }

                        Spacer(Modifier.width(8.dp))

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
        val filteredSongs = songs.filter {
            it.name.contains(value, true) ||
                    it.title.contains(value, true) ||
                    it.artist.contains(value, true) ||
                    it.album.contains(value, true)
        }

        if (value.isNotBlank() && filteredSongs.isEmpty()) {
            EmptySearchResults(query = value)
        } else {
            LazyColumn {
                itemsIndexed(
                    filteredSongs,
                    key = { index, item -> item.id}
                ) { _, song ->
                    Song(
                        song = song,
                        onClick = {
                            onSongClicked(it, songs.indexOf(it))
                        },
                        showMoreIcon = false
                    )
                }
            }
        }

    }
}

@Composable
private fun EmptySearchResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No songs found for \"$query\"",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}