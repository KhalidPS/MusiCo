package com.k.sekiro.musico.playmusic.data.local

import androidx.room.Dao
import androidx.room.Query
import com.k.sekiro.musico.playmusic.domain.model.Song

@Dao
interface SongsDao {

    @Query("SELECT * FROM Song")
    fun getAllSongs(): List<Song>
}
