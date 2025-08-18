package com.k.sekiro.musico.playmusic.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.model.SongWithPlaylists

@Dao
interface SongsDao {

    @Query("SELECT * FROM Song order by addedDate DESC")
    fun getAllSongs(): List<Song>

    @Insert(Song::class, onConflict = OnConflictStrategy.REPLACE)
    fun addSongs(songs: List<Song>)

    @Insert(Song::class, onConflict = OnConflictStrategy.REPLACE)
    fun addSong(song: Song)

    @Delete
    fun deleteSong(song: Song)

    @Delete
    fun deleteSongs(songs: List<Song>)

    @Query("SELECT * FROM Song WHERE id = :songId")
    suspend fun getSong(songId: Long): Song?

    @Transaction
    @Query("SELECT * FROM Song")
    suspend fun getSongsWithPlaylist(): List<SongWithPlaylists>

    @Transaction
    @Query("SELECT * FROM Song WHERE id = :songId")
    suspend fun getSongsWithPlaylist(songId: Long): SongWithPlaylists?
}

/** !! don't forget to solve index problem for (continue playing last song)
 *  when add new songs  to list after last visited time for app**/
