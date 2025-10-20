package com.k.sekiro.musico.playmusic.presenation.player.notification

import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.k.sekiro.musico.R

enum class NotificationPlayerCustomCommand(
    val customAction: String,
    val commandButton: CommandButton
) {
    /*    REWIND(
            customAction = CUSTOM_COMMAND_REWIND_ACTION,
            commandButton = CommandButton.Builder()
                .setDisplayName("Rewind")
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REWIND_ACTION, Bundle()))
                .setIconResId(R.drawable.rewind_5)
                .build()
        ),

        FORWARD(
            customAction = CUSTOM_COMMAND_FORWARD_ACTION,
            commandButton = CommandButton.Builder()
                .setDisplayName("Forward")
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_FORWARD_ACTION, Bundle()))
                .setIconResId(R.drawable.forward_5)
                .build()
        ),*/

    FAVORITE(
        customAction = CUSTOM_COMMAND_ADD_FAVORITE_ACTION,
        commandButton = CommandButton.Builder()
            .setDisplayName("Add Favorite")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_ADD_FAVORITE_ACTION, Bundle()))
            .setIconResId(R.drawable.favorite)
            .build()
    ),

    REPEAT_ALL(
        customAction = CUSTOM_COMMAND_REPEAT_ALL_ACTION,
        commandButton = CommandButton.Builder()
            .setDisplayName("Repeat All")
            .setSessionCommand(
                SessionCommand(
                    CUSTOM_COMMAND_REPEAT_ALL_ACTION,
                    Bundle()
                )
            )
            .setIconResId(R.drawable.repeat_all)
            .build()
    ),

    REPEAT_ONE(
        customAction = CUSTOM_COMMAND_REPEAT_ONE_ACTION,
        commandButton = CommandButton.Builder()
            .setDisplayName("Repeat One")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REPEAT_ONE_ACTION, Bundle()))
            .setIconResId(R.drawable.repeat_one)
            .build()
    ),

    SHUFFLE(
        customAction = CUSTOM_COMMAND_SHUFFLE_ACTION,
        commandButton = CommandButton.Builder()
            .setDisplayName("Shuffle")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SHUFFLE_ACTION, Bundle()))
            .setIconResId(R.drawable.shuffle)
            .build()
    ),




}

private const val CUSTOM_COMMAND_REWIND_ACTION = "REWIND"
private const val CUSTOM_COMMAND_FORWARD_ACTION = "FORWARD"
const val CUSTOM_COMMAND_ADD_FAVORITE_ACTION = "ADD_FAVORITE"
const val CUSTOM_COMMAND_REMOVE_FAVORITE_ACTION = "REMOVE_FAVORITE"
const val CUSTOM_COMMAND_REPEAT_ONE_ACTION = "REPEAT_ONE"
const val CUSTOM_COMMAND_REPEAT_ALL_ACTION = "REPEAT_ALL"
const val CUSTOM_COMMAND_SHUFFLE_ACTION = "SHUFFLE"
