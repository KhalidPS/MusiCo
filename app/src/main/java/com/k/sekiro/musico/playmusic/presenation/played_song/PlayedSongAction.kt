package com.k.sekiro.musico.playmusic.presenation.played_song

sealed interface PlayedSongAction {
    object PlayPause: PlayedSongAction
    object SeekToNext: PlayedSongAction
    object SeekToPrevious: PlayedSongAction
    object SeekForward: PlayedSongAction
    object SeekBackward: PlayedSongAction
    data class ChangePlayType(val playType: PlayType): PlayedSongAction // shuffle , repeat one , repeat
    data class SeekTo(val position: Float): PlayedSongAction
    data class ChangeToOtherSong(val index: Int): PlayedSongAction
    data class UpdateProgress(val newProgress: Float): PlayedSongAction
    object OnMoreActionClicked: PlayedSongAction
    object OnDownArrowClicked: PlayedSongAction
}