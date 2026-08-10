package com.k.sekiro.musico.playmusic.presenation.browse

import com.k.sekiro.musico.playmusic.presenation.browse.model.BrowseGroupUi
import com.k.sekiro.musico.playmusic.presenation.model.SongUi

fun List<SongUi>.groupByArtist(): List<BrowseGroupUi> =
    groupBy { it.artist.trim() }
        .map { (key, songs) -> BrowseGroupUi(key, key.ifBlank { "Unknown Artist" }, songs) }
        .sortedBy { it.title.lowercase() }

fun List<SongUi>.groupByAlbum(): List<BrowseGroupUi> =
    groupBy { it.album.trim() }
        .map { (key, songs) -> BrowseGroupUi(key, key.ifBlank { "Unknown Album" }, songs) }
        .sortedBy { it.title.lowercase() }
