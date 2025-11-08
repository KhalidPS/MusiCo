package com.k.sekiro.musico.playmusic.data.repository

import com.k.sekiro.musico.playmusic.data.local.room.PlaylistSongDao
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistSongRepository
import kotlinx.coroutines.flow.Flow

class PlaylistSongRepositoryImpl(private val playlistSongDao: PlaylistSongDao): PlaylistSongRepository {
    override suspend fun addPlaylistSongRef(playlistSongRef: PlaylistSong) {
        playlistSongDao.insert(playlistSongRef)
    }

    override suspend fun getPlaylistSongRef(
        playlistId: Long,
        songId: Long
    ): PlaylistSong? {
        return playlistSongDao.getPlaylistSongRef(playlistId,songId)
    }

    override suspend fun deletePlaylistSongRef(playlistSongRef: PlaylistSong) {
        playlistSongDao.delete(playlistSongRef)
    }

    override suspend fun deletePlaylistSongRefs(playlistSongRefs: List<PlaylistSong>) {
        playlistSongDao.deleteGroup(playlistSongRefs)
    }

    override fun getRecentPlaylistSongs(): Flow<List<Song>> {
        return playlistSongDao.getRecentPlaylistSongs()
    }


}