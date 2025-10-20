package com.k.sekiro.musico.playmusic.presenation.player.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

@UnstableApi
class CustomMediaNotification(context: Context): DefaultMediaNotificationProvider(context) {
    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        return super.getMediaButtons(
            session,
            playerCommands,
            mediaButtonPreferences,
            showPauseButton
        )
    }

    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory
    ): IntArray {


        return super.addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)
    }

    override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence? {
        return super.getNotificationContentTitle(metadata)
    }

    override fun getNotificationContentText(metadata: MediaMetadata): CharSequence? {
        return super.getNotificationContentText(metadata)
    }

}