package com.k.sekiro.musico.playmusic.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey


@Entity(
    primaryKeys = ["playlistId","songId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
            ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        ),

    ]
)
data class PlaylistSong(
    val playlistId: Int,
    val songId:Int
)
