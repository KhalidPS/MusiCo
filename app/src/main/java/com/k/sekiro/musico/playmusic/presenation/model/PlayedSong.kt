package com.k.sekiro.musico.playmusic.presenation.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayedSong(
    val index: Int,
    val isFromPlaylist: Boolean = false,
    val playlistId: Long = -1,
    // Which shared-element key namespace PlayedSongScreen should bind to: the mini player
    // bottom bar (true) or the song list card (false, default) that triggered this navigation.
    val launchedFromBottomBar: Boolean = false
)