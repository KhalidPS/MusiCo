package com.k.sekiro.musico.playmusic.player.service

import android.content.SharedPreferences
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MusiCoServiceHandler(private val mediaControllerShared: SharedFlow<MediaController>) :
    Player.Listener , KoinComponent{
    private val _audioState: MutableStateFlow<PlayerState> =
        MutableStateFlow(PlayerState.Initial)
    val audioState: StateFlow<PlayerState> = _audioState.asStateFlow()

    val sharedPreferences: SharedPreferences by inject()

    private var mediaController: MediaController? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var job: Job? = null


    init {
        scope.launch(Dispatchers.Default) {
            mediaControllerShared.collectLatest {
                mediaController = it
                if (mediaController != null) {
                    mediaController!!.addListener(this@MusiCoServiceHandler)

                }
            }
        }
    }


    fun addMediaItem(mediaItem: MediaItem) {
        mediaController!!.setMediaItem(mediaItem)
        mediaController!!.prepare()
    }

    fun setMediaItemList(mediaItems: List<MediaItem>) {
        mediaController!!.setMediaItems(mediaItems)
        mediaController!!.prepare()
    }


    suspend fun onPlayerEvents(
        playerEvent: PlayerEvent,
        selectedAudioIndex: Int = -1,
        seekPosition: Long = 0,
    ) {
        when (playerEvent) {
            PlayerEvent.Backward -> mediaController!!.seekBack()
            PlayerEvent.Forward -> mediaController!!.seekForward()
            PlayerEvent.SeekToNext -> mediaController!!.seekToNext()
            PlayerEvent.SeekToPrevious -> mediaController!!.seekToPrevious()
            PlayerEvent.PlayPause -> playOrPause()
            PlayerEvent.SeekTo -> mediaController!!.seekTo(seekPosition)
            PlayerEvent.SelectedAudioChange -> {
                when (selectedAudioIndex) {
                    mediaController!!.currentMediaItemIndex -> {
                        playOrPause()
                    }

                    else -> {
                        mediaController!!.seekToDefaultPosition(selectedAudioIndex)
                        _audioState.value = PlayerState.Playing(
                            isPlaying = true
                        )
                        mediaController!!.playWhenReady = true
                        startProgressUpdate()
                    }
                }
            }

            PlayerEvent.Stop -> stopProgressUpdate()
            is PlayerEvent.UpdateProgress -> {
                mediaController!!.seekTo(
                    (mediaController!!.duration * playerEvent.newProgress).toLong()
                )
            }

            is PlayerEvent.ChangePlayType -> {
                onChangPlayType(playerEvent.type)
            }

            is PlayerEvent.ClickNotification -> {
                val song = getSongPlayedInBackground()

                if (song != null){
                    _audioState.update {
                        PlayerState.CurrentPlaying(song.first)
                    }

                    mediaController!!.seekTo(song.second)

                }

            }
        }
    }


    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value =
                PlayerState.Buffering(mediaController!!.currentPosition)

            ExoPlayer.STATE_READY -> _audioState.value =
                PlayerState.Ready(mediaController!!.duration)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _audioState.value = PlayerState.Playing(isPlaying = isPlaying)
        _audioState.value = PlayerState.CurrentPlaying(mediaController!!.currentMediaItemIndex)

        val currentSong = mediaController!!.currentMediaItemIndex
        val currentProgress = mediaController!!.currentPosition

        Log.e("ks","playing changed")
        
        if (isPlaying) {
            scope.launch {
                startProgressUpdate()
                sharedPreferences.edit().apply{
                    putInt("currentIndex",currentSong)
                    putLong("currentProgress",currentProgress)
                    putBoolean("isResumeable",true)
                    apply()
                }

            }
        } else {
            stopProgressUpdate()
            sharedPreferences.edit().apply{
                putInt("currentIndex",currentSong)
                putLong("currentProgress",currentProgress)
                putBoolean("isResumeable",false)
                apply()
            }
        }
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)

        _audioState.update {
            PlayerState.CurrentPlaying(mediaController!!.currentMediaItemIndex)
        }

    }


    private suspend fun playOrPause() {
        if (mediaController!!.isPlaying) {
            mediaController!!.pause()
            stopProgressUpdate()
        } else {
            mediaController!!.play()
            _audioState.value = PlayerState.Playing(
                isPlaying = true
            )
            startProgressUpdate()
        }
    }

    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(500)
            _audioState.value = PlayerState.Progress(mediaController!!.currentPosition)
        }
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _audioState.value = PlayerState.Playing(isPlaying = false)
    }


    fun cancelServiceScope() {
        scope.cancel()
        mediaController?.removeListener(this)
    }

    /*    fun initController(mediaController: MediaController){
            this.mediaController = mediaController
        }*/

  private  fun onChangPlayType(type: PlayType) {
        when (type) {
            PlayType.RepeatAll -> {
                mediaController!!.shuffleModeEnabled = false
                mediaController!!.repeatMode = Player.REPEAT_MODE_ALL
            }

            PlayType.RepeatOne -> {
                mediaController!!.shuffleModeEnabled = false
                mediaController!!.repeatMode = Player.REPEAT_MODE_ONE
            }

            PlayType.Shuffle -> {
                mediaController!!.repeatMode = Player.REPEAT_MODE_OFF
                mediaController!!.shuffleModeEnabled = true
            }
        }

        _audioState.update { PlayerState.PlayingType(type) }

    }


  private  fun getSongPlayedInBackground(): Pair<Int, Long>? {
        return if (mediaController != null && mediaController!!.isPlaying) {
            Pair(mediaController!!.currentMediaItemIndex, mediaController!!.currentPosition)
        } else {
            null
        }
    }

}


sealed interface PlayerEvent {
    object PlayPause : PlayerEvent
    object SelectedAudioChange : PlayerEvent
    object Backward : PlayerEvent
    object SeekToNext : PlayerEvent
    object SeekToPrevious : PlayerEvent
    object Forward : PlayerEvent
    object SeekTo : PlayerEvent
    object Stop : PlayerEvent
    data class UpdateProgress(val newProgress: Float) : PlayerEvent
    data class ChangePlayType(val type: PlayType) : PlayerEvent
    object ClickNotification : PlayerEvent
}

sealed interface PlayerState {
    object Initial : PlayerState
    data class Ready(val duration: Long) : PlayerState
    data class Progress(val progress: Long) : PlayerState
    data class Buffering(val progress: Long) : PlayerState
    data class Playing(val isPlaying: Boolean) : PlayerState
    data class CurrentPlaying(val mediaItemIndex: Int) : PlayerState
    data class PlayingType(val type: PlayType) : PlayerState
}






