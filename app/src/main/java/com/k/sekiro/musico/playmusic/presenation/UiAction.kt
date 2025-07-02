package com.k.sekiro.musico.playmusic.presenation

sealed interface UiAction {
    object PlayPause: UiAction
    object SeekToNext: UiAction
    object SeekToPrevious: UiAction
    object SeekForward: UiAction
    object SeekBackward: UiAction
    data class ChangePlayType(val playType: PlayType): UiAction // shuffle , repeat one , repeat
    data class SeekTo(val position: Float): UiAction
    data class ChangeToOtherSong(val index: Int): UiAction
    data class UpdateProgress(val newProgress: Float): UiAction
    object OnMoreActionClicked: UiAction
    object OnDownArrowClicked: UiAction
}


//data class OnSongClicked(song:SongUi)
//object OnMoreClicked
//object OnShareClicked