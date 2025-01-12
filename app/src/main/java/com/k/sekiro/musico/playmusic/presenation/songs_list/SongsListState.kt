package com.k.sekiro.musico.playmusic.presenation.songs_list

import androidx.compose.runtime.Immutable
import com.k.sekiro.musico.playmusic.domain.model.Song


@Immutable
data class SongsListState(
    val songs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val favouritePlaylist: List<Song> = emptyList(),
    val recentPlaylist: List<Song> = emptyList(),
   // val playlists: List<Playlist> = emptyList(),
    val playedSong: Song? = null,
    val searchBarExpanded: Boolean = false,
    val searchBarValue: String = "",
    val searchBarPlaceHolderVisibility: Boolean = true,
)
