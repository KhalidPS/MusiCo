package com.k.sekiro.musico.playmusic.presenation.equalizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.k.sekiro.musico.playmusic.domain.equalizer.EqualizerSettings
import com.k.sekiro.musico.playmusic.presenation.player.equalizer.EqualizerAvailability
import com.k.sekiro.musico.playmusic.presenation.player.equalizer.EqualizerBand
import com.k.sekiro.musico.playmusic.presenation.player.equalizer.EqualizerState
import com.k.sekiro.musico.ui.theme.MusiCoTheme
import com.k.sekiro.musico.ui.theme.appDimens
import com.k.sekiro.musico.ui.theme.deviceConfiguration
import kotlin.math.roundToInt

/**
 * Equalizer + bass boost. Stateless: everything comes in through [state] and goes out through the
 * callbacks, which map 1:1 onto `EqualizerController`.
 *
 * [accent] is the playing song's cover colour, handed over from the now-playing screen, so the
 * curve and controls read as part of the song that's playing.
 */
@Composable
fun EqualizerScreen(
    state: EqualizerState,
    accent: Color,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onBandChange: (band: Int, millibels: Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReady = state.availability == EqualizerAvailability.Ready

    Column(modifier = modifier.fillMaxSize()) {
        TopBar(
            enabled = state.enabled,
            showControls = isReady,
            accent = accent,
            onBack = onBack,
            onEnabledChange = onEnabledChange,
            onReset = onReset,
        )

        when (state.availability) {
            EqualizerAvailability.Waiting -> CenteredMessage(
                title = "Nothing playing yet",
                body = "Start a song and the equalizer will be ready for it.",
            )

            EqualizerAvailability.Unsupported -> CenteredMessage(
                title = "Equalizer not available",
                body = "This device doesn't let apps add an equalizer to their audio. " +
                    "Playback isn't affected.",
            )

            EqualizerAvailability.Ready -> ReadyContent(
                state = state,
                accent = accent,
                onPresetSelected = onPresetSelected,
                onBandChange = onBandChange,
                onBassBoostChange = onBassBoostChange,
            )
        }
    }
}

@Composable
private fun TopBar(
    enabled: Boolean,
    showControls: Boolean,
    accent: Color,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Default.ArrowBackIos, contentDescription = "back")
        }
        Text(
            text = "Equalizer",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (showControls) {
            IconButton(onClick = onReset) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset to flat")
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    checkedThumbColor = Color.White,
                ),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .semantics { contentDescription = "Equalizer on" },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadyContent(
    state: EqualizerState,
    accent: Color,
    onPresetSelected: (Int) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
) {
    // One clock for the whole entrance; every element takes its own slice of it (see reveal()).
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, tween(ENTER_MS, easing = FastOutSlowInEasing))
    }
    val enterProgress = { enter.value }
    val panelHeight = appDimens.equalizer.bandsPanelHeight

    val bands: @Composable (Modifier) -> Unit = { mod ->
        BandsSection(
            bands = state.bands,
            range = state.levelRange,
            enabled = state.enabled,
            accent = accent,
            panelHeight = panelHeight,
            enterProgress = enterProgress,
            onBandChange = onBandChange,
            modifier = mod,
        )
    }
    val side: @Composable () -> Unit = {
        ControlLostBanner(visible = !state.hasControl)
        if (state.presets.isNotEmpty()) {
            PresetsSection(
                presets = state.presets,
                selected = state.selectedPreset,
                accent = accent,
                onPresetSelected = onPresetSelected,
                modifier = Modifier.reveal(enterProgress, delay = 0.30f, dx = 48.dp),
            )
        }
        if (state.bassBoostSupported) {
            BassBoostCard(
                strength = state.bassBoostStrength,
                accent = accent,
                onChange = onBassBoostChange,
                modifier = Modifier.reveal(enterProgress, delay = 0.42f, dy = 40.dp),
            )
        }
    }

    // A landscape phone has width to spare and no height: put the bands beside the controls
    // rather than above them, so the panel keeps a usable height.
    if (deviceConfiguration.isMobileLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            bands(Modifier.weight(1.3f))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) { side() }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            bands(Modifier.fillMaxWidth())
            side()
        }
    }
}

@Composable
private fun BandsSection(
    bands: List<EqualizerBand>,
    range: IntRange,
    enabled: Boolean,
    accent: Color,
    panelHeight: Dp,
    enterProgress: () -> Float,
    onBandChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth().reveal(enterProgress, delay = 0.10f)) {
            bands.forEach { band ->
                Text(
                    text = formatDecibels(band.level),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (band.level != 0 && enabled) accent else labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        BandsPanel(
            bands = bands,
            range = range,
            enabled = enabled,
            accent = accent,
            enterProgress = enterProgress,
            onBandChange = onBandChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().reveal(enterProgress, delay = 0.10f)) {
            bands.forEach { band ->
                Text(
                    text = formatFrequency(band.centerHz),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * The band sliders, drawn as one canvas so a single response curve can run through all the
 * thumbs. Touch is handled by an invisible column per band on top, which is also where each
 * band's accessibility node lives.
 *
 * Displayed levels are [Animatable]s chasing the state: a preset change springs every band to its
 * new level (staggered left to right), while the band under the finger snaps so it tracks the
 * finger exactly. All per-frame reads happen in the draw phase.
 */
@Composable
private fun BandsPanel(
    bands: List<EqualizerBand>,
    range: IntRange,
    enabled: Boolean,
    accent: Color,
    enterProgress: () -> Float,
    onBandChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnBandChange by rememberUpdatedState(onBandChange)
    val haptics = LocalHapticFeedback.current
    var draggingBand by remember { mutableIntStateOf(NO_BAND) }

    val displayed = remember(bands.size) { List(bands.size) { Animatable(bands[it].level.toFloat()) } }
    bands.forEachIndexed { i, band ->
        LaunchedEffect(displayed, i, band.level) {
            if (draggingBand == i) {
                displayed[i].snapTo(band.level.toFloat())
            } else {
                kotlinx.coroutines.delay(i * PRESET_STAGGER_MS)
                displayed[i].animateTo(band.level.toFloat(), spring(dampingRatio = 0.7f, stiffness = 160f))
            }
        }
    }

    val thumbScale = remember { Animatable(1f) }
    var scaledBand by remember { mutableIntStateOf(NO_BAND) }
    LaunchedEffect(draggingBand) {
        if (draggingBand != NO_BAND) {
            scaledBand = draggingBand
            thumbScale.animateTo(1.35f, spring(dampingRatio = 0.55f, stiffness = 400f))
        } else {
            thumbScale.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 400f))
        }
    }

    val intensity by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.35f,
        animationSpec = tween(250),
        label = "eqIntensity",
    )

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val thumbFill = MaterialTheme.colorScheme.surface
    val min = range.first.toFloat()
    val max = range.last.toFloat()
    val span = (max - min).coerceAtLeast(1f)

    Box(modifier = modifier) {
        Canvas(Modifier.matchParentSize()) {
            val n = bands.size
            if (n == 0) return@Canvas
            val pad = PANEL_PAD.toPx()
            val usable = size.height - 2 * pad
            fun yOf(level: Float) = pad + (1f - (level - min) / span) * usable
            val colW = size.width / n
            fun xOf(i: Int) = colW * (i + 0.5f)
            val zeroY = yOf(0f.coerceIn(min, max))
            val p = enterProgress()
            val accentNow = accent.copy(alpha = accent.alpha * intensity)

            // 0 dB guide
            drawLine(
                color = guideColor,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
            )

            // Entrance: each band rises out of the flat 0 dB line, left to right.
            val levels = List(n) { i -> displayed[i].value * slice(p, 0.05f + i * 0.06f) }
            val points = List(n) { i -> Offset(xOf(i), yOf(levels[i])) }

            // Tracks + the active segment from 0 dB to the level.
            val trackW = 4.dp.toPx()
            for (i in 0 until n) {
                val x = xOf(i)
                drawLine(trackColor, Offset(x, pad), Offset(x, size.height - pad), trackW, StrokeCap.Round)
                drawLine(accentNow, Offset(x, zeroY), points[i], trackW, StrokeCap.Round)
            }

            // Response curve: flat run-in from the left edge, smooth through each thumb (cubic
            // through midpoints), flat run-out to the right edge.
            val curve = Path().apply {
                moveTo(0f, points.first().y)
                lineTo(points.first().x, points.first().y)
                for (i in 1 until n) {
                    val a = points[i - 1]
                    val b = points[i]
                    val midX = (a.x + b.x) / 2f
                    cubicTo(midX, a.y, midX, b.y, b.x, b.y)
                }
                lineTo(size.width, points.last().y)
            }
            val fill = Path().apply {
                addPath(curve)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.32f * intensity),
                    1f to accent.copy(alpha = 0f),
                ),
            )
            drawPath(curve, accentNow, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            // Thumbs
            val baseR = THUMB_RADIUS.toPx()
            for (i in 0 until n) {
                val r = baseR * (if (i == scaledBand) thumbScale.value else 1f)
                val a = slice(p, 0.05f + i * 0.06f)
                drawCircle(thumbFill.copy(alpha = a), radius = r, center = points[i])
                drawCircle(accentNow.copy(alpha = accentNow.alpha * a), radius = r, center = points[i], style = Stroke(3.dp.toPx()))
            }
        }

        Row(Modifier.matchParentSize()) {
            bands.forEachIndexed { i, band ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(i, range) {
                            val pad = PANEL_PAD.toPx()
                            fun levelAt(y: Float): Int {
                                val frac = 1f - ((y - pad) / (size.height - 2 * pad)).coerceIn(0f, 1f)
                                return snapMillibels(min + frac * span, range)
                            }
                            var last = Int.MIN_VALUE
                            fun emit(level: Int) {
                                if (level == last) return
                                // A tick on landing exactly on 0 dB, the one spot people aim for.
                                if (level == 0 && last != Int.MIN_VALUE) {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                                last = level
                                currentOnBandChange(i, level)
                            }
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                draggingBand = i
                                last = Int.MIN_VALUE
                                emit(levelAt(down.position.y))
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    emit(levelAt(change.position.y))
                                    change.consume()
                                }
                                draggingBand = NO_BAND
                            }
                        }
                        .semantics {
                            contentDescription = "${formatFrequency(band.centerHz)} band"
                            stateDescription = "${formatDecibels(band.level)} dB"
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = band.level.toFloat(),
                                range = min..max,
                            )
                            setProgress { target ->
                                currentOnBandChange(i, snapMillibels(target, range))
                                true
                            }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetsSection(
    presets: List<String>,
    selected: Int,
    accent: Color,
    onPresetSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Presets")
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.copy(alpha = 0.22f),
            selectedLeadingIconColor = accent,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // "Custom" isn't something you pick - it's what you get by moving a band - so it
            // only shows up, already selected, once you have.
            AnimatedVisibility(
                visible = selected == EqualizerSettings.CUSTOM_PRESET,
                enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 400f)) + fadeIn(),
                exit = scaleOut(tween(120)) + fadeOut(tween(120)),
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Custom") },
                    leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) },
                    colors = chipColors,
                )
            }
            presets.forEachIndexed { index, name ->
                val isSelected = index == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onPresetSelected(index) },
                    label = { Text(name) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null,
                    colors = chipColors,
                )
            }
        }
    }
}

@Composable
private fun BassBoostCard(
    strength: Int,
    accent: Color,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SpeakerGroup, contentDescription = null, tint = accent)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Bass boost",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(strength / 10f).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (strength > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = strength.toFloat(),
                onValueChange = { onChange(it.roundToInt()) },
                valueRange = 0f..EqualizerSettings.MAX_BASS_BOOST_STRENGTH.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                ),
                modifier = Modifier.semantics { contentDescription = "Bass boost" },
            )
        }
    }
}

@Composable
private fun ControlLostBanner(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Another app or your phone's sound settings are controlling audio effects " +
                        "right now. Your changes are saved and apply once it lets go.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CenteredMessage(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// --- motion helpers ------------------------------------------------------------------------

/** This element's share of a shared 0..1 clock: starts at [delay], still finishes at 1. */
private fun slice(progress: Float, delay: Float): Float =
    ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)

/**
 * Fade + slide in from ([dx], [dy]) on its slice of the shared clock. Read in the graphics layer,
 * so the entrance costs no recomposition.
 */
private fun Modifier.reveal(
    progress: () -> Float,
    delay: Float,
    dx: Dp = 0.dp,
    dy: Dp = 0.dp,
): Modifier = graphicsLayer {
    val t = slice(progress(), delay)
    alpha = t
    translationX = (1f - t) * dx.toPx()
    translationY = (1f - t) * dy.toPx()
}

private const val ENTER_MS = 900
private const val PRESET_STAGGER_MS = 25L
private const val NO_BAND = -1
private val PANEL_PAD = 14.dp
private val THUMB_RADIUS = 9.dp

@Preview(showBackground = true)
@Composable
private fun EqualizerScreenPreview() {
    MusiCoTheme {
        Surface {
            EqualizerScreen(
                state = EqualizerState(
                    availability = EqualizerAvailability.Ready,
                    enabled = true,
                    bands = listOf(
                        EqualizerBand(60, 500), EqualizerBand(230, 300), EqualizerBand(910, 0),
                        EqualizerBand(3600, 200), EqualizerBand(14000, 400),
                    ),
                    presets = listOf("Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock"),
                    selectedPreset = EqualizerSettings.CUSTOM_PRESET,
                    bassBoostSupported = true,
                    bassBoostStrength = 350,
                ),
                accent = Color(0xFF30C6FF),
                onBack = {}, onEnabledChange = {}, onPresetSelected = {},
                onBandChange = { _, _ -> }, onBassBoostChange = {}, onReset = {},
            )
        }
    }
}
