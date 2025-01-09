package com.k.sekiro.musico.playmusic.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Playlist(
    val name: String,
    val cover: Int = 0,
    val count: Int = 0,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    )
