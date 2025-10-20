package com.k.sekiro.musico.playmusic.data.local.room

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoomCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            // Use SQL inserts directly here, as DAOs are not available yet.
            db.execSQL("INSERT INTO Playlist (id, name) VALUES (1, 'Favorite')")
            db.execSQL("INSERT INTO Playlist (id, name) VALUES (2, 'Recent')")
            // Note: If you want to use auto-generated IDs, you'll need to handle it differently
            // but for default items, explicit IDs can be easier to manage.
        }
    }
}