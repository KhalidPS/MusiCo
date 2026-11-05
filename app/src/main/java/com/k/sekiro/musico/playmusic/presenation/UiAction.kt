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
    /** A tapped song (Home list, playlist, artist/album) opened the player: play it - seek if it
     * isn't the current one, resume if it is but paused, leave it alone if it's playing. */
    data class PlayTappedSong(val index: Int): UiAction
    /** The now-playing pager settled on [index]: if that's another song, play it; if it's already
     * the current one (the pager catching up with a notification skip), do nothing. */
    data class SettleOnPage(val index: Int): UiAction
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