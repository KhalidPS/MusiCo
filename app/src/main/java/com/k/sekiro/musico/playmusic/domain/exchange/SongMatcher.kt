package com.k.sekiro.musico.playmusic.domain.exchange

import com.k.sekiro.musico.playmusic.domain.model.Song
import kotlin.math.abs

/**
 * Resolves the fingerprints in a [PlaylistExport] against the receiving device's library.
 *
 * `Song.id` / `path` can't be used across devices, so matching is done on normalized
 * title/artist/album plus a duration tolerance. Pure Kotlin (no Android deps) so it is
 * JVM-unit-testable.
 *
 * The roadmap suggested a `SongsRepository.findSong(...)` DAO method; this in-memory matcher over
 * the existing `SongsRepository.getSongsFromRoom()` snapshot is used instead - the tiered fuzzy
 * match can't be expressed as one SQL query and a per-song DAO round-trip for a whole playlist
 * would be wasteful.
 */
object SongMatcher {

    private val punctuation = Regex("[\\p{Punct}]")
    private val whitespace = Regex("\\s+")

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(punctuation, " ")
            .replace(whitespace, " ")
            .trim()

    /**
     * @return the first library [Song] matching [fingerprint], or `null`. Tiers, most strict first:
     *  1. title + artist + album all equal (normalized) and duration within 2s
     *  2. title + artist equal and duration within 3s (album ignored)
     *  3. title equal and duration within 1.5s (artist + album ignored)
     */
    fun match(fingerprint: SongFingerprint, library: List<Song>): Song? {
        val nTitle = normalize(fingerprint.title)
        val nArtist = normalize(fingerprint.artist)
        val nAlbum = normalize(fingerprint.album)
        val target = fingerprint.durationMs

        library.firstOrNull {
            normalize(it.title) == nTitle &&
                normalize(it.artist) == nArtist &&
                normalize(it.album) == nAlbum &&
                abs(it.duration - target) <= 2_000
        }?.let { return it }

        library.firstOrNull {
            normalize(it.title) == nTitle &&
                normalize(it.artist) == nArtist &&
                abs(it.duration - target) <= 3_000
        }?.let { return it }

        return library.firstOrNull {
            normalize(it.title) == nTitle && abs(it.duration - target) <= 1_500
        }
    }

    fun matchAll(export: PlaylistExport, library: List<Song>): MatchResult {
        val matched = mutableListOf<Song>()
        val matchedIds = HashSet<Long>()
        val unmatched = mutableListOf<SongFingerprint>()
        // Dedupe by Song.id explicitly - Song.equals() is unreliable (it casts to SongUi).
        for (fingerprint in export.songs) {
            val song = match(fingerprint, library)
            when {
                song == null -> unmatched.add(fingerprint)
                matchedIds.add(song.id) -> matched.add(song)
            }
        }
        return MatchResult(
            playlistName = export.name,
            matched = matched,
            unmatched = unmatched,
        )
    }
}

data class MatchResult(
    val playlistName: String,
    /** Library songs to add, in export order, de-duplicated. */
    val matched: List<Song>,
    /** Fingerprints with no counterpart in this library. */
    val unmatched: List<SongFingerprint>,
)
