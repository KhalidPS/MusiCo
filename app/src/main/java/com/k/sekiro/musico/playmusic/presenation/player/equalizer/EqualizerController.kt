package com.k.sekiro.musico.playmusic.presenation.player.equalizer

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver
import com.k.sekiro.musico.playmusic.domain.equalizer.EqualizerSettings
import com.k.sekiro.musico.playmusic.domain.equalizer.EqualizerSettings.Companion.CUSTOM_PRESET
import com.k.sekiro.musico.playmusic.domain.model.BassBoostStrength_KEY
import com.k.sekiro.musico.playmusic.domain.model.EqualizerBandLevels_KEY
import com.k.sekiro.musico.playmusic.domain.model.EqualizerEnabled_KEY
import com.k.sekiro.musico.playmusic.domain.model.EqualizerPreset_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the platform [Equalizer] and [BassBoost] for the player's audio session and keeps them in
 * step with the user's saved [EqualizerSettings].
 *
 * Lifetime: a Koin singleton, so the UI and [PlayerSessionService] (same process) share one
 * instance. The *effects* follow the service - it calls [attach] whenever ExoPlayer reports an
 * audio session and [release] before releasing the player - while the *settings and state*
 * outlive it, so the screen keeps rendering and edits made between sessions are applied on the
 * next [attach].
 *
 * Every method must be called on the main thread (the player's application thread, and where the
 * screen calls from). Every platform call is guarded: OEM effect stacks throw from constructors and
 * setters (`UnsupportedOperationException`, `IllegalStateException`, `RuntimeException`), and an
 * equalizer failure must never take playback down with it.
 */
class EqualizerController(private val dataSaver: SimpleDataSaver) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var attachedSessionId = NO_SESSION

    /** Null until loaded from DataStore on the first [attach]; setters no-op before that. */
    private var settings: EqualizerSettings? = null

    private var attachJob: Job? = null
    private var persistJob: Job? = null

    /**
     * Binds the effects to [audioSessionId]. Session 0 is ignored on purpose: an effect created on
     * session 0 attaches to the *global output mix* and would equalize every app on the device.
     * ExoPlayer starts at 0 (`C.AUDIO_SESSION_ID_UNSET`) and generates the real id on its playback
     * thread a moment later, reporting it through `onAudioSessionIdChanged`.
     */
    fun attach(audioSessionId: Int) {
        if (audioSessionId == NO_SESSION) return
        if (audioSessionId == attachedSessionId && equalizer != null) return
        attachJob?.cancel()
        attachJob = scope.launch {
            val saved = settings ?: loadSettings()
            releaseEffects()
            createEffects(audioSessionId, saved)
        }
    }

    fun release() {
        attachJob?.cancel()
        releaseEffects()
    }

    fun setEnabled(enabled: Boolean) {
        val current = settings ?: return
        applyEnabled(enabled)
        commit(current.copy(enabled = enabled))
    }

    fun selectPreset(index: Int) {
        val current = settings ?: return
        if (index !in _state.value.presets.indices) return
        val eq = equalizer
        // Without a live effect we can't know what the preset's curve looks like, so keep the
        // old levels and let the next attach() apply the preset and read them back.
        val levels = if (eq != null) {
            guard("usePreset") { eq.usePreset(index.toShort()) }
            readBandLevels(eq) ?: current.bandLevels
        } else {
            current.bandLevels
        }
        turnOnIfOff(current)
        commit(settings!!.copy(presetIndex = index, bandLevels = levels))
    }

    /** [level] in millibels; moving any band by hand leaves the preset ("Custom"). */
    fun setBandLevel(band: Int, level: Int) {
        val current = settings ?: return
        if (band !in current.bandLevels.indices) return
        val clamped = level.coerceIn(_state.value.levelRange)
        equalizer?.let { eq -> guard("setBandLevel") { eq.setBandLevel(band.toShort(), clamped.toShort()) } }
        turnOnIfOff(current)
        val levels = settings!!.bandLevels.toMutableList().also { it[band] = clamped }
        commit(settings!!.copy(presetIndex = CUSTOM_PRESET, bandLevels = levels))
    }

    fun setBassBoostStrength(strength: Int) {
        val current = settings ?: return
        val clamped = strength.coerceIn(0, EqualizerSettings.MAX_BASS_BOOST_STRENGTH)
        bassBoost?.let { bb -> guard("setStrength") { bb.setStrength(clamped.toShort()) } }
        turnOnIfOff(current)
        commit(settings!!.copy(bassBoostStrength = clamped))
    }

    /** Flattens every band and turns bass boost off; leaves the on/off switch alone. */
    fun reset() {
        val current = settings ?: return
        val flat = List(current.bandLevels.size) { 0.coerceIn(_state.value.levelRange) }
        equalizer?.let { eq ->
            flat.forEachIndexed { band, level ->
                guard("setBandLevel") { eq.setBandLevel(band.toShort(), level.toShort()) }
            }
        }
        bassBoost?.let { bb -> guard("setStrength") { bb.setStrength(0) } }
        commit(current.copy(presetIndex = CUSTOM_PRESET, bandLevels = flat, bassBoostStrength = 0))
    }

    // --- internals -------------------------------------------------------------------------

    private fun createEffects(sessionId: Int, saved: EqualizerSettings) {
        val eq = try {
            Equalizer(EFFECT_PRIORITY, sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Equalizer unavailable on session $sessionId", e)
            _state.update { it.copy(availability = EqualizerAvailability.Unsupported) }
            return
        }

        try {
            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange.let { it[0].toInt()..it[1].toInt() }
            // getCenterFreq is in milliHertz.
            val centers = (0 until bandCount).map { eq.getCenterFreq(it.toShort()) / 1000 }
            val presets = (0 until eq.numberOfPresets.toInt()).map { eq.getPresetName(it.toShort()) }

            val fitted = saved.fittedTo(bandCount, range, presets.size)
            val levels = if (fitted.presetIndex != CUSTOM_PRESET) {
                eq.usePreset(fitted.presetIndex.toShort())
                readBandLevels(eq) ?: fitted.bandLevels
            } else {
                fitted.bandLevels.forEachIndexed { band, level ->
                    eq.setBandLevel(band.toShort(), level.toShort())
                }
                fitted.bandLevels
            }
            eq.enabled = fitted.enabled
            eq.setControlStatusListener { _, controlGranted ->
                _state.update { it.copy(hasControl = controlGranted) }
            }

            val bb = createBassBoost(sessionId, fitted)

            equalizer = eq
            bassBoost = bb
            attachedSessionId = sessionId
            val applied = fitted.copy(bandLevels = levels)
            settings = applied
            _state.update {
                it.copy(
                    availability = EqualizerAvailability.Ready,
                    levelRange = range,
                    presets = presets,
                    bassBoostSupported = bb != null,
                    hasControl = eq.hasControl(),
                ).withSettings(applied, centers)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Equalizer setup failed on session $sessionId", e)
            runCatching { eq.release() }
            _state.update { it.copy(availability = EqualizerAvailability.Unsupported) }
        }
    }

    /** Null when the device has no bass boost, or has one whose strength can't be set. */
    private fun createBassBoost(sessionId: Int, settings: EqualizerSettings): BassBoost? {
        val bb = try {
            BassBoost(EFFECT_PRIORITY, sessionId)
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost unavailable on session $sessionId", e)
            return null
        }
        return try {
            if (!bb.strengthSupported) {
                bb.release()
                null
            } else {
                bb.setStrength(settings.bassBoostStrength.toShort())
                bb.enabled = settings.enabled
                bb
            }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost setup failed on session $sessionId", e)
            runCatching { bb.release() }
            null
        }
    }

    private fun releaseEffects() {
        equalizer?.let { runCatching { it.release() } }
        bassBoost?.let { runCatching { it.release() } }
        equalizer = null
        bassBoost = null
        attachedSessionId = NO_SESSION
    }

    private fun readBandLevels(eq: Equalizer): List<Int>? = runCatching {
        (0 until eq.numberOfBands.toInt()).map { eq.getBandLevel(it.toShort()).toInt() }
    }.getOrNull()

    private fun applyEnabled(enabled: Boolean) {
        equalizer?.let { guard("setEnabled") { it.enabled = enabled } }
        bassBoost?.let { guard("setEnabled") { it.enabled = enabled } }
    }

    /**
     * Touching a control while the equalizer is off turns it on - otherwise the slider moves and
     * nothing audible happens, which reads as broken.
     */
    private fun turnOnIfOff(current: EqualizerSettings) {
        if (current.enabled) return
        applyEnabled(true)
        settings = current.copy(enabled = true)
    }

    private fun commit(updated: EqualizerSettings) {
        settings = updated
        val centers = _state.value.bands.map { it.centerHz }
        _state.update { it.withSettings(updated, centers) }
        schedulePersist(updated)
    }

    private fun EqualizerState.withSettings(s: EqualizerSettings, centers: List<Int>) = copy(
        enabled = s.enabled,
        bands = centers.mapIndexed { i, hz -> EqualizerBand(hz, s.bandLevels.getOrElse(i) { 0 }) },
        selectedPreset = s.presetIndex,
        bassBoostStrength = s.bassBoostStrength,
    )

    /**
     * Debounced: a slider drag calls this every frame, and only where it lands matters. Runs on
     * this singleton's own scope, so a write still pending when the service releases the effects
     * still completes.
     */
    private fun schedulePersist(s: EqualizerSettings) {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            withContext(Dispatchers.IO) {
                dataSaver.suspendSave(
                    EqualizerEnabled_KEY to s.enabled,
                    EqualizerPreset_KEY to s.presetIndex,
                    EqualizerBandLevels_KEY to EqualizerSettings.encodeBandLevels(s.bandLevels),
                    BassBoostStrength_KEY to s.bassBoostStrength,
                )
            }
        }
    }

    private suspend fun loadSettings(): EqualizerSettings = withContext(Dispatchers.IO) {
        EqualizerSettings(
            enabled = dataSaver.suspendGet(EqualizerEnabled_KEY, false),
            presetIndex = dataSaver.suspendGet(EqualizerPreset_KEY, CUSTOM_PRESET),
            bandLevels = EqualizerSettings.decodeBandLevels(
                dataSaver.suspendGet(EqualizerBandLevels_KEY, "")
            ),
            bassBoostStrength = dataSaver.suspendGet(BassBoostStrength_KEY, 0),
        )
    }

    private inline fun guard(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            // Typically "control lost" to a higher-priority effect - the value is still kept in
            // settings and re-applied on the next attach.
            Log.w(TAG, "$what failed", e)
        }
    }

    private companion object {
        const val TAG = "EqualizerController"
        /** Same value as `C.AUDIO_SESSION_ID_UNSET`, and the global output mix to audiofx. */
        const val NO_SESSION = 0
        /** Normal priority: a system enhancer that asks for more keeps control (see hasControl). */
        const val EFFECT_PRIORITY = 0
        const val PERSIST_DEBOUNCE_MS = 400L
    }
}
