package com.k.sekiro.musico.playmusic.player

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.FileDataSource
import androidx.media3.session.MediaController
import com.k.sekiro.musico.playmusic.player.notification.NotificationPlayerCustomCommand
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toUri
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayType
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


private var progressJob: Job? = null
private var scope: CoroutineScope? = null

fun MediaController?.addMediaItem(mediaItem: MediaItem) {
    this!!.setMediaItem(mediaItem)
    this!!.prepare()
}

suspend fun MediaController?.playOrPause(viewModel: PlayedSongViewModel) {
    if (this!!.isPlaying) {
        this.pause()
        stopProgressUpdate(viewModel)
    } else {
        this.play()
        viewModel.updateIsPlaying(true)
        startProgressUpdate(viewModel)
    }
}

suspend fun MediaController?.startProgressUpdate(viewModel: PlayedSongViewModel) {
    scope?.cancel()
    coroutineScope {
        scope = this
        launch {
            while (isActive) {
                delay(500)
                viewModel.calculateProgressValue(this@startProgressUpdate!!.currentPosition)
                Log.e("ks","progressing..........................")
            }
        }
    }
}


@OptIn(UnstableApi::class)
fun MediaController?.setMediaItemsList(songs: List<SongUi>) {
    try {
        songs.map { song ->
            MediaItem.Builder()
                .setUri(song.dataUri)
                .setMediaId("${song.name}_${song.dataUri}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtworkUri(song.cover.toUri())
                        .setTitle(song.artist)
                        .setDisplayTitle(song.name)
                        .setAlbumTitle(song.album)
                        .setArtist(song.artist)
                        .build()
                ).build()
        }.also {
            this!!.setMediaItems(it)
            this.prepare()
        }
    } catch (ex: DataSourceException) {
        Log.e("ks", "converting song to MediaItem problem :${ex}")
    } catch (ex: FileDataSource.FileDataSourceException) {
        Log.e("ks", "converting song to MediaItem problem :${ex}")
    } catch (ex: Exception) {
        Log.e("ks", "converting song to MediaItem problem :${ex}")

    }
}

fun stopProgressUpdate(viewModel: PlayedSongViewModel) {
    viewModel.updateIsPlaying(false)
    scope?.cancel()
}


fun MediaController?.onChangPlayType(type: PlayType, viewModel: PlayedSongViewModel) {
    when (type) {
        PlayType.RepeatAll -> {
            this!!.shuffleModeEnabled = false
            this!!.repeatMode = Player.REPEAT_MODE_ALL
            this.sendCustomCommand(NotificationPlayerCustomCommand.SHUFFLE.commandButton.sessionCommand!!,
                Bundle.EMPTY)
        }

        PlayType.RepeatOne -> {
            this!!.shuffleModeEnabled = false
            this!!.repeatMode = Player.REPEAT_MODE_ONE
            this.sendCustomCommand(NotificationPlayerCustomCommand.REPEAT_ALL.commandButton.sessionCommand!!,
                Bundle.EMPTY)

        }

        PlayType.Shuffle -> {
            this!!.repeatMode = Player.REPEAT_MODE_OFF
            this!!.shuffleModeEnabled = true
            this!!.sendCustomCommand(NotificationPlayerCustomCommand.REPEAT_ONE.commandButton.sessionCommand!!,
                Bundle.EMPTY)
        }
    }

    viewModel.updatePlayType(type)

}


fun MediaController?.getSongPlayedInBackground(): Pair<Int, Long>? {
    return if (this != null && this.isPlaying) {
        Pair(this.currentMediaItemIndex, this.currentPosition)
    } else {
        null
    }
}
