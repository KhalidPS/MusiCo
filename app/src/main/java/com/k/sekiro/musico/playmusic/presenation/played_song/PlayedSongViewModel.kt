package com.k.sekiro.musico.playmusic.presenation.played_song

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.presenation.model.fromMillis
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayedSongViewModel(
    private val songsRepository: SongsRepository,
    private val savedStateHandle: SavedStateHandle,
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




    fun updatePlayedSong(index: Int){
        _state.update {
            it.copy(
                playedSong = if (
                    it.songs.isNotEmpty()
                ){
                    it.songs[index]
                }else{
                    return
                }
            )
        }
    }

    fun getPlayedSong() = _state.value.playedSong

    fun getSongs() = _state.value.songs

    fun updateProgress(progress: Float){
        _state.update {
            it.copy(
                sliderProgress = progress
            )
        }
    }


    fun updateIsPlaying(isPlaying: Boolean){
        _state.update {
            it.copy(
                isPlaying = isPlaying
            )
        }
    }

    fun updatePlayType(type: PlayType){
        _state.update {
            it.copy(
                playType = type
            )
        }
    }

    fun updateDuration(duration: Long){
        _state.update {
            it.copy(

            )
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

               val songs =  songsRepository.getAllStorageSongs()
                   .filter { it.path.endsWith(".mp3") }.map {
                    it.toSongUi()
                }


                it.copy(
                    songs = songs
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







    internal fun calculateProgressValue(currentProgress: Long){


        _state.update {
            val progress =  if (currentProgress > 0 && it.playedSong!=null){
                ((currentProgress.toFloat() /it.playedSong.displayableDuration.durationMillis.toFloat()) * 100f)

            }else{
                0f
            }
            it.copy(
                sliderProgress =  progress,
                passedTimeDuration = fromMillis(currentProgress),
                currentPosition = currentProgress
            )
        }
    }





    @OptIn(UnstableApi::class)
    override fun onCleared() {

        super.onCleared()
    }



}

