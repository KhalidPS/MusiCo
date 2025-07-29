package com.k.sekiro.musico.playmusic.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong

@Dao
interface PlaylistSongDao {

    @Insert(entity = PlaylistSong::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playlistSongRef: PlaylistSong)

    @Query("SELECT * FROM PlaylistSong WHERE songId = :songId AND playlistId = :playlistId")
    suspend fun getPlaylistSongRef(playlistId: Long,songId: Long): PlaylistSong

    @Delete
    suspend fun delete(playlistSongRef: PlaylistSong)
}