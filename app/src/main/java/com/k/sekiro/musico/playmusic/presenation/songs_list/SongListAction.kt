package com.k.sekiro.musico.playmusic.presenation.songs_list

import com.k.sekiro.musico.playmusic.domain.model.Song

sealed interface SongListAction {
    data class OnSongClick(val song: Song): SongListAction
    data class OnMoreActionClick(val song : Song): SongListAction
    data class OnShareClick(val song : Song): SongListAction
}