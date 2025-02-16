package com.k.sekiro.musico.playmusic.presenation.played_song

import com.k.sekiro.musico.playmusic.presenation.model.SongUi

data class PlayedSongState(
    val songs: List<SongUi> = emptyList(),
    val playedSong: SongUi? = null, //current or selected
    val playedSongIndex: Int = 0,
    val sliderProgress: Float = 0f,
    val passedTimeDuration: String = "",
    val playType: PlayType = PlayType.RepeatAll,
    val isPlaying: Boolean = false
)


enum class PlayType{
    RepeatOne,
    RepeatAll,
    Shuffle
}