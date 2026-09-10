package com.k.sekiro.musico.playmusic.presenation.model

sealed interface SleepTimerMode {
    data class Duration(val totalMillis: Long) : SleepTimerMode
    object EndOfTrack : SleepTimerMode
}

sealed interface SleepTimerState {
    object Off : SleepTimerState
    data class Active(val mode: SleepTimerMode, val remainingMillis: Long) : SleepTimerState
}
