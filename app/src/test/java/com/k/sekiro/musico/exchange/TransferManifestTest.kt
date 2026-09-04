package com.k.sekiro.musico.exchange

import com.k.sekiro.musico.playmusic.domain.exchange.TransferManifest
import com.k.sekiro.musico.playmusic.domain.exchange.TransferSong
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferManifestTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun transferSong(title: String, sha256: String) = TransferSong(
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 200_000,
        sizeBytes = 4_500_000,
        mime = "audio/mpeg",
        sha256 = sha256,
        fileName = "$title.mp3",
    )

    @Test
    fun `manifest round-trips through json`() {
        val original = TransferManifest(
            name = "Road Trip",
            songs = listOf(transferSong("Twirl Away", "abc123"), transferSong("Club Cubano", "def456")),
        )
        val restored = json.decodeFromString(TransferManifest.serializer(), json.encodeToString(TransferManifest.serializer(), original))
        assertEquals(original, restored)
    }

    @Test
    fun `default version is 2`() {
        val manifest = TransferManifest(name = "Mix", songs = emptyList())
        assertEquals(2, manifest.version)
    }

    @Test
    fun `short serial names keep the payload compact`() {
        val encoded = json.encodeToString(TransferManifest.serializer(), TransferManifest(name = "Mix", songs = listOf(transferSong("Song", "hash"))))
        assert(encoded.contains("\"t\":\"Song\"")) { "expected short key \"t\", got: $encoded" }
        assert(!encoded.contains("\"title\"")) { "expected short key, not the field name: $encoded" }
    }
}
