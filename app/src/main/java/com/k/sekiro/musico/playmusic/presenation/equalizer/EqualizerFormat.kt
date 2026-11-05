package com.k.sekiro.musico.playmusic.presenation.equalizer

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** "60 Hz", "910 Hz", "3.6 kHz", "14 kHz" - one decimal only where it carries information. */
fun formatFrequency(hz: Int): String = when {
    hz < 1000 -> "$hz Hz"
    hz < 10_000 -> {
        val k = (hz / 100f).roundToInt() / 10f
        if (k % 1f == 0f) "${k.toInt()} kHz" else String.format(Locale.US, "%.1f kHz", k)
    }
    else -> "${(hz / 1000f).roundToInt()} kHz"
}

/** Millibels to a signed dB label: "+3.5", "-12", "0". */
fun formatDecibels(millibels: Int): String {
    val db = millibels / 100f
    val rounded = (db * 2).roundToInt() / 2f // half-dB steps, matching the slider snap
    val body = if (rounded % 1f == 0f) {
        abs(rounded.toInt()).toString()
    } else {
        String.format(Locale.US, "%.1f", abs(rounded))
    }
    return when {
        rounded > 0f -> "+$body"
        rounded < 0f -> "-$body"
        else -> "0"
    }
}

/** Slider positions snap to half a decibel - finer than anyone can hear, coarse enough to land on 0. */
fun snapMillibels(millibels: Float, range: IntRange): Int =
    ((millibels / SNAP_MB).roundToInt() * SNAP_MB).coerceIn(range)

private const val SNAP_MB = 50
