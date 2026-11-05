package com.k.sekiro.musico.playmusic.domain.equalizer

/**
 * What the user chose on the equalizer screen, in a form that survives process death and can be
 * re-applied to a fresh audio session. Kept free of `android.media.audiofx` so the persistence and
 * clamping rules are plain JVM-testable logic.
 *
 * Band levels are in **millibels** (1/100 dB) - the unit `Equalizer.setBandLevel` takes - and are
 * stored by band index. The number of bands and their range are device-specific, so a stored list
 * is only trusted after [fittedTo] has checked it against what the current device reports.
 */
data class EqualizerSettings(
    val enabled: Boolean = false,
    /** Index into the device's built-in presets, or [CUSTOM_PRESET] when the bands were set by hand. */
    val presetIndex: Int = CUSTOM_PRESET,
    val bandLevels: List<Int> = emptyList(),
    /** 0..[MAX_BASS_BOOST_STRENGTH], the range `BassBoost.setStrength` takes. */
    val bassBoostStrength: Int = 0,
) {

    /**
     * These settings made safe for a device with [bandCount] bands spanning [levelRange] and
     * [presetCount] presets: a preset index the device doesn't have becomes [CUSTOM_PRESET], each
     * level is clamped into range, and a band list of the wrong length - saved on a build/ROM that
     * reported a different band count - is dropped entirely (flat), since mapping band 3 of a
     * 10-band layout onto a 5-band one would boost the wrong frequencies.
     */
    fun fittedTo(bandCount: Int, levelRange: IntRange, presetCount: Int): EqualizerSettings {
        val levels = if (bandLevels.size == bandCount) {
            bandLevels.map { it.coerceIn(levelRange) }
        } else {
            List(bandCount) { 0.coerceIn(levelRange) }
        }
        return copy(
            presetIndex = if (presetIndex in 0 until presetCount) presetIndex else CUSTOM_PRESET,
            bandLevels = levels,
            bassBoostStrength = bassBoostStrength.coerceIn(0, MAX_BASS_BOOST_STRENGTH),
        )
    }

    companion object {
        const val CUSTOM_PRESET = -1
        const val MAX_BASS_BOOST_STRENGTH = 1000

        /** DataStore has no list type, so the levels are kept as a comma-separated string. */
        fun encodeBandLevels(levels: List<Int>): String = levels.joinToString(",")

        /** Anything unparsable yields an empty list, which [fittedTo] then treats as flat. */
        fun decodeBandLevels(raw: String): List<Int> {
            if (raw.isBlank()) return emptyList()
            val parsed = raw.split(',').map { it.trim().toIntOrNull() }
            return if (parsed.any { it == null }) emptyList() else parsed.filterNotNull()
        }
    }
}
