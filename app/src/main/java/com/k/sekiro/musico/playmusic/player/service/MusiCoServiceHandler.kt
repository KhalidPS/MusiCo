package com.k.sekiro.musico.playmusic.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@UnstableApi
class MusiCoServiceHandler(
    private val mediaController: ExoPlayer,
) : Player.Listener {
    private val _audioState: MutableStateFlow<PlayerState> =
        MutableStateFlow(PlayerState.Initial)
    val audioState: StateFlow<PlayerState> = _audioState.asStateFlow()

    private val scope =  CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var job: Job? = null

    init {
        mediaController.addListener(this)
    }


    fun addMediaItem(mediaItem: MediaItem) {
        mediaController.setMediaItem(mediaItem)
        mediaController.prepare()
    }

    fun setMediaItemList(mediaItems: List<MediaItem>) {
        mediaController.setMediaItems(mediaItems)
        mediaController.prepare()
    }



    suspend fun onPlayerEvents(
        playerEvent: PlayerEvent,
        selectedAudioIndex: Int = -1,
        seekPosition: Long = 0,
    ) {
        when (playerEvent) {
            PlayerEvent.Backward -> mediaController.seekBack()
            PlayerEvent.Forward -> mediaController.seekForward()
            PlayerEvent.SeekToNext -> mediaController.seekToNext()
            PlayerEvent.SeekToPrevious -> mediaController.seekToPrevious()
            PlayerEvent.PlayPause -> playOrPause()
            PlayerEvent.SeekTo -> mediaController.seekTo(seekPosition)
            PlayerEvent.SelectedAudioChange -> {
                when (selectedAudioIndex) {
                    mediaController.currentMediaItemIndex -> {
                        playOrPause()
                    }

                    else -> {
                        mediaController.seekToDefaultPosition(selectedAudioIndex)
                        _audioState.value = PlayerState.Playing(
                            isPlaying = true
                        )
                        mediaController.playWhenReady = true
                        startProgressUpdate()
                    }
                }
            }

            PlayerEvent.Stop -> stopProgressUpdate()
            is PlayerEvent.UpdateProgress -> {
                mediaController.seekTo(
                    (mediaController.duration * playerEvent.newProgress).toLong()
                )
            }
        }
    }


    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value =
                PlayerState.Buffering(mediaController.currentPosition)

            ExoPlayer.STATE_READY -> _audioState.value =
                PlayerState.Ready(mediaController.duration)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _audioState.value = PlayerState.Playing(isPlaying = isPlaying)
        _audioState.value = PlayerState.CurrentPlaying(mediaController.currentMediaItemIndex)
        if (isPlaying) {
            scope.launch {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)

    }

    private suspend fun playOrPause() {
        if (mediaController.isPlaying) {
            mediaController.pause()
            stopProgressUpdate()
        } else {
            mediaController.play()
            _audioState.value = PlayerState.Playing(
                isPlaying = true
            )
            startProgressUpdate()
        }
    }

    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(500)
            _audioState.value = PlayerState.Progress(mediaController.currentPosition)
        }
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _audioState.value = PlayerState.Playing(isPlaying = false)
    }


    fun cancelServiceScope(){
        scope.cancel()
    }
}

sealed interface PlayerEvent {
    object PlayPause : PlayerEvent
    object SelectedAudioChange : PlayerEvent
    object Backward : PlayerEvent
    object SeekToNext : PlayerEvent
    object SeekToPrevious: PlayerEvent
    object Forward : PlayerEvent
    object SeekTo : PlayerEvent
    object Stop : PlayerEvent
    data class UpdateProgress(val newProgress: Float) : PlayerEvent
}

sealed interface PlayerState {
    object Initial : PlayerState
    data class Ready(val duration: Long) : PlayerState
    data class Progress(val progress: Long) : PlayerState
    data class Buffering(val progress: Long) : PlayerState
    data class Playing(val isPlaying: Boolean) : PlayerState
    data class CurrentPlaying(val mediaItemIndex: Int) : PlayerState
}






