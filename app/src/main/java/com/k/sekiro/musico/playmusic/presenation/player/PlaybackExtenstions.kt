package com.k.sekiro.musico.playmusic.presenation.player

import android.content.Context
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
import com.k.sekiro.musico.R
import com.k.sekiro.musico.playmusic.domain.getUriFromDrawable
import com.k.sekiro.musico.playmusic.domain.isValidUri
import com.k.sekiro.musico.playmusic.presenation.player.notification.NotificationPlayerCustomCommand
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import androidx.core.net.toUri
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver
import com.k.sekiro.musico.playmusic.domain.model.PROGRESS_KEY
import com.k.sekiro.musico.playmusic.presenation.PlayType
import com.k.sekiro.musico.playmusic.presenation.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext


private var progressJob: Job? = null
private var scope: CoroutineScope? = null

fun MediaController?.addMediaItem(mediaItem: MediaItem) {
    this!!.setMediaItem(mediaItem)
    this!!.prepare()
}

suspend fun MediaController?.playOrPause(
    calculateProgress: (Long) -> Unit,
    updateIsPlaying: (Boolean) -> Unit
) {
    if (this!!.isPlaying) {
        this.pause()
        stopProgressUpdate(updateIsPlaying)
    } else {
        this.play()
        updateIsPlaying(true)
        startProgressUpdate(calculateProgress)
    }
}

suspend fun MediaController?.startProgressUpdate(calculateProgress: (Long) -> Unit) {
    scope?.cancel()
    coroutineScope {
        scope = this
        launch {
            while (isActive) {
                delay(500)
                calculateProgress(this@startProgressUpdate!!.currentPosition)
                Log.e("ks", "progressing..........................")
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
                        .setMediaId(song.path)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setArtworkUri(song.cover.toUri())
                                .setTitle(song.artist)
                                .setDisplayTitle(song.title)
                                .setAlbumTitle(song.album)
                                .setArtist(song.artist)
                                .setDiscNumber(song.id.toInt())
                                .build()
                        ).build()

            }.also {
                this@setMediaItemsList!!.setMediaItems(it)
                this@setMediaItemsList.prepare()
            }
        } catch (ex: DataSourceException) {
            Log.e("ks", "converting song to MediaItem problem :${ex}")
        } catch (ex: FileDataSource.FileDataSourceException) {
            Log.e("ks", "converting song to MediaItem problem :${ex}")
        } catch (ex: Exception) {
            Log.e("ks", "converting song to MediaItem problem :${ex}")

        }
    }


@OptIn(UnstableApi::class)
fun MediaController?.setMediaItemsList(songs: List<SongUi>, startIndex: Int, startProgress: Long) {
    try {
        songs.map { song ->
            MediaItem.Builder()
                .setUri(song.dataUri)
                .setMediaId(song.path)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtworkUri(song.cover.toUri())
                        .setTitle(song.artist)
                        .setDisplayTitle(song.name)
                        .setAlbumTitle(song.album)
                        .setArtist(song.artist)
                        .setDiscNumber(song.id.toInt())
                        .build()
                ).build()
        }.also {
            this!!.setMediaItems(it, startIndex, startProgress)
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


fun stopProgressUpdate(updateIsPlaying: (Boolean) -> Unit) {
    updateIsPlaying(false)
    scope?.cancel()
}


fun MediaController?.onChangPlayType(type: PlayType, updatePlayType: (PlayType) -> Unit) {
    when (type) {
        PlayType.RepeatAll -> {
            this!!.shuffleModeEnabled = false
            this!!.repeatMode = Player.REPEAT_MODE_ALL
            this.sendCustomCommand(
                NotificationPlayerCustomCommand.SHUFFLE.commandButton.sessionCommand!!,
                Bundle.EMPTY
            )
        }

        PlayType.RepeatOne -> {
            this!!.shuffleModeEnabled = false
            this!!.repeatMode = Player.REPEAT_MODE_ONE
            this.sendCustomCommand(
                NotificationPlayerCustomCommand.REPEAT_ALL.commandButton.sessionCommand!!,
                Bundle.EMPTY
            )

        }

        PlayType.Shuffle -> {
            this!!.repeatMode = Player.REPEAT_MODE_OFF
            this!!.shuffleModeEnabled = true
            this!!.sendCustomCommand(
                NotificationPlayerCustomCommand.REPEAT_ONE.commandButton.sessionCommand!!,
                Bundle.EMPTY
            )
        }
    }

    updatePlayType(type)

}


fun MediaController?.getSongPlayedInBackground(): Pair<Int, Long>? {
    return if (this != null && this.isPlaying) {
        Pair(this.currentMediaItemIndex, this.currentPosition)
    } else {
        null
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////

suspend fun MediaController?.setupRecentPlayedSongWhenPlayerRunning(
    songs: List<SongUi>,
    viewModel: ViewModel
) {
    viewModel.updateIsPlaying(true)

    val currentPath = this!!.currentMediaItem!!.mediaId
    //val currentPlayedSong = songs.find { it.path == currentPath }
    //val index = if (currentPlayedSong != null) songs.indexOf(currentPlayedSong) else this!!.currentMediaItemIndex
    val index = songs.indexOfFirst { it.path == currentPath }.takeIf { it != -1 }
        ?: this!!.currentMediaItemIndex
    viewModel.updatePlayedSong(index)

    /** May need to add the whole songs list to controller **/

    Log.e(
        "ks",
        "Played song not null: ${viewModel.getPlayedSong()}"
    )

    syncUiPlayModeWithController(viewModel)


    this!!.startProgressUpdate(viewModel::calculateProgressValue)
}


fun MediaController.syncUiPlayModeWithController(viewModel: ViewModel){
    if (shuffleModeEnabled){
        viewModel.updatePlayType(PlayType.Shuffle)
    }else if (!shuffleModeEnabled && repeatMode == Player.REPEAT_MODE_ALL){
        viewModel.updatePlayType(PlayType.RepeatAll)
    }else {
        viewModel.updatePlayType(PlayType.RepeatOne)

    }
}

suspend fun MediaController?.setupRecentPlayedSongWhenServiceNotActive(
    dataSaver: SimpleDataSaver,
    songs: List<SongUi>,
    viewModel: ViewModel,
    path: String,
) {
    val progress = dataSaver.suspendGet(PROGRESS_KEY, 0L)
    var index = 0

    /** Previously I was get the saved index from pref and then get the last played song
    but this would not be good solution in some scenarios (e.g if pause the player
    and close the app this will save the index then u downloaded new song then u open app
    again the problem is that the played song would be now not the song u expected to be which is the
    last one played before u close the app, instead it would be the previous song for the song u expected to be
    due to adding new songs to storage)**/


    Log.e("ks", "path: $path")

    withContext(Dispatchers.IO) {
        for (i in 0 until songs.size) {
            Log.e(
                "ks",
                "inside for size of List: ${songs.size}"
            )
            Log.e(
                "ks",
                "path inside for loop :${songs[i].path}"
            )
            if (songs[i].path == path) {
                Log.e("ks", "path inside if :${songs[i].path}")
                index = i
                viewModel.updatePlayedSong(i)
                Log.e("ks", "catch the index :$i")
                Log.e("ks", "catch the index 2 :$index")
                break
            }
        }
    }

    this!!.setMediaItemsList(
        songs,
        index,
        progress,
    )
    //controller!!.startProgressUpdate(viewModel)
    viewModel.calculateProgressValue(progress)
}
