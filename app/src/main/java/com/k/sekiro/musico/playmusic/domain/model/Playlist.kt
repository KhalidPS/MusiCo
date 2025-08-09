package com.k.sekiro.musico.playmusic.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Playlist(
    val name: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    )
