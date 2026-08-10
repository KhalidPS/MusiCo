package com.k.sekiro.musico.playmusic.presenation.widget

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Each widget button tap connects its own short-lived MediaController (mirroring
MediaControllerManager's connection pattern), sends exactly one command, then releases -
Glance's ActionCallback execution model doesn't support holding a persistent connection
across taps, and the resulting binder round-trip latency on rapid double-taps is expected,
not a bug.**/
private suspend fun withMediaController(context: Context, block: suspend (MediaController) -> Unit) {
    // MediaController enforces same-thread access for every method call, verified against
    // whatever thread it was *built* on - and that thread must have a Looper. Glance invokes
    // ActionCallback.onAction on a background dispatcher, not main, so both the build and
    // every subsequent controller call must be explicitly forced onto Dispatchers.Main.
    withContext(Dispatchers.Main) {
        Log.e("ks", "widget: connecting MediaController")
        // ActionCallback's context is a ReceiverRestrictedContext (Glance routes action
        // execution through its internal BroadcastReceiver) - bindService() (which
        // MediaController.Builder does internally to connect to PlayerSessionService) is
        // hard-blocked from that context with ReceiverCallNotAllowedException.
        // applicationContext is never receiver-restricted.
        val appContext = context.applicationContext
        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlayerSessionService::class.java))
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        try {
            val controller = suspendCancellableCoroutine { cont ->
                controllerFuture.addListener({
                    try {
                        cont.resume(controllerFuture.get())
                    } catch (ex: Exception) {
                        Log.e("ks", "widget: MediaController connect failed: ${ex.message}", ex)
                        cont.resumeWithException(ex)
                    }
                }, MoreExecutors.directExecutor())
            }
            Log.e("ks", "widget: MediaController connected, running command")
            block(controller)
            Log.e("ks", "widget: command sent")
        } catch (ex: Exception) {
            Log.e("ks", "widget: withMediaController failed: ${ex.message}", ex)
        } finally {
            MediaController.releaseFuture(controllerFuture)
        }
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.e("ks", "widget: PlayPauseAction.onAction fired")
        withMediaController(context) { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.e("ks", "widget: NextAction.onAction fired")
        withMediaController(context) { controller -> controller.seekToNext() }
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.e("ks", "widget: PreviousAction.onAction fired")
        withMediaController(context) { controller -> controller.seekToPrevious() }
    }
}

class FavoriteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.e("ks", "widget: FavoriteAction.onAction fired")
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
