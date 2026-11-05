package com.k.sekiro.musico.playmusic.presenation

import com.k.sekiro.musico.playmusic.presenation.model.DeletionType
import com.k.sekiro.musico.playmusic.presenation.model.SongUi

sealed interface UiAction {
    object PlayPause: UiAction
    object SeekToNext: UiAction
    object SeekToPrevious: UiAction
    object SeekForward: UiAction
    object SeekBackward: UiAction
    data class ChangePlayType(val playType: PlayType): UiAction // shuffle , repeat one , repeat
    data class SeekTo(val position: Float): UiAction

    data class OnFavoriteClicked(val songUi: SongUi): UiAction
    data class ChangeToOtherSong(val index: Int): UiAction
    /** A song tapped in Home's list: play it - seek if it isn't the current one, resume if it is
     * but paused, leave it alone if it's already playing. Never toggles. */
    data class PlayFromList(val index: Int): UiAction
    data class UpdateProgress(val newProgress: Float): UiAction
    object OnMoreActionClicked: UiAction
    object OnDownArrowClicked: UiAction
    data class DeletionConfirmClicked(val deletionType: DeletionType) : UiAction

    data class StartSleepTimer(val durationMillis: Long) : UiAction
    object StartSleepTimerEndOfTrack : UiAction
    object CancelSleepTimer : UiAction
}


//data class OnSongClicked(song:SongUi)
//object OnMoreClicked
//object OnShareClicked