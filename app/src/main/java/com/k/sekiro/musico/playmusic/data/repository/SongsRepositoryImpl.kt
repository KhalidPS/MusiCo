package com.k.sekiro.musico.playmusic.data.repository

import android.app.RecoverableSecurityException
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
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


        //Songs from External storage (device storage either primary or secondary like SDCard)
        val externalAudioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

       // Log.e("ks","the song list inside fun \n ${songList.filter { it.cover != null }}")

        getSongsByUri(context,externalAudioUri)

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

    override fun getSongsFromRoom(): List<Song>{
        return songsDao.getAllSongs()
    }

    override fun addSongs(songs: List<Song>) {
        songsDao.addSongs(songs)
    }

    override fun addSong(song: Song) {
        songsDao.addSong(song)
    }

    override fun deleteSong(song: Song) {
        songsDao.deleteSong(song)
    }

    override fun deleteSongs(songs: List<Song>) {
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
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                onChange()
            }
        }
        val externalAudioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        contentResolver.registerContentObserver(externalAudioUri,true,contentObserver!!)

    }

    override fun stopObservingSongChanges() {
        val resolver = context.contentResolver
        contentObserver?.let { resolver.unregisterContentObserver(it) }
        contentObserver = null
        Log.e("ks","heyyy stop observing songs")
    }


}