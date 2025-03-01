package com.k.sekiro.musico.playmusic.presenation.songs_list

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
import androidx.compose.foundation.lazy.items
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
import com.k.sekiro.musico.playmusic.data.repository.SongsRepositoryImpl
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.model.mockSongs
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.PlayListBox
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.PlayedSongBottomBar
import com.k.sekiro.musico.playmusic.presenation.songs_list.component.Song
import com.k.sekiro.musico.ui.theme.Green2
import com.k.sekiro.musico.ui.theme.Orange
import com.k.sekiro.musico.ui.theme.Orange1
import com.k.sekiro.musico.ui.theme.PlayListColor
import com.k.sekiro.musico.ui.theme.Purple40
import com.k.sekiro.musico.ui.theme.Purple80
import com.k.sekiro.musico.ui.theme.RecentPlayListColor
import com.k.sekiro.musico.ui.theme.Red2
import com.k.sekiro.musico.ui.theme.SkyBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsList(
    modifier: Modifier = Modifier,
    //repositoryImpl: SongsRepository
    songs: List<SongUi>,
    onSongClicked:(SongUi)-> Unit = {}
) {

    Scaffold (
        //bottomBar = { PlayedSongBottomBar() }
    ){
        Column(
            modifier = modifier.fillMaxSize().padding(it)
        ) {

            var isExpanded by remember { mutableStateOf(false) }
            var value by remember { mutableStateOf("") }
            val context = LocalContext.current


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
                            Box{
                                if(value.isEmpty() || value.isBlank()){
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
                    .padding(horizontal = 4.dp)
            ) {
                val filteredSongs =
                    mockSongs.filter { it.name.contains(value,true) }.map { it.toSongUi() }

                LazyColumn {
                    items(filteredSongs){
                        Song(
                            song = it,
                            onClick = onSongClicked
                            )
                    }
                }

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
                    playListName = "Favorite"
                )


                PlayListBox(
                    boxColor = Red2,
                    latestSongImagePerPlayList = R.drawable.song_cover,
                    playListIcon = Icons.AutoMirrored.Default.List,
                    playListName = "Playlists"
                )


                PlayListBox(
                    boxColor = Green2,
                    latestSongImagePerPlayList = R.drawable.logo_1,
                    playListIcon = Icons.TwoTone.Refresh,
                    playListName = "Recent"
                )

            }

            LazyColumn (
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ){

                items(songs){
                    Song(
                        song = it,
                        onClick = onSongClicked
                    )
                }

            }



        }
    }

}


@Preview
@Composable
private fun SongsListPrev() {
    SongsList(songs = mockSongs.map { it.toSongUi() })
}
