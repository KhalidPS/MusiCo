package com.k.sekiro.musico.playmusic.domain.exchange

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cross-device fingerprint of a single track. Deliberately excludes [com.k.sekiro.musico
 * .playmusic.domain.model.Song.id] and `path` - both are device-local (raw MediaStore `_ID` /
 * absolute filesystem path) and meaningless on the receiving device. The receiver re-resolves
 * each fingerprint against its own library via [SongMatcher].
 *
 * Short [SerialName] keys keep the encoded payload small enough to fit one QR code.
 */
@Serializable
data class SongFingerprint(
    @SerialName("t") override val title: String,
    @SerialName("a") override val artist: String,
    @SerialName("al") override val album: String,
    @SerialName("d") override val durationMs: Long,
) : SongKey

/**
 * The full exported-playlist payload. [version] is the payload-schema version (independent of the
 * transport marker prefix added by `PlaylistQrCodec`), so a future chunked / richer format can be
 * told apart from this one.
 */
@Serializable
data class PlaylistExport(
    @SerialName("n") val name: String,
    @SerialName("s") val songs: List<SongFingerprint>,
    @SerialName("v") val version: Int = 1,
)
