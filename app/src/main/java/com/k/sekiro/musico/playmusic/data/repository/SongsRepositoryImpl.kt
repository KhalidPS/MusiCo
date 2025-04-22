package com.k.sekiro.musico.playmusic.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.k.sekiro.musico.core.data.util.getSongsByUri
import com.k.sekiro.musico.playmusic.data.local.SongsDao
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongsRepositoryImpl(
    private val songsDao: SongsDao,
    private val context: Context
) : SongsRepository {
    override suspend fun getAllStorageSongs(): List<Song> = withContext(Dispatchers.IO){

        val resolver = context.contentResolver
        val songList = ArrayList<Song>()


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


        launch{ getSongsByUri(resolver,internalAudioUri,songList) }
        launch{ getSongsByUri(resolver,externalAudioUri,songList) }


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


}