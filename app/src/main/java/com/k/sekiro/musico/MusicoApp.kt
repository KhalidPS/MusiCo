package com.k.sekiro.musico

import android.app.Application
import com.k.sekiro.musico.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

class MusicoApp : Application(){
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MusicoApp)
            modules(appModule)
        }
    }
}