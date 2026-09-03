package com.k.sekiro.musico.playmusic.presenation.model

import kotlinx.serialization.Serializable

/** Nav route: show a QR code for the playlist with this id so another device can scan it. */
@Serializable
data class PlaylistQrExport(val playlistId: Long)
