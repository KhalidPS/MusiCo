package com.k.sekiro.musico.playmusic.presenation.model

import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistWithSongs


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