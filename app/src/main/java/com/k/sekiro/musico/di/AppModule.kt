package com.k.sekiro.musico.di

import androidx.collection.LruCache
import androidx.palette.graphics.Palette
import androidx.room.Room
import com.k.sekiro.musico.playmusic.data.local.AppDatabase
import com.k.sekiro.musico.playmusic.data.local.PaletteCache
import com.k.sekiro.musico.playmusic.data.repository.SongsRepositoryImpl
import com.k.sekiro.musico.playmusic.domain.SongsRepository
import com.k.sekiro.musico.playmusic.presenation.played_song.PlayedSongViewModel
import com.k.sekiro.musico.playmusic.presenation.request_permission_screen.RequestPermissionScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module


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
}