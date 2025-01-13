package com.k.sekiro.musico.playmusic.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Song(
    val name: String,
    val title: String = "",
    val artist: String = "",
    val cover: String? = "",
    val dataUri: String = "",
    val album: String = "",
    val path: String = "",
    val addedDate: Long = 0L,
    val duration: Long = 0L,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)



val mockSongs = listOf(
    Song("Lalala","","Me & you", ""),
    Song("Kingdom track 1","","Bandicom", ""),
    Song("Qadim","","Ajnad Nasheed", ""),
    Song("Funk Virso","","Irokz", "")
)