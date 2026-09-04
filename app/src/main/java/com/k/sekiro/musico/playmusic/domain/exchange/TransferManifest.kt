package com.k.sekiro.musico.playmusic.domain.exchange

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Manifest for the audio-transfer flow ([SongFingerprint]'s superset): carries enough per-track
 * metadata for the receiver to both fuzzy-match ([SongKey]) and, for tracks it's missing, verify
 * and stream the actual bytes ([TransferSong.sha256] / [TransferSong.sizeBytes]).
 *
 * Short [SerialName] keys match [PlaylistExport]'s style. [version] is this manifest's own
 * schema version, unrelated to [PlaylistExport.version].
 */
@Serializable
data class TransferManifest(
    @SerialName("n") val name: String,
    @SerialName("s") val songs: List<TransferSong>,
    @SerialName("v") val version: Int = 2,
)

@Serializable
data class TransferSong(
    @SerialName("t") override val title: String,
    @SerialName("a") override val artist: String,
    @SerialName("al") override val album: String,
    @SerialName("d") override val durationMs: Long,
    @SerialName("sz") val sizeBytes: Long,
    @SerialName("m") val mime: String,
    @SerialName("h") val sha256: String,
    @SerialName("fn") val fileName: String,
) : SongKey
