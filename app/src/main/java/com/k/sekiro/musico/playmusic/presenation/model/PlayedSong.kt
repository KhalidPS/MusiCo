package com.k.sekiro.musico.playmusic.presenation.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayedSong(val index: Int,val isFromPlaylist: Boolean = false,val playlistId: Long = -1)