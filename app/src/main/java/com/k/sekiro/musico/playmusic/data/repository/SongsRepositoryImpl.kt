package com.k.sekiro.musico.playmusic.data.repository

import android.app.RecoverableSecurityException
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.k.sekiro.musico.playmusic.data.util.getSongsByUri
import com.k.sekiro.musico.playmusic.data.local.room.SongsDao
import com.k.sekiro.musico.playmusic.domain.repositroy.SongsRepository
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.domain.model.SongWithPlaylists
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongsRepositoryImpl(
    private val songsDao: SongsDao,
    private val context: Context
) : SongsRepository {

    private var contentObserver: ContentObserver? = null

    override suspend fun getAllStorageSongs(): List<Song> = withContext(Dispatchers.IO){
        // EXTERNAL_CONTENT_URI is MediaStore's synthetic "external" volume - a merged view over
        // every mounted shared volume, so one query covers built-in storage AND a removable SD
        // card. VOLUME_EXTERNAL_PRIMARY, used here before, is built-in storage only: on a device
        // with its music on an SD card the scan came back empty.
        getSongsByUri(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
    }

    override fun deleteSongsFromLocal2(songsIds: List<Long>): Boolean {

        val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selectionPlaceholders = songsIds.joinToString { "?" }
        val selection = "${MediaStore.Audio.Media._ID} IN ($selectionPlaceholders)"
        val selectionArgs = songsIds.map { it.toString() }.toTypedArray()

        val deletedRows = context.contentResolver.delete(
            audioUri,
            selection,
            selectionArgs
        )

        return deletedRows > 0
    }

    override fun deleteSongsFromLocal(songsUri: List<Uri>){
        val resolver = context.contentResolver
        try {
            for (uri in songsUri){
                resolver.delete(uri,null,null)
            }
        }catch (ex: SecurityException){
            throw ex
        }catch (ex: Exception){
            throw ex
        }

    }

    override suspend fun getSongsFromRoom(): List<Song>{
        return songsDao.getAllSongs()
    }

    override suspend fun addSongs(songs: List<Song>) {
        songsDao.addSongs(songs)
    }

    override suspend fun addSong(song: Song) {
        songsDao.addSong(song)
    }

    override suspend fun deleteSong(song: Song) {
        songsDao.deleteSong(song)
    }

    override suspend fun deleteSongs(songs: List<Song>) {
        songsDao.deleteSongs(songs)
    }

    override suspend fun getSong(songId: Long): Song? {
        return songsDao.getSong(songId)
    }

    override suspend fun getSongsWithPlaylist(): List<SongWithPlaylists> {
        return songsDao.getSongsWithPlaylist()
    }

    override suspend fun getSongsWithPlaylist(songId: Long): SongWithPlaylists? {
        return songsDao.getSongsWithPlaylist(songId)
    }

    override fun startObservingSongChanges(onChange:() -> Unit) {
        val contentResolver = context.contentResolver
        // Release any previous observer first. Overwriting the field without unregistering leaks
        // the old one: it stays registered with the resolver (still firing its own rescan on every
        // MediaStore change) while the only reference to it is gone, so stopObservingSongChanges()
        // can never unregister it. This is called from the state flow's onStart, which re-runs
        // whenever subscribers drop to zero for the WhileSubscribed timeout and come back - i.e.
        // on every background/resume - so without this the rescans multiply the longer the app runs.
        stopObservingSongChanges()
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                onChange()
            }
        }
        // All shared volumes, SD card included - matches getAllStorageSongs so a change on the
        // card triggers a rescan too.
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, contentObserver!!
        )

    }

    override fun stopObservingSongChanges() {
        val resolver = context.contentResolver
        contentObserver?.let { resolver.unregisterContentObserver(it) }
        contentObserver = null
        Log.e("ks","heyyy stop observing songs")
    }


}