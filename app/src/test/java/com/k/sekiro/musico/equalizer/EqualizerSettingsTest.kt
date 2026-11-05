package com.k.sekiro.musico.equalizer

import com.k.sekiro.musico.playmusic.domain.equalizer.EqualizerSettings
import com.k.sekiro.musico.playmusic.domain.equalizer.EqualizerSettings.Companion.CUSTOM_PRESET
import com.k.sekiro.musico.playmusic.presenation.equalizer.formatDecibels
import com.k.sekiro.musico.playmusic.presenation.equalizer.formatFrequency
import com.k.sekiro.musico.playmusic.presenation.equalizer.snapMillibels
import org.junit.Assert.assertEquals
import org.junit.Test

class EqualizerSettingsTest {

    private val range = -1500..1500

    @Test
    fun `band levels round-trip through the stored string`() {
        val levels = listOf(300, -150, 0, 1200, -1500)
        val raw = EqualizerSettings.encodeBandLevels(levels)
        assertEquals(levels, EqualizerSettings.decodeBandLevels(raw))
    }

    @Test
    fun `blank or corrupt stored levels decode to nothing rather than a partial list`() {
        assertEquals(emptyList<Int>(), EqualizerSettings.decodeBandLevels(""))
        assertEquals(emptyList<Int>(), EqualizerSettings.decodeBandLevels("300,abc,0"))
        assertEquals(emptyList<Int>(), EqualizerSettings.decodeBandLevels("300,,0"))
    }

    @Test
    fun `levels outside the device range are clamped`() {
        val fitted = EqualizerSettings(bandLevels = listOf(2000, -2000, 100))
            .fittedTo(bandCount = 3, levelRange = range, presetCount = 10)
        assertEquals(listOf(1500, -1500, 100), fitted.bandLevels)
    }

    @Test
    fun `levels saved for a different band count are dropped to flat, not remapped`() {
        val fitted = EqualizerSettings(bandLevels = List(10) { 600 })
            .fittedTo(bandCount = 5, levelRange = range, presetCount = 10)
        assertEquals(List(5) { 0 }, fitted.bandLevels)
    }

    @Test
    fun `flat means 0 dB even on a range that does not include it`() {
        // Defensive: 0 is coerced into range so a weird device can't make us write out of range.
        val fitted = EqualizerSettings().fittedTo(bandCount = 2, levelRange = 100..900, presetCount = 0)
        assertEquals(listOf(100, 100), fitted.bandLevels)
    }

    @Test
    fun `a preset the device does not have falls back to custom`() {
        val fitted = EqualizerSettings(presetIndex = 12)
            .fittedTo(bandCount = 5, levelRange = range, presetCount = 10)
        assertEquals(CUSTOM_PRESET, fitted.presetIndex)
        val kept = EqualizerSettings(presetIndex = 3)
            .fittedTo(bandCount = 5, levelRange = range, presetCount = 10)
        assertEquals(3, kept.presetIndex)
    }

    @Test
    fun `bass boost strength is clamped to the platform range`() {
        assertEquals(1000, EqualizerSettings(bassBoostStrength = 5000).fittedTo(5, range, 0).bassBoostStrength)
        assertEquals(0, EqualizerSettings(bassBoostStrength = -10).fittedTo(5, range, 0).bassBoostStrength)
    }

    @Test
    fun `frequency labels`() {
        assertEquals("60 Hz", formatFrequency(60))
        assertEquals("910 Hz", formatFrequency(910))
        assertEquals("1 kHz", formatFrequency(1000))
        assertEquals("3.6 kHz", formatFrequency(3600))
        assertEquals("14 kHz", formatFrequency(14000))
    }

    @Test
    fun `decibel labels are signed and drop a pointless decimal`() {
        assertEquals("0", formatDecibels(0))
        assertEquals("+3", formatDecibels(300))
        assertEquals("+3.5", formatDecibels(350))
        assertEquals("-12", formatDecibels(-1200))
        assertEquals("-0.5", formatDecibels(-50))
    }

    @Test
    fun `slider positions snap to half-decibel steps inside the range`() {
        assertEquals(0, snapMillibels(20f, range))
        assertEquals(50, snapMillibels(30f, range))
        assertEquals(-350, snapMillibels(-340f, range))
        assertEquals(1500, snapMillibels(1720f, range))
    }
}
