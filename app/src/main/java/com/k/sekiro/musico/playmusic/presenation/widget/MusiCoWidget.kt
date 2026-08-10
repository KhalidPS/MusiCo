package com.k.sekiro.musico.playmusic.presenation.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.LocalContext
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.k.sekiro.musico.MainActivity
import com.k.sekiro.musico.R

/** Renders straight from the primitive state PlayerSessionService pushes via WidgetUpdater -
never connects its own MediaController, never touches the player directly. Deliberately
plain (no glance-material3): a fixed dark background keeps the white icons legible in both
system themes without needing day/night ColorProvider plumbing.**/
class MusiCoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            WidgetContent(prefs)
        }
    }
}

private val WidgetBackground = ColorProvider(color = Color(0xFF1A1A1A))
private val WidgetTextPrimary = ColorProvider(color = Color.White)
private val WidgetTextSecondary = ColorProvider(color = Color(0xFFB3B3B3))
private val WidgetFavoriteActive = ColorProvider(color = Color(0xFFFF4D67))

@Composable
private fun WidgetContent(prefs: Preferences) {
    val hasState = prefs[WidgetStateKeys.HasState] ?: false

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        if (!hasState) {
            EmptyState()
        } else {
            PlayingState(prefs)
        }
    }
}

@Composable
private fun EmptyState() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.logo_musico),
            contentDescription = null,
            modifier = GlanceModifier.size(32.dp)
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = "Open MusiCo to start playing",
            style = TextStyle(
                color = WidgetTextSecondary,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun PlayingState(prefs: Preferences) {
    val context = LocalContext.current
    val title = prefs[WidgetStateKeys.Title].orEmpty()
    val artist = prefs[WidgetStateKeys.Artist].orEmpty()
    val isPlaying = prefs[WidgetStateKeys.IsPlaying] ?: false
    val isFavorite = prefs[WidgetStateKeys.IsFavorite] ?: false
    val coverPath = prefs[WidgetStateKeys.CoverPath].orEmpty()
    val coverVersion = prefs[WidgetStateKeys.CoverVersion] ?: 0

    val coverProvider = remember(coverPath, coverVersion) {
        val decoded = coverPath.takeIf { it.isNotBlank() }
            ?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
        if (decoded != null) ImageProvider(decoded) else ImageProvider(R.drawable.logo_2)
    }

    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = coverProvider,
            contentDescription = null,
            modifier = GlanceModifier
                .size(64.dp)
                .cornerRadius(10.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        )

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = title.ifBlank { "Unknown title" },
                maxLines = 1,
                style = TextStyle(color = WidgetTextPrimary, fontWeight = FontWeight.Bold)
            )
            Text(
                text = artist.ifBlank { "Unknown artist" },
                maxLines = 1,
                style = TextStyle(color = WidgetTextSecondary)
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetIconButton(
                    resId = R.drawable.ic_widget_skip_previous,
                    contentDescription = "Previous",
                    onClick = actionRunCallback<PreviousAction>()
                )
                WidgetIconButton(
                    resId = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = actionRunCallback<PlayPauseAction>()
                )
                WidgetIconButton(
                    resId = R.drawable.ic_widget_skip_next,
                    contentDescription = "Next",
                    onClick = actionRunCallback<NextAction>()
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                WidgetIconButton(
                    // Same filled-vs-outline heart as before, but now also tinted so the
                    // "on" state pops against the black background instead of relying on
                    // fill-vs-outline alone (which read as "not clear" at a glance).
                    resId = if (isFavorite) R.drawable.favorite else R.drawable.unfavorite,
                    contentDescription = "Favorite",
                    onClick = actionRunCallback<FavoriteAction>(),
                    tint = if (isFavorite) WidgetFavoriteActive else WidgetTextSecondary,
                    size = 30.dp
                )
            }
        }
    }
}

@Composable
private fun WidgetIconButton(
    resId: Int,
    contentDescription: String,
    onClick: Action,
    tint: ColorProvider = WidgetTextPrimary,
    size: Dp = 34.dp
) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier
            .size(size)
            .padding(4.dp)
            .clickable(onClick)
    )
}
