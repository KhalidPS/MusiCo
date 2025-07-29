package com.k.sekiro.musico.playmusic.domain.repositroy

import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistWithSongs

interface PlaylistRepository {

    suspend fun addPlaylist(playlist: Playlist): Long

    suspend fun getPlaylist(playlistId: Long): Playlist?

    suspend fun getPlaylistWithSongs(): List<PlaylistWithSongs>

    suspend fun getPlaylistWithSongs(playlistId: Long): PlaylistWithSongs?

    suspend fun getAllPlaylists(): List<Playlist>
}