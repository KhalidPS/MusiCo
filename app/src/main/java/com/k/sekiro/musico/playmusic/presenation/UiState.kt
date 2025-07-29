package com.k.sekiro.musico.playmusic.presenation

import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.presenation.model.SongUi

data class UiState(
    val songs: List<SongUi> = emptyList(),
    val playedSong: SongUi? = null, //current or selected
    val playedSongIndex: Int = 0,
    val sliderProgress: Float = 0f,
    val passedTimeDuration: String = "",
    val currentPosition: Long = 0L,
    val playType: PlayType = PlayType.RepeatAll,
    val isPlaying: Boolean = false,
    val selectedSongs: List<SongUi> = emptyList(),
    val selectModeEnabled: Boolean = false,
    val playlists: List<Playlist> = emptyList()
)


enum class PlayType{
    RepeatOne,
    RepeatAll,
    Shuffle
}





/*    val songs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val favouritePlaylist: List<Song> = emptyList(),
    val recentPlaylist: List<Song> = emptyList(),
   // val playlists: List<Playlist> = emptyList(),
    val playedSong: Song? = null,
    val searchBarExpanded: Boolean = false,
    val searchBarValue: String = "",
    val searchBarPlaceHolderVisibility: Boolean = true,*/