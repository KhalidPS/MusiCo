package com.k.sekiro.musico.di

import android.annotation.SuppressLint
import android.content.Context
import androidx.collection.LruCache
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.palette.graphics.Palette
import androidx.room.Room
import androidx.room.RoomDatabase
import com.k.sekiro.musico.playmusic.data.local.room.AppDatabase
import com.k.sekiro.musico.playmusic.data.local.PaletteCache
import com.k.sekiro.musico.playmusic.data.local.preferences.PreferencesDataStoreSaver
import com.k.sekiro.musico.playmusic.data.local.room.RoomCallback
import com.k.sekiro.musico.playmusic.data.repository.PlaylistRepositoryImpl
import com.k.sekiro.musico.playmusic.data.repository.PlaylistSongRepositoryImpl
import com.k.sekiro.musico.playmusic.data.repository.SongsRepositoryImpl
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistRepository
import com.k.sekiro.musico.playmusic.domain.repositroy.PlaylistSongRepository
import com.k.sekiro.musico.playmusic.domain.repositroy.SongsRepository
import com.k.sekiro.musico.playmusic.presenation.ViewModel
import com.k.sekiro.musico.playmusic.presenation.player.MediaControllerManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.math.sin


@SuppressLint("UnsafeOptInUsageError")
val appModule = module{

    single { LruCache<String, Palette>(4*1024*1024)}//4MB

    singleOf(::PaletteCache)

    
    singleOf(::RoomCallback) bind RoomDatabase.Callback::class

    single{
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
            )
            .addCallback(get())
            .build()
    }

    single{ get<AppDatabase>().songsDao }
    single { get<AppDatabase>().playlistDao }
    single { get<AppDatabase>().playlistSongDao }

    singleOf(::SongsRepositoryImpl) bind SongsRepository::class
    singleOf(::PlaylistRepositoryImpl) bind PlaylistRepository::class
    singleOf(::PlaylistSongRepositoryImpl) bind PlaylistSongRepository::class
    singleOf(::PreferencesDataStoreSaver) bind SimpleDataSaver::class

    viewModelOf(::ViewModel)

    single{ androidContext().contentResolver }
    single{ androidContext().resources }

/*
    single{
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    single{
        ExoPlayer.Builder(
            androidContext(),
        )
            .setAudioAttributes(get(), true)
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(DefaultTrackSelector(androidContext()))
            .build()
    }


    single{

        val pendingIntent = PendingIntent.getActivity(
            androidContext(),
            MusicoApp.NOTIFICATION_ID,
            Intent(androidContext(), MainActivity::class.java).apply {
                action = Intent.ACTION_RUN
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationPlayerCustomCommandButtons = NotificationPlayerCustomCommand.values().map { it.commandButton }


        MediaSession.Builder(androidContext(), get<ExoPlayer>())
            .setSessionActivity(pendingIntent)
            .setCallback(get<MediaSession.Callback>())
            .setCustomLayout(notificationPlayerCustomCommandButtons)
            .build()
    }
*/

    single{

        /**this way if u want use the primary NotificationManager class directly**/
  /*      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            androidContext().getSystemService(NotificationManager::class.java)
        }else{
            androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }*/

        /**this way if u want to use support class which you don't need to check**/
        NotificationManagerCompat.from(androidContext())
    }


/*
    single{ MusiCoNotificationManager(androidContext(),get<ExoPlayer>()) }
*/


    single { androidContext().getSharedPreferences("settings", Context.MODE_PRIVATE) }

    //single { MusiCoServiceHandler(get()) }

    single { androidContext().dataStore}

    single { MediaControllerManager(androidContext(),get()) }

}


val Context.dataStore: DataStore<Preferences> by preferencesDataStore("setting_v2")
