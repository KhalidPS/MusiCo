package com.k.sekiro.musico.playmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistWithSongs

@Dao
interface PlaylistDao {
    @Insert(entity = Playlist::class,onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playlist: Playlist): Long

    @Query("SELECT * FROM Playlist WHERE id = :playlistId")
    suspend fun getPlaylist(playlistId: Long): Playlist?

    @Transaction
    @Query("SELECT * FROM Playlist")
    suspend fun getPlaylistWithSongs(): List<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    suspend fun getPlaylistWithSongs(playlistId: Long): PlaylistWithSongs?

    @Query("SELECT * FROM Playlist")
    suspend fun getAllPlaylists(): List<Playlist>
}