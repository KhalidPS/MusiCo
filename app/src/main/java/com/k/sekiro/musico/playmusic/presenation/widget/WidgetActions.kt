package com.k.sekiro.musico.playmusic.presenation.widget

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.k.sekiro.musico.playmusic.presenation.player.notification.CUSTOM_COMMAND_ADD_FAVORITE_ACTION
import com.k.sekiro.musico.playmusic.presenation.player.notification.CUSTOM_COMMAND_REMOVE_FAVORITE_ACTION
import com.k.sekiro.musico.playmusic.presenation.player.service.PlayerSessionService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Each widget button tap connects its own short-lived MediaController (mirroring
MediaControllerManager's connection pattern), sends exactly one command, then releases -
Glance's ActionCallback execution model doesn't support holding a persistent connection
across taps, and the resulting binder round-trip latency on rapid double-taps is expected,
not a bug.**/
private suspend fun withMediaController(context: Context, block: suspend (MediaController) -> Unit) {
    val sessionToken = SessionToken(context, ComponentName(context, PlayerSessionService::class.java))
    val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
    try {
        val controller = suspendCancellableCoroutine { cont ->
            controllerFuture.addListener({
                try {
                    cont.resume(controllerFuture.get())
                } catch (ex: Exception) {
                    cont.resumeWithException(ex)
                }
            }, MoreExecutors.directExecutor())
        }
        block(controller)
    } finally {
        MediaController.releaseFuture(controllerFuture)
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withMediaController(context) { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withMediaController(context) { controller -> controller.seekToNext() }
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withMediaController(context) { controller -> controller.seekToPrevious() }
    }
}

class FavoriteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val isFavorite = prefs[WidgetStateKeys.IsFavorite] ?: false
        // Mirrors PlayerSessionService.favoriteOrUnFavoriteCommand: despite the confusing
        // display names on these two custom commands ("Add Favorite" / "Remove Favorite"),
        // the ADD_FAVORITE action is what onCustomCommand actually treats as "currently
        // favorited, so un-favorite it" and REMOVE_FAVORITE as "currently not favorited, so
        // favorite it" - sending the "intuitively" opposite one would silently no-op.
        val action = if (isFavorite) CUSTOM_COMMAND_ADD_FAVORITE_ACTION else CUSTOM_COMMAND_REMOVE_FAVORITE_ACTION

        withMediaController(context) { controller ->
            controller.sendCustomCommand(SessionCommand(action, Bundle()), Bundle.EMPTY)
        }
    }
}
