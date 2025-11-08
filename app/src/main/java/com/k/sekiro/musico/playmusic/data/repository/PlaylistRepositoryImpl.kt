package com.k.sekiro.musico.playmusic.data.repository

import com.k.sekiro.musico.playmusic.data.local.room.PlaylistDao
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistWithSongs
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistRepository
import kotlinx.coroutines.flow.Flow

class PlaylistRepositoryImpl(private val playlistDao: PlaylistDao): PlaylistRepository {
    override suspend fun addPlaylist(playlist: Playlist): Long {
        return playlistDao.insert(playlist)
    }

    override suspend fun getPlaylist(playlistId: Long): Playlist? {
        return playlistDao.getPlaylist(playlistId)
    }

    override suspend fun getPlaylistWithSongs(): Flow<List<PlaylistWithSongs>> {
        return playlistDao.getPlaylistWithSongs()
    }

    override suspend fun getPlaylistWithSongs(playlistId: Long): PlaylistWithSongs? {
        return playlistDao.getPlaylistWithSongs(playlistId)
    }

    override suspend fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists()
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.delete(playlist)
    }

}