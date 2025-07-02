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
import com.k.sekiro.musico.playmusic.data.local.AppDatabase
import com.k.sekiro.musico.playmusic.data.local.PaletteCache
import com.k.sekiro.musico.playmusic.data.repository.SongsRepositoryImpl
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.presenation.ViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module


@SuppressLint("UnsafeOptInUsageError")
val appModule = module{

    single { LruCache<String, Palette>(4*1024*1024)}//4MB

    singleOf(::PaletteCache)


    single{
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
            ).build()
    }

    single{ get<AppDatabase>().songsDao }


    singleOf(::SongsRepositoryImpl) bind SongsRepository::class

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

}


val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
