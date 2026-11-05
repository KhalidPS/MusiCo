package com.k.sekiro.musico.playmusic.presenation.model

import kotlinx.serialization.Serializable

/**
 * Nav route: the equalizer / bass boost screen, opened from the now-playing screen.
 * [accentArgb] is that screen's cover colour, so the equalizer is tinted to the playing song.
 */
@Serializable
data class EqualizerRoute(val accentArgb: Int)
