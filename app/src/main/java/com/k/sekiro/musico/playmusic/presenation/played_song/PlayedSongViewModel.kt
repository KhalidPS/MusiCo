package com.k.sekiro.musico.playmusic.presenation.played_song

import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.domain.model.toSong
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.fromMillis
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
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
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000L
            ),
            PlayedSongState()
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


    fun updatePlayedSong(index: Int) {
        _state.update {
            it.copy(
                playedSong = if (
                    it.songs.isNotEmpty()
                ) {
                    it.songs[index]
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
            /** First step is show all songs either from room or from local storage**/
/*
            savedStateHandle[stateKey] = savedStateHandle.get<PlayedSongState>(stateKey)?.copy(
                songs = songsRepository.getAllStorageSongs().map { it.toSongUi() }
            )*/

            /*            savedStateHandle.update<PlayedSongState>(stateKey){
                it?.copy(
                    songs = songsRepository.getAllStorageSongs().map { it.toSongUi() }
                )
            }*/

            val roomSongs = songsRepository.getSongsFromRoom()
            val roomSongsMapped = roomSongs
                .filter { it.path.endsWith(".mp3") }.map {
            it.toSongUi()
        }

            val songsFromLocal = songsRepository.getAllStorageSongs()
            val songsFromLocalMapped = songsFromLocal
                .filter { it.path.endsWith(".mp3") }.map {
                    it.toSongUi()
                }

            val songs = if (roomSongs.isNotEmpty()) {
                Log.e("ks","room is not empty")
                roomSongsMapped

            } else {
                Log.e("ks","room is empty")
                songsRepository.addSongs(songsFromLocal)
                songsFromLocalMapped
            }

            _state.update {
                //delay(1000)

                it.copy(
                    songs = songs
                )

            }

            delay(500) /** Next step is update the songs list by compare the songs in local storage with ones in room
            to see if there are new songs added to storage by downloading them or share them from friends, etc..**/

            if (roomSongs.isNotEmpty()){

                /** Showing the songs directly from local storage until handle changes to room**/
                if (roomSongsMapped.toString() != songsFromLocalMapped.toString()){
                    _state.update {
                        it.copy(
                            songs = songsFromLocalMapped
                        )
                    }

                    val songsToBeAdded = async { songsFromLocal.filter { it !in roomSongs } }/** which songs are new in local storage
                     and not in room to be added**/

                    val songsToBeDeleted = async { roomSongs.filter { it !in songsFromLocal } }

                    songsRepository.deleteSongs(songsToBeDeleted.await())
                    songsRepository.addSongs(songsToBeAdded.await())
                }
            }

            /** At the end update the state with room songs cuz all other operations would be on room
             * songs not local ones (e.g playlists and their relationships with songs ids) so the dela would
             * with room at the end**/
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

