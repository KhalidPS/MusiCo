package com.k.sekiro.musico.playmusic.domain.exchange

/**
 * The fields [SongMatcher] fuzzy-matches on, shared by [SongFingerprint] (QR-only exchange) and
 * [TransferSong] (audio-transfer manifest) so matching logic works over either payload type.
 */
interface SongKey {
    val title: String
    val artist: String
    val album: String
    val durationMs: Long
}
