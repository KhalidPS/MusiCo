package com.k.sekiro.musico.exchange

import com.k.sekiro.musico.playmusic.presenation.exchange.PlaylistQrCodec
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the deflate/inflate leg of the QR wire format (the Base64 + `QRCodeWriter` layers are
 * Android-only and symmetric; they're exercised on-device).
 */
class PlaylistQrCodecTest {

    @Test
    fun `deflate then inflate round-trips arbitrary text`() {
        val original = """
            {"n":"Road Trip","s":[
              {"t":"Twirl Away","a":"Dr. MAD","al":"Music","d":4000},
              {"t":"Club Cubano","a":"Dr. Mad","al":"Music","d":4000}
            ],"v":1}
        """.trimIndent()
        val bytes = original.toByteArray(Charsets.UTF_8)
        val restored = PlaylistQrCodec.inflate(PlaylistQrCodec.deflate(bytes)).toString(Charsets.UTF_8)
        assertEquals(original, restored)
    }

    @Test
    fun `deflate actually shrinks a repetitive payload`() {
        val repetitive = "{\"t\":\"same\",\"a\":\"same\",\"al\":\"same\"}".repeat(40)
        val bytes = repetitive.toByteArray(Charsets.UTF_8)
        val compressed = PlaylistQrCodec.deflate(bytes)
        assert(compressed.size < bytes.size) {
            "expected compression, got ${compressed.size} >= ${bytes.size}"
        }
    }

    @Test
    fun `empty input round-trips`() {
        val restored = PlaylistQrCodec.inflate(PlaylistQrCodec.deflate(ByteArray(0)))
        assertEquals(0, restored.size)
    }
}
