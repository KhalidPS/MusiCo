package com.k.sekiro.musico.playmusic.domain.repositroy

import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistSongRepository {
    suspend fun addPlaylistSongRef(playlistSongRef: PlaylistSong)

    suspend fun getPlaylistSongRef(playlistId: Long, songId: Long): PlaylistSong?

    suspend fun deletePlaylistSongRef(playlistSongRef: PlaylistSong)

    suspend fun deletePlaylistSongRefs(playlistSongRefs: List<PlaylistSong>)

    fun getRecentPlaylistSongs(): Flow<List<Song>>
}