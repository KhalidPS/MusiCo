package com.k.sekiro.musico.playmusic.domain.repositroy

import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    suspend fun addPlaylist(playlist: Playlist): Long

    suspend fun getPlaylist(playlistId: Long): Playlist?

    suspend fun getPlaylistWithSongs(): Flow<List<PlaylistWithSongs>>

    suspend fun getPlaylistWithSongs(playlistId: Long): PlaylistWithSongs?

    suspend fun getAllPlaylists(): Flow<List<Playlist>>
}