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
            commandButton = CommandButton.Builder(R.drawable.rewind_5)
                .setDisplayName("Rewind")
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REWIND_ACTION, Bundle()))
                .build()
        ),

        FORWARD(
            customAction = CUSTOM_COMMAND_FORWARD_ACTION,
            commandButton = CommandButton.Builder(R.drawable.forward_5)
                .setDisplayName("Forward")
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_FORWARD_ACTION, Bundle()))
                .build()
        ),*/

    FAVORITE(
        customAction = CUSTOM_COMMAND_ADD_FAVORITE_ACTION,
        commandButton = CommandButton.Builder(R.drawable.favorite)
            .setDisplayName("Add Favorite")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_ADD_FAVORITE_ACTION, Bundle()))
            .build()
    ),

    REPEAT_ALL(
        customAction = CUSTOM_COMMAND_REPEAT_ALL_ACTION,
        commandButton = CommandButton.Builder(R.drawable.repeat_all)
            .setDisplayName("Repeat All")
            .setSessionCommand(
                SessionCommand(
                    CUSTOM_COMMAND_REPEAT_ALL_ACTION,
                    Bundle()
                )
            )
            .build()
    ),

    REPEAT_ONE(
        customAction = CUSTOM_COMMAND_REPEAT_ONE_ACTION,
        commandButton = CommandButton.Builder(R.drawable.repeat_one)
            .setDisplayName("Repeat One")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REPEAT_ONE_ACTION, Bundle()))
            .build()
    ),

    SHUFFLE(
        customAction = CUSTOM_COMMAND_SHUFFLE_ACTION,
        commandButton = CommandButton.Builder(R.drawable.shuffle)
            .setDisplayName("Shuffle")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SHUFFLE_ACTION, Bundle()))
            .build()
    ),

    UNFAVORITE(
        customAction = CUSTOM_COMMAND_REMOVE_FAVORITE_ACTION,
        commandButton = CommandButton.Builder(R.drawable.unfavorite)
            .setDisplayName("Remove Favorite")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REMOVE_FAVORITE_ACTION, Bundle()))
            .build()
    )




}

private const val CUSTOM_COMMAND_REWIND_ACTION = "REWIND"
private const val CUSTOM_COMMAND_FORWARD_ACTION = "FORWARD"
const val CUSTOM_COMMAND_ADD_FAVORITE_ACTION = "ADD_FAVORITE"
const val CUSTOM_COMMAND_REMOVE_FAVORITE_ACTION = "REMOVE_FAVORITE"
const val CUSTOM_COMMAND_REPEAT_ONE_ACTION = "REPEAT_ONE"
const val CUSTOM_COMMAND_REPEAT_ALL_ACTION = "REPEAT_ALL"
const val CUSTOM_COMMAND_SHUFFLE_ACTION = "SHUFFLE"
