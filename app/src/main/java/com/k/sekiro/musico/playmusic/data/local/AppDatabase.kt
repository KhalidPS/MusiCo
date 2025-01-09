
package com.k.sekiro.musico.playmusic.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
import com.k.sekiro.musico.playmusic.domain.model.Playlist
import com.k.sekiro.musico.playmusic.domain.model.Song

@Database(entities = [Song::class, Playlist::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract val songsDao: SongsDao

    companion object{
        const val DATABASE_NAME = "music_database"
    }
}
