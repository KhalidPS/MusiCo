package com.k.sekiro.musico.playmusic.presenation.played_song

import android.content.ContentResolver
import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.text.get

class PlayedSongViewModel(
    private val songsRepository: SongsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /* @OptIn(SavedStateHandleSaveableApi::class)
    var duration by savedStateHandle.saveable{ mutableLongStateOf(0L) }
    var progress by savedStateHandle.saveable{ mutableFloatStateOf(0f) }
    var durationString by savedStateHandle.saveable{ mutableStateOf("00:00") }*/

    private val stateKey = "playedSongState"

    private val _state = MutableStateFlow<PlayedSongState>(PlayedSongState())
    val state = _state
        .onStart {
            getAllSongsFromLocal()
        }
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000L
            )
        )

    private  var playedSong: SongUi? = null

    /*    private val _state = savedStateHandle.getStateFlow(stateKey, PlayedSongState())
    val state = _state
        .onStart {
            getAllSongsFromLocal()
        }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000
            )
        )*/


    fun onAction(action: PlayedSongAction) {
        when (action) {
            PlayedSongAction.ChangePlayType -> TODO()
            is PlayedSongAction.ChangeToOtherSong -> TODO()
            PlayedSongAction.OnDownArrowClicked -> TODO()
            PlayedSongAction.OnMoreActionClicked -> TODO()
            PlayedSongAction.PlayPause -> TODO()
            PlayedSongAction.SeekBackward -> TODO()
            PlayedSongAction.SeekForward -> TODO()
            is PlayedSongAction.SeekTo -> {
                /* viewModelScope.launch{
                   savedStateHandle.update<PlayedSongState>(stateKey){
                       it?.copy(
                           sliderProgress = action.position
                       )
                   }
               }*/
            }

            PlayedSongAction.SeekToNext -> TODO()
            PlayedSongAction.SeekToPrevious -> TODO()
            is PlayedSongAction.UpdateProgress -> TODO()
        }
    }


    private fun getAllSongsFromLocal() {
        viewModelScope.launch {
/*
            savedStateHandle[stateKey] = savedStateHandle.get<PlayedSongState>(stateKey)?.copy(
                songs = songsRepository.getAllStorageSongs().map { it.toSongUi() }
            )*/

            /*            savedStateHandle.update<PlayedSongState>(stateKey){
                it?.copy(
                    songs = songsRepository.getAllStorageSongs().map { it.toSongUi() }
                )
            }*/

            _state.update {
                //delay(1000)
                it.copy(
                    songs = songsRepository.getAllStorageSongs().map { it.toSongUi() }
                )
                /*.filter { it.path.endsWith(".mp3")}.map {
                    val start = System.currentTimeMillis()
                async(Dispatchers.Default){it.toSongUi(resolver,resources)}
                }.awaitAll()*//*
            }*/

            }
        }
    }


    private suspend fun <T> SavedStateHandle.update(key: String, function: suspend (T?) -> T?) {

        this[key] = function(this.get<T>(key))
    }

}

