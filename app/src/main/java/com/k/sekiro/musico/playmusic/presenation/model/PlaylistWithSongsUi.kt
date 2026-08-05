package com.k.sekiro.musico.playmusic.presenation.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistWithSongs

@Immutable
@Stable
data class PlaylistWithSongsUi(
    val playlist: Playlist,
    val songs: List<SongUi>
)

fun PlaylistWithSongs.toPlaylistWithSongsUi(): PlaylistWithSongsUi{
    return PlaylistWithSongsUi(
        playlist = playlist,
        songs = songs.map { it.toSongUi() }
    )
}