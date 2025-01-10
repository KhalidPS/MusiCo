package com.k.sekiro.musico

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.k.sekiro.musico.di.appModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

class MusicoApp : Application(){
    val notificationManager: NotificationManagerCompat by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MusicoApp)
            modules(appModule)
        }

        createNotificationChannel(notificationManager)
    }


    companion object{
        const val NOTIFICATION_CHANNEL_ID = "song_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Played Song Channel"
        const val NOTIFICATION_ID = 123
    }


    private fun createNotificationChannel(notificationManager: NotificationManagerCompat){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannel(channel)

        }
    }
}