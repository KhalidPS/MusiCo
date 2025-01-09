package com.k.sekiro.musico.playmusic.presenation.played_song

import android.content.ContentResolver
import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayedSongViewModel(
    private val songsRepository: SongsRepository,
    private val resources: Resources,
    private val resolver: ContentResolver
) : ViewModel() {

    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    val songList = _songList
        .onStart {
            getAllSongsFromLocal()
        }
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000L
            )
        )

    private fun getAllSongsFromLocal() {
        viewModelScope.launch {

            _songList.update {
                //delay(1000)
                songsRepository.getAllStorageSongs()
                /*.filter { it.path.endsWith(".mp3")}.map {
                    val start = System.currentTimeMillis()
                async(Dispatchers.Default){it.toSongUi(resolver,resources)}
                }.awaitAll()*/
            }

        }
    }
}