package com.k.sekiro.musico.playmusic.presenation.browse.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.k.sekiro.musico.playmusic.presenation.model.SongUi

@Immutable
@Stable
data class BrowseGroupUi(
    val key: String,
    val title: String,
    val songs: List<SongUi>
)
