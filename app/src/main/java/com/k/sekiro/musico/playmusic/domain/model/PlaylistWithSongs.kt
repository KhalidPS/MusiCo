package com.k.sekiro.musico.playmusic.domain.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithSongs(
   @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "id", // id column for playlist (embedded) entity
        entityColumn = "id", // id column for song entity
        associateBy = Junction(
            PlaylistSong::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<Song>
)
