package com.k.sekiro.musico.playmusic.presenation

import android.net.Uri

sealed interface UiEvents {
    data class Message(val msg: String): UiEvents
    data class Error(val error: kotlin.Error) : UiEvents
    data class IntentSender(val exception: SecurityException,val uris: List<Uri>): UiEvents
}