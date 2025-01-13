package com.k.sekiro.musico.playmusic.presenation.played_song

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.FileDataSource
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.domain.model.Song
import com.k.sekiro.musico.playmusic.player.service.MusiCoServiceHandler
import com.k.sekiro.musico.playmusic.player.service.PlayerEvent
import com.k.sekiro.musico.playmusic.player.service.PlayerState
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.fromMillis
import com.k.sekiro.musico.playmusic.presenation.model.toSongUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayedSongViewModel(
    private val songsRepository: SongsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val musiCoServiceHandler: MusiCoServiceHandler
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



    init {
        viewModelScope.launch{
            musiCoServiceHandler.audioState.collectLatest { playerState ->
                when(playerState) {
                    is PlayerState.Buffering -> calculateProgressValue(playerState.progress)
                    is PlayerState.CurrentPlaying -> {
                            _state.update {
                                it.copy(
                                    playedSong = it.songs[playerState.mediaItemIndex]
                                )
                            }
                    }
                    PlayerState.Initial -> {}
                    is PlayerState.Playing -> playerState.isPlaying
                    is PlayerState.Progress -> calculateProgressValue(playerState.progress)
                    is PlayerState.Ready -> {

                    }
                }
            }
        }

    }

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
        viewModelScope.launch{
            when (action) {
                PlayedSongAction.ChangePlayType -> TODO()
                is PlayedSongAction.ChangeToOtherSong -> {
                    musiCoServiceHandler.onPlayerEvents(
                        PlayerEvent.SelectedAudioChange,
                        selectedAudioIndex = action.index
                    )
                }

                PlayedSongAction.OnDownArrowClicked -> TODO()
                PlayedSongAction.OnMoreActionClicked -> TODO()
                PlayedSongAction.PlayPause -> musiCoServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
                PlayedSongAction.SeekBackward -> TODO()
                PlayedSongAction.SeekForward -> TODO()
                is PlayedSongAction.SeekTo -> {
                   musiCoServiceHandler.onPlayerEvents(
                       PlayerEvent.SeekTo,
                       seekPosition = ((_state.value.playedSong!!.displayableDuration.durationMillis * action.position / 100f)).toLong()
                   )
                }

                PlayedSongAction.SeekToNext -> TODO()
                PlayedSongAction.SeekToPrevious -> TODO()
                is PlayedSongAction.UpdateProgress -> {
                    musiCoServiceHandler.onPlayerEvents(
                        PlayerEvent.UpdateProgress(action.newProgress)
                    )
                    _state.update {
                        it.copy(
                            sliderProgress = action.newProgress
                        )
                    }
                }
            }
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

               val songs =  songsRepository.getAllStorageSongs().map {
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

    @OptIn(UnstableApi::class)
    private fun addMediaItem(songs: List<SongUi>){
        try {
            songs.map {song ->
                MediaItem.Builder()
                    .setUri(song.dataUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setArtworkUri(song.cover)
                            .setTitle(song.title)
                            .setDisplayTitle(song.name)
                            .setAlbumTitle(song.album)
                            .setArtist(song.artist)
                            .build()
                    ).build()
            }.also { musiCoServiceHandler.setMediaItemList(it) }
        }catch (ex: DataSourceException){
            Log.e("ks","converting song to MediaItem problem :${ex}")
        }catch (ex: FileDataSource.FileDataSourceException){
            Log.e("ks","converting song to MediaItem problem :${ex}")
        }catch (ex: Exception){
            Log.e("ks","converting song to MediaItem problem :${ex}")

        }
    }

    private fun calculateProgressValue(currentProgress: Long){


        _state.update {
            val progress =  if (currentProgress > 0 && it.playedSong!=null){
                ((currentProgress.toFloat() /it.playedSong.displayableDuration.durationMillis.toFloat()) * 100f)

            }else{
                0f
            }
            it.copy(
                sliderProgress =  progress,
                passedTimeDuration = fromMillis(currentProgress)
            )
        }
    }


    override fun onCleared() {
        viewModelScope.launch{
            musiCoServiceHandler.onPlayerEvents(
                PlayerEvent.Stop
            )
        }
        super.onCleared()
    }

}

