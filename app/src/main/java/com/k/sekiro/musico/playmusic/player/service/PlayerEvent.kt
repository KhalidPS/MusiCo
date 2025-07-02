package com.k.sekiro.musico.playmusic.player.service

import com.k.sekiro.musico.playmusic.presenation.PlayType

sealed interface PlayerEvent {
    object PlayPause : PlayerEvent
    object SelectedAudioChange : PlayerEvent
    object Backward : PlayerEvent
    object SeekToNext : PlayerEvent
    object SeekToPrevious : PlayerEvent
    object Forward : PlayerEvent
    object SeekTo : PlayerEvent
    object Stop : PlayerEvent
    data class UpdateProgress(val newProgress: Float) : PlayerEvent
    data class ChangePlayType(val type: PlayType) : PlayerEvent
    object ClickNotification : PlayerEvent
}

sealed interface PlayerState {
    object Initial : PlayerState
    data class Ready(val duration: Long) : PlayerState
    data class Progress(val progress: Long) : PlayerState
    data class Buffering(val progress: Long) : PlayerState
    data class Playing(val isPlaying: Boolean) : PlayerState
    data class CurrentPlaying(val mediaItemIndex: Int) : PlayerState
    data class PlayingType(val type: PlayType) : PlayerState
}