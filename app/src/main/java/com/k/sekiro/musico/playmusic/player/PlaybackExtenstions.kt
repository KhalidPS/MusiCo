package com.k.sekiro.musico.playmusic.player

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
import coil3.Image
import coil3.ImageLoader
import coil3.compose.ImagePainter
import coil3.request.ImageRequest
import com.k.sekiro.musico.R
import com.k.sekiro.musico.core.presentaion.util.getUriFromDrawable
import com.k.sekiro.musico.core.presentaion.util.isUriValid
import com.k.sekiro.musico.playmusic.player.notification.NotificationPlayerCustomCommand
import com.k.sekiro.musico.playmusic.player.setMediaItemsList
import com.k.sekiro.musico.playmusic.presenation.model.SongUi
import com.k.sekiro.musico.playmusic.presenation.model.toUri
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayType
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongViewModel
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
suspend fun MediaController?.setMediaItemsList(songs: List<SongUi>, context: Context) =
    supervisorScope {

        val dispatcher = Dispatchers.Default
        try {
            songs.map { song ->

                async(dispatcher){
                    val uri = song.cover.toUri()

                    /** check if image uri is valid if not then put placeholder from drawable **/
                    val cover =
                        if (isUriValid(context, uri)) {
                            uri
                        } else {
                            getUriFromDrawable(context, R.drawable.logo_2)
                        }



                    MediaItem.Builder()
                        .setUri(song.dataUri)
                        .setMediaId(song.path)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setArtworkUri(cover)
                                .setTitle(song.artist)
                                .setDisplayTitle(song.name)
                                .setAlbumTitle(song.album)
                                .setArtist(song.artist)
                                .build()
                        ).build()
                }

            }.also {
                this@setMediaItemsList!!.setMediaItems(it.awaitAll())
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
suspend fun MediaController?.setMediaItemsList(songs: List<SongUi>, startIndex: Int, startProgress: Long,context: Context) = supervisorScope {
    val dispatcher = Dispatchers.Default

    try {
            songs.map { song ->

                async(dispatcher){
                    val uri = song.cover.toUri()

                    /** check if image uri is valid if not then put placeholder from drawable **/
                    val cover =
                        if (isUriValid(context, uri)) {
                            uri
                        } else {
                            getUriFromDrawable(context, R.drawable.logo_2)
                        }



                    MediaItem.Builder()
                        .setUri(song.dataUri)
                        .setMediaId(song.path)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setArtworkUri(cover)
                                .setTitle(song.artist)
                                .setDisplayTitle(song.name)
                                .setAlbumTitle(song.album)
                                .setArtist(song.artist)
                                .build()
                        ).build()
                }

            }.also {
                this@setMediaItemsList!!.setMediaItems(it.awaitAll(),startIndex,startProgress)
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
