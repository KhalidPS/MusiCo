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
    val lastModified: Long = 0L,
    @PrimaryKey(autoGenerate = false) val id: Long = 0,
){
    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (javaClass != other?.javaClass) false
        else this.path == (other as SongUi).path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

fun SongUi.toSong(): Song = Song(
    name = name,
    title = title,
    artist = artist,
    cover = cover,
    dataUri= dataUri,
    album = album,
    path = path,
    addedDate = addedDate,
    lastModified = lastModified,
    duration = displayableDuration.durationMillis,
    id = id
)



val mockSongs = listOf(
    Song("LalalaLalalaLalalaLalalaLalalaLalalaLalalaLalala","LalalaLalalaLalalaLalalaLalalaLalalaLalalaLalala","Me & you", ""),
    Song("Kingdom track 1","Kingdom track 1","Bandicom", ""),
    Song("Qadim","","Ajnad Nasheed", ""),
    Song("Funk Virso","","Irokz", "")
)