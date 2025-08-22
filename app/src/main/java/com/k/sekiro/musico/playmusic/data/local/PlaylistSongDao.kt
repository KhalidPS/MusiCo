package com.k.sekiro.musico.playmusic.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongDao {

    @Insert(entity = PlaylistSong::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlistSongRef: PlaylistSong)

    @Query("SELECT * FROM PlaylistSong WHERE songId = :songId AND playlistId = :playlistId")
    suspend fun getPlaylistSongRef(playlistId: Long,songId: Long): PlaylistSong

    @Query("""
        SELECT SongTable.* FROM (SELECT * FROM PlaylistSong WHERE playlistId = 2) as PlaylistSongTable
        join (SELECT * FROM Song) as SongTable on PlaylistSongTable.songId = SongTable.id order by PlaylistSongTable.millis Desc""")
    fun getRecentPlaylistSongs(): Flow<List<Song>>

    @Delete
    suspend fun delete(playlistSongRef: PlaylistSong)
}