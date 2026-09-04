package com.k.sekiro.musico.playmusic.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.model.SongWithPlaylists

@Dao
interface SongsDao {

    // Plain (non-suspend) Room DAO methods run wherever the caller calls them, bypassing Room's
    // own TransactionExecutor - which is what actually serializes DB writes across threads. With
    // ViewModel's library rescan (triggered by a MediaStore ContentObserver, running on its own
    // Dispatchers.IO coroutine) and TransferService's sequential per-song writes both hitting
    // these unserialized methods concurrently on the app's single SQLite connection, one thread's
    // write can be silently lost - no exception, no delete call, just gone. Marking them suspend
    // routes them through CoroutinesRoom.execute() like PlaylistSongDao.insert() already does,
    // giving every write here the same serialization guarantee.
    @Query("SELECT * FROM Song order by addedDate DESC")
    suspend fun getAllSongs(): List<Song>

    // @Upsert, NOT @Insert(onConflict = REPLACE). SQLite implements REPLACE as *delete the
    // conflicting row, then insert* - and that internal delete fires ON DELETE CASCADE. PlaylistSong
    // references Song(id) with ON DELETE CASCADE, so re-adding an already-known song (which the
    // library rescan in ViewModel.getAllSongsFromLocal() does constantly - its Room snapshot goes
    // stale while the slow getAllStorageSongs() query runs, so freshly-imported songs look "new")
    // silently wiped every playlist membership that song had. Nothing logs it, no app-level delete
    // is ever called, and the Song row looks untouched afterwards because it's immediately
    // re-inserted - it just quietly loses its playlists. That's what was dropping transferred songs
    // from their playlist while leaving them in the library, and what emptied Favorite/Recent too.
    // @Upsert updates the existing row in place instead, so no delete, no cascade, memberships survive.
    @Upsert
    suspend fun addSongs(songs: List<Song>)

    @Upsert
    suspend fun addSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Delete
    suspend fun deleteSongs(songs: List<Song>)

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
