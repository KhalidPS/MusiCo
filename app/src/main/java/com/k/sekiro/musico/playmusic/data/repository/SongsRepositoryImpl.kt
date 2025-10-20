package com.k.sekiro.musico.playmusic.data.repository

import android.content.Context
import android.database.ContentObserver
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongsRepositoryImpl(
    private val songsDao: SongsDao,
    private val context: Context
) : SongsRepository {

    private var contentObserver: ContentObserver? = null

    override suspend fun getAllStorageSongs(): List<Song> = withContext(Dispatchers.IO){

        val resolver = context.contentResolver
        val songList = ArrayList<Song>()
        /* prevent shared state problem using limitedParallelism or we can use mutex.withLock when adding new song
               to songList*/
        val dispatcher = Dispatchers.IO.limitedParallelism(1)

        //Songs from Internal storage (device storage)
        val internalAudioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_INTERNAL
            )
        } else {
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI
        }

        //Songs from External storage (SDCard memory)
        val externalAudioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }


        launch(dispatcher){ getSongsByUri(resolver,internalAudioUri,songList) }
        launch(dispatcher){ getSongsByUri(resolver,externalAudioUri,songList) }


       // Log.e("ks","the song list inside fun \n ${songList.filter { it.cover != null }}")

        songList
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