package com.k.sekiro.musico.playmusic.presenation

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.fromMillis
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.iterator

class ViewModel(
    private val songsRepository: SongsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {


    /* @OptIn(SavedStateHandleSaveableApi::class)
    var duration by savedStateHandle.saveable{ mutableLongStateOf(0L) }
    var progress by savedStateHandle.saveable{ mutableFloatStateOf(0f) }
    var durationString by savedStateHandle.saveable{ mutableStateOf("00:00") }*/

    private val stateKey = "uiState"


    private val _state = MutableStateFlow<UiState>(UiState())
    val state = _state
        .onStart {
            getAllSongsFromLocal()
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000L
            ),
            UiState()
        )


    /*    private val _state = savedStateHandle.getStateFlow(stateKey, UiState())
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


    fun updatePlayedSong(index: Int) {
        val validateIndex = if (index < 0) 0 else index
        _state.update {
            it.copy(
                playedSong = if (
                    it.songs.isNotEmpty()
                ) {
                    it.songs[validateIndex]
                } else {
                    return
                }
            )
        }
    }

    fun getPlayedSong() = _state.value.playedSong

    fun getSongs() = _state.value.songs

    fun updateProgress(progress: Float) {
        _state.update {
            it.copy(
                sliderProgress = progress
            )
        }
    }


    fun updateIsPlaying(isPlaying: Boolean) {
        _state.update {
            it.copy(
                isPlaying = isPlaying
            )
        }
    }

    fun updatePlayType(type: PlayType) {
        _state.update {
            it.copy(
                playType = type
            )
        }
    }

    fun updateDuration(duration: Long) {
        _state.update {
            it.copy(

            )
        }
    }

    fun updateSongs(songs: List<SongUi>) {
        _state.update {
            it.copy(
                songs = songs
            )
        }
    }


    private fun getAllSongsFromLocal() {
        viewModelScope.launch(Dispatchers.IO) {


            val roomSongsIdentifier = songsRepository.getSongsFromRoom().associate { it.path to it }
            val songsFromLocal = songsRepository.getAllStorageSongs()
            val songsFromLocalIdentifier = songsFromLocal.associate { it.path to it }


            if (roomSongsIdentifier.isNotEmpty()){

                    for (song in songsFromLocal){
                        val songByPath = roomSongsIdentifier[song.path]

                        if (songByPath == null){
                            songsRepository.addSong(song)
                        }
                    }


                    for ((path,songByPath) in roomSongsIdentifier){
                        if (!songsFromLocalIdentifier.containsKey(path)){
                            songsRepository.deleteSong(songByPath)
                        }
                    }


/*                    val songsToBeAdded = async { songsFromLocal.filter { it !in roomSongs } }*//** which songs are new in local storage
                     and not in room to be added **//*

                    val songsToBeDeleted = async { roomSongs.filter { it !in songsFromLocal } }

                    songsRepository.deleteSongs(songsToBeDeleted.await())
                    songsRepository.addSongs(songsToBeAdded.await())*/
                }else{
                    songsRepository.addSongs(songsFromLocal)
                 }


            /** At the end update the state with room songs cuz all other operations would be on room
             * songs not local ones (e.g playlists and their relationships with songs ids) so the dela would
             * **/
                _state.update {
                    it.copy(
                        songs = songsRepository.getSongsFromRoom().map { it.toSongUi() }
                    )
                }
            }
        }



    private suspend fun <T> SavedStateHandle.update(key: String, function: suspend (T?) -> T?) {

        this[key] = function(this.get<T>(key))
    }




    internal fun calculateProgressValue(currentProgress: Long) {


        _state.update {
            val progress = if (currentProgress > 0 && it.playedSong != null) {
                ((currentProgress.toFloat() / it.playedSong.displayableDuration.durationMillis.toFloat()) * 100f)

            } else {
                0f
            }
            it.copy(
                sliderProgress = progress,
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

