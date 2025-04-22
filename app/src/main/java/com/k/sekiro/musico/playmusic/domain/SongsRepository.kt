package com.k.sekiro.musico.playmusic.domain

import android.content.Context
import com.k.sekiro.musico.playmusic.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongsRepository {

   suspend fun getAllStorageSongs(): List<Song>

    fun getSongsFromRoom(): List<Song>

    fun addSongs(songs: List<Song>)

    fun addSong(song: Song)

    fun deleteSong(song: Song)

    fun deleteSongs(songs: List<Song>)

}