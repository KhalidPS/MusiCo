package com.k.sekiro.musico.playmusic.presenation

sealed interface UiEvents {
    data class Message(val msg: String): UiEvents
    data class Error(val error: kotlin.Error) : UiEvents
}