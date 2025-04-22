package com.k.sekiro.musico.playmusic.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.k.sekiro.musico.playmusic.presenation.model.SongUi

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

fun SongUi.toSong(): Song = Song(
    name = name,
    title = title,
    artist = artist,
    cover = cover,
    dataUri= dataUri,
    album = album,
    path = path,
    addedDate = addedDate,
    duration = displayableDuration.durationMillis
)



val mockSongs = listOf(
    Song("LalalaLalalaLalalaLalalaLalalaLalalaLalalaLalala","","Me & you", ""),
    Song("Kingdom track 1","","Bandicom", ""),
    Song("Qadim","","Ajnad Nasheed", ""),
    Song("Funk Virso","","Irokz", "")
)