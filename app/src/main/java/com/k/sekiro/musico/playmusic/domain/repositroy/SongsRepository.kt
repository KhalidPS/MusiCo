package com.k.sekiro.musico.playmusic.domain.repositroy

import android.net.Uri
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.model.SongWithPlaylists

interface SongsRepository {

    suspend fun getAllStorageSongs(): List<Song>

    fun deleteSongsFromLocal2(songsIds: List<Long>): Boolean

    fun deleteSongsFromLocal(songsUri: List<Uri>)

    fun getSongsFromRoom(): List<Song>

    fun addSongs(songs: List<Song>)

    fun addSong(song: Song)

    fun deleteSong(song: Song)

    fun deleteSongs(songs: List<Song>)

    suspend fun getSong(songId: Long): Song?

    suspend fun getSongsWithPlaylist(): List<SongWithPlaylists>

    suspend fun getSongsWithPlaylist(songId: Long): SongWithPlaylists?

    fun startObservingSongChanges(onChange:() -> Unit)

    fun stopObservingSongChanges()

}