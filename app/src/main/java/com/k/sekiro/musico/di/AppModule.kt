package com.k.sekiro.musico.di

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.collection.LruCache
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.palette.graphics.Palette
import androidx.room.Room
import com.k.sekiro.musico.playmusic.data.local.AppDatabase
import com.k.sekiro.musico.playmusic.data.local.PaletteCache
import com.k.sekiro.musico.playmusic.data.repository.SongsRepositoryImpl
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.player.notification.MusiCoNotificationManager
import com.k.sekiro.musico.playmusic.player.service.MusiCoServiceHandler
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongViewModel
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.RequestPermissionScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module


@SuppressLint("UnsafeOptInUsageError")
val appModule = module{

    single { LruCache<String, Palette>(4*1024*1024)}//4MB

    singleOf(::PaletteCache)

    viewModelOf(::RequestPermissionScreenViewModel)

    single{
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
            ).build()
    }

    single{ get<AppDatabase>().songsDao }


    singleOf(::SongsRepositoryImpl) bind SongsRepository::class

    viewModelOf(::PlayedSongViewModel)

    single{ androidContext().contentResolver }
    single{ androidContext().resources }

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
        MediaSession.Builder(androidContext(), get()).build()
    }

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


    single{ MusiCoNotificationManager(androidContext(),get()) }

    single { MusiCoServiceHandler(get()) }


}