package com.k.sekiro.musico.playmusic.presenation.played_song.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.k.sekiro.musico.playmusic.presenation.model.SleepTimerMode
import com.k.sekiro.musico.playmusic.presenation.model.SleepTimerState
import com.k.sekiro.musico.playmusic.presenation.model.fromMillis
import com.k.sekiro.musico.ui.theme.SkyBlue
import kotlin.math.roundToInt

private val presetMinutes = listOf(5, 15, 30, 45, 60, 90)
private const val MIN_MINUTES = 1f
private const val MAX_MINUTES = 180f
private const val DEFAULT_MINUTES = 30f

private val DarkDialogBase = Color(0xFF17171D)
private val LightDialogBase = Color(0xFFF6F5F8)

/** Picks readable text/icon color for whatever tinted surface [background] ends up being,
since that surface is a blend of the theme base and the cover-art accent color and isn't
known ahead of time. **/
private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White

@Composable
fun SleepTimerDialog(
    sleepTimer: SleepTimerState,
    modifier: Modifier = Modifier,
    accentColor: Color = SkyBlue,
    coverUrl: String? = null,
    onDismissRequest: () -> Unit,
    onDurationSelected: (Long) -> Unit,
    onEndOfTrackSelected: () -> Unit,
    onCancelTimer: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val dialogSurfaceColor = remember(isDarkTheme, accentColor) {
        val base = if (isDarkTheme) DarkDialogBase else LightDialogBase
        lerp(base, accentColor, if (isDarkTheme) 0.14f else 0.10f)
    }
    val onDialogColor = remember(dialogSurfaceColor) { contentColorFor(dialogSurfaceColor) }
    val onAccentColor = remember(accentColor) { contentColorFor(accentColor) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogSurfaceColor,
            modifier = modifier.fillMaxWidth(.92f)
        ) {
            Box {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(55.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(dialogSurfaceColor.copy(alpha = if (isDarkTheme) 0.82f else 0.88f))
                    )
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sleep Timer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = onDialogColor,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = onDialogColor.copy(alpha = .7f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val isActive = sleepTimer is SleepTimerState.Active
                    AnimatedContent(
                        targetState = isActive,
                        transitionSpec = {
                            (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 6 }) togetherWith
                                (fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 6 })
                        },
                        label = "sleepTimerDialogContent"
                    ) { active ->
                        if (active) {
                            val timer = sleepTimer as? SleepTimerState.Active
                            if (timer != null) {
                                ActiveTimerContent(
                                    timer = timer,
                                    accentColor = accentColor,
                                    onDialogColor = onDialogColor,
                                    onCancelTimer = onCancelTimer,
                                )
                            }
                        } else {
                            SetupTimerContent(
                                accentColor = accentColor,
                                onAccentColor = onAccentColor,
                                onDialogColor = onDialogColor,
                                onDurationSelected = onDurationSelected,
                                onEndOfTrackSelected = onEndOfTrackSelected,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTimerContent(
    timer: SleepTimerState.Active,
    accentColor: Color,
    onDialogColor: Color,
    onCancelTimer: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.height(8.dp))

        CircularSleepTimer(
            mode = timer.mode,
            remainingMillis = timer.remainingMillis,
            accentColor = accentColor,
            onDialogColor = onDialogColor,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            when (timer.mode) {
                is SleepTimerMode.Duration -> "Playback will pause automatically"
                SleepTimerMode.EndOfTrack -> "Playback will pause when this track ends"
            },
            color = onDialogColor.copy(alpha = .65f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onCancelTimer,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B))
        ) {
            Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cancel Timer", fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Duration mode shrinks a solid ring from full to empty as [remainingMillis] counts down to
zero; end-of-track mode has no fixed length to show progress against, so instead a short arc
spins and pulses to read as "waiting", not "counting". **/
@Composable
private fun CircularSleepTimer(
    mode: SleepTimerMode,
    remainingMillis: Long,
    accentColor: Color,
    onDialogColor: Color,
    modifier: Modifier = Modifier,
) {
    val targetFraction = when (mode) {
        is SleepTimerMode.Duration ->
            if (mode.totalMillis > 0) (remainingMillis.toFloat() / mode.totalMillis.toFloat()).coerceIn(0f, 1f) else 0f
        SleepTimerMode.EndOfTrack -> 1f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "sleepTimerRingProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sleepTimerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sleepTimerPulseAlpha"
    )
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2600, easing = LinearEasing)),
        label = "sleepTimerSpin"
    )

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 12.dp.toPx()
            drawArc(
                color = onDialogColor.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        if (mode is SleepTimerMode.Duration) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 12.dp.toPx()
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedFraction,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .rotate(spinAngle)
            ) {
                val strokeWidth = 12.dp.toPx()
                drawArc(
                    color = accentColor.copy(alpha = pulseAlpha),
                    startAngle = -90f,
                    sweepAngle = 65f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.wrapContentSize()
        ) {
            when (mode) {
                is SleepTimerMode.Duration -> {
                    AnimatedContent(
                        targetState = fromMillis(remainingMillis),
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 3 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 3 })
                        },
                        label = "sleepTimerCountdownText"
                    ) { text ->
                        Text(
                            text,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = onDialogColor
                        )
                    }
                    Text(
                        "remaining",
                        color = onDialogColor.copy(alpha = .55f),
                        fontSize = 12.sp
                    )
                }

                SleepTimerMode.EndOfTrack -> {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "end of\ntrack",
                        textAlign = TextAlign.Center,
                        color = onDialogColor.copy(alpha = .6f),
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupTimerContent(
    accentColor: Color,
    onAccentColor: Color,
    onDialogColor: Color,
    onDurationSelected: (Long) -> Unit,
    onEndOfTrackSelected: () -> Unit,
) {
    var selectedMinutes by remember { mutableFloatStateOf(DEFAULT_MINUTES) }
    val roundedMinutes = selectedMinutes.roundToInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$roundedMinutes",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = onDialogColor
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (roundedMinutes == 1) "minute" else "minutes",
                fontSize = 16.sp,
                color = onDialogColor.copy(alpha = .6f),
                modifier = Modifier.padding(bottom = 9.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Slider(
            value = selectedMinutes,
            onValueChange = { selectedMinutes = it },
            valueRange = MIN_MINUTES..MAX_MINUTES,
            colors = SliderDefaults.colors(
                activeTrackColor = accentColor,
                thumbColor = accentColor,
                inactiveTrackColor = onDialogColor.copy(alpha = .15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        PresetChipsRow(
            selectedMinutes = roundedMinutes,
            accentColor = accentColor,
            onAccentColor = onAccentColor,
            onDialogColor = onDialogColor,
            onPresetSelected = { selectedMinutes = it.toFloat() }
        )

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = { onDurationSelected(roundedMinutes * 60_000L) },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = onAccentColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Start Timer", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(10.dp))

        TextButton(
            onClick = onEndOfTrackSelected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = onDialogColor.copy(alpha = .7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Stop at end of current track", color = onDialogColor.copy(alpha = .85f))
        }
    }
}

@Composable
private fun PresetChipsRow(
    selectedMinutes: Int,
    accentColor: Color,
    onAccentColor: Color,
    onDialogColor: Color,
    onPresetSelected: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        presetMinutes.forEach { minutes ->
            val selected = selectedMinutes == minutes
            FilterChip(
                selected = selected,
                onClick = { onPresetSelected(minutes) },
                label = { Text("${minutes}m", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = onDialogColor.copy(alpha = .08f),
                    labelColor = onDialogColor.copy(alpha = .75f),
                    selectedContainerColor = accentColor,
                    selectedLabelColor = onAccentColor
                ),
                border = null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview
@Composable
private fun SleepTimerDialogOffPrev() {
    SleepTimerDialog(
        sleepTimer = SleepTimerState.Off,
        onDismissRequest = {},
        onDurationSelected = {},
        onEndOfTrackSelected = {},
        onCancelTimer = {}
    )
}

@Preview
@Composable
private fun SleepTimerDialogActiveDurationPrev() {
    SleepTimerDialog(
        sleepTimer = SleepTimerState.Active(SleepTimerMode.Duration(15 * 60_000L), 12 * 60_000L + 34_000L),
        onDismissRequest = {},
        onDurationSelected = {},
        onEndOfTrackSelected = {},
        onCancelTimer = {}
    )
}

@Preview
@Composable
private fun SleepTimerDialogActiveEndOfTrackPrev() {
    SleepTimerDialog(
        sleepTimer = SleepTimerState.Active(SleepTimerMode.EndOfTrack, 0L),
        onDismissRequest = {},
        onDurationSelected = {},
        onEndOfTrackSelected = {},
        onCancelTimer = {}
    )
}
