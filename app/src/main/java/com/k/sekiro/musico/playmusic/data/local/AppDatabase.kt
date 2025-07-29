
package com.k.sekiro.musico.playmusic.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.PlaylistSong
import com.k.sekiro.musico.playmusic.domain.model.Song

@Database(entities = [Song::class, Playlist::class, PlaylistSong::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract val songsDao: SongsDao

    abstract val playlistDao: PlaylistDao

    abstract val playlistSongDao: PlaylistSongDao

    companion object{
        const val DATABASE_NAME = "music_database"
    }
}
