package com.k.sekiro.musico.playmusic.domain.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class SongWithPlaylists(
    @Embedded val song: Song,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            PlaylistSong::class,
            parentColumn = "songId",
            entityColumn = "playlistId"
        )
    )
    val playlists: List<Playlist>
)
