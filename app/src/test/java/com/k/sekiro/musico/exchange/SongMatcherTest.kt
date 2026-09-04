package com.k.sekiro.musico.exchange

import com.k.sekiro.musico.playmusic.domain.exchange.PlaylistExport
import com.k.sekiro.musico.playmusic.domain.exchange.SongFingerprint
import com.k.sekiro.musico.playmusic.domain.exchange.SongMatcher
import com.k.sekiro.musico.playmusic.domain.exchange.TransferManifest
import com.k.sekiro.musico.playmusic.domain.exchange.TransferSong
import com.k.sekiro.musico.playmusic.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongMatcherTest {

    private fun song(
        title: String,
        artist: String,
        album: String,
        duration: Long,
        id: Long,
    ) = Song(
        name = "$title.mp3",
        title = title,
        artist = artist,
        album = album,
        path = "/music/$id.mp3",
        duration = duration,
        id = id,
    )

    private fun fp(title: String, artist: String, album: String, durationMs: Long) =
        SongFingerprint(title, artist, album, durationMs)

    private fun transferSong(
        title: String,
        artist: String = "Artist",
        album: String = "Album",
        durationMs: Long = 200_000,
        sha256: String = "",
    ) = TransferSong(
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        sizeBytes = 1_000,
        mime = "audio/mpeg",
        sha256 = sha256,
        fileName = "$title.mp3",
    )

    @Test
    fun `exact metadata plus duration within tolerance matches`() {
        val library = listOf(song("Song A", "Artist", "Album", 200_000, 1))
        val match = SongMatcher.match(fp("Song A", "Artist", "Album", 201_500), library)
        assertEquals(1L, match?.id)
    }

    @Test
    fun `duration outside every tolerance does not match`() {
        val library = listOf(song("Song A", "Artist", "Album", 200_000, 1))
        assertNull(SongMatcher.match(fp("Song A", "Artist", "Album", 260_000), library))
    }

    @Test
    fun `case and punctuation and whitespace are normalized`() {
        val library = listOf(song("Sunrise (Remix)", "The  Band", "OST", 180_000, 7))
        val match = SongMatcher.match(fp("sunrise remix", "the band", "ost", 180_400), library)
        assertEquals(7L, match?.id)
    }

    @Test
    fun `album mismatch still matches on title plus artist within wider tolerance`() {
        val library = listOf(song("Track", "Artist", "Deluxe Edition", 240_000, 3))
        val match = SongMatcher.match(fp("Track", "Artist", "Standard Edition", 242_000), library)
        assertEquals(3L, match?.id)
    }

    @Test
    fun `title only tier matches when artist and album differ`() {
        val library = listOf(song("Unique Title", "Real Artist", "Real Album", 150_000, 9))
        val match = SongMatcher.match(fp("Unique Title", "", "", 150_800), library)
        assertEquals(9L, match?.id)
    }

    @Test
    fun `matchAll splits matched and unmatched and preserves order`() {
        val library = listOf(
            song("One", "A", "X", 100_000, 1),
            song("Three", "C", "Z", 300_000, 3),
        )
        val export = PlaylistExport(
            name = "Mix",
            songs = listOf(
                fp("One", "A", "X", 100_000),
                fp("Two", "B", "Y", 200_000),
                fp("Three", "C", "Z", 300_000),
            ),
        )
        val result = SongMatcher.matchAll(export, library)
        assertEquals("Mix", result.playlistName)
        assertEquals(listOf(1L, 3L), result.matched.map { it.id })
        assertEquals(listOf("Two"), result.unmatched.map { it.title })
    }

    @Test
    fun `matchAll de-duplicates when two fingerprints resolve to the same song`() {
        val library = listOf(song("Dup", "A", "X", 100_000, 1))
        val export = PlaylistExport(
            name = "Mix",
            songs = listOf(fp("Dup", "A", "X", 100_000), fp("Dup", "A", "X", 100_200)),
        )
        val result = SongMatcher.matchAll(export, library)
        assertEquals(1, result.matched.size)
        assertTrue(result.unmatched.isEmpty())
    }

    @Test
    fun `transfer matchAll uses sha256 tier 0 even when metadata differs`() {
        val library = listOf(song("Totally Different Title", "Someone Else", "Other Album", 999_000, 5))
        val manifest = TransferManifest(
            name = "Mix",
            songs = listOf(transferSong("Original Title", sha256 = "deadbeef")),
        )
        val result = SongMatcher.matchAll(manifest, library, libraryHashes = mapOf(5L to "deadbeef"))
        assertEquals(listOf(5L), result.matched.map { it.id })
        assertTrue(result.unmatched.isEmpty())
    }

    @Test
    fun `transfer matchAll falls back to fuzzy tiers when hash is unknown`() {
        val library = listOf(song("Song A", "Artist", "Album", 200_000, 1))
        val manifest = TransferManifest(
            name = "Mix",
            songs = listOf(transferSong("Song A", "Artist", "Album", 200_500, sha256 = "unknown-hash")),
        )
        val result = SongMatcher.matchAll(manifest, library)
        assertEquals(listOf(1L), result.matched.map { it.id })
    }

    @Test
    fun `transfer matchAll reports unmatched when neither hash nor fuzzy tiers hit`() {
        val library = listOf(song("Song A", "Artist", "Album", 200_000, 1))
        val manifest = TransferManifest(
            name = "Mix",
            songs = listOf(transferSong("Completely Unrelated", "Nobody", "Nothing", 50_000, sha256 = "x")),
        )
        val result = SongMatcher.matchAll(manifest, library)
        assertTrue(result.matched.isEmpty())
        assertEquals(1, result.unmatched.size)
    }
}
