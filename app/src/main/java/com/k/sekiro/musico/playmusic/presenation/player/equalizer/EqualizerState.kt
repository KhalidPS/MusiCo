package com.k.sekiro.musico.playmusic.presenation.player.equalizer

import com.k.sekiro.musico.playmusic.domain.equalizer.EqualizerSettings

enum class EqualizerAvailability {
    /** No audio session has been attached yet, so the device's bands/presets aren't known. */
    Waiting,
    Ready,
    /** The device refused to create an equalizer for our session (missing effect, OEM block...). */
    Unsupported,
}

/** One frequency band, as this device reports it. [level] is in millibels. */
data class EqualizerBand(val centerHz: Int, val level: Int)

/**
 * Everything the equalizer screen renders. Capabilities ([bands], [levelRange], [presets],
 * [bassBoostSupported]) come from the device; the rest mirrors the user's [EqualizerSettings].
 */
data class EqualizerState(
    val availability: EqualizerAvailability = EqualizerAvailability.Waiting,
    val enabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val levelRange: IntRange = -1500..1500,
    val presets: List<String> = emptyList(),
    val selectedPreset: Int = EqualizerSettings.CUSTOM_PRESET,
    val bassBoostSupported: Boolean = false,
    val bassBoostStrength: Int = 0,
    /**
     * False while another app (a system sound enhancer, a third-party EQ) holds a higher-priority
     * effect on our session - our values are kept but the platform ignores them until it lets go.
     */
    val hasControl: Boolean = true,
)
