package com.k.sekiro.musico.exchange

import com.k.sekiro.musico.playmusic.data.exchange.TransferPairingCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the deflate/inflate leg and marker detection (the Base64 layer is Android-only and
 * symmetric with `PlaylistQrCodecTest`; exercised on-device).
 */
class TransferPairingCodecTest {

    @Test
    fun `deflate then inflate round-trips arbitrary text`() {
        val original = """{"ssid":"DIRECT-ab","pass":"s3cret!","host":"192.168.49.1","port":8988,"tok":"tok123","man":"deadbeef","n":12,"bytes":150000000}"""
        val bytes = original.toByteArray(Charsets.UTF_8)
        val restored = TransferPairingCodec.inflate(TransferPairingCodec.deflate(bytes)).toString(Charsets.UTF_8)
        assertEquals(original, restored)
    }

    @Test
    fun `empty input round-trips`() {
        val restored = TransferPairingCodec.inflate(TransferPairingCodec.deflate(ByteArray(0)))
        assertEquals(0, restored.size)
    }

    @Test
    fun `isTransferPayload recognizes the marker`() {
        assertTrue(TransferPairingCodec.isTransferPayload("MUSICO-XFER-1:abc"))
        assertTrue(TransferPairingCodec.isTransferPayload("  MUSICO-XFER-1:abc  "))
    }

    @Test
    fun `isTransferPayload rejects the QR-only marker and other text`() {
        assertFalse(TransferPairingCodec.isTransferPayload("MUSICO1:abc"))
        assertFalse(TransferPairingCodec.isTransferPayload("""{"n":"Mix"}"""))
        assertFalse(TransferPairingCodec.isTransferPayload(""))
    }

    @Test
    fun `decode rejects a string without the marker without throwing`() {
        assertNull(TransferPairingCodec.decode("not a transfer payload"))
        assertNull(TransferPairingCodec.decode("MUSICO1:abc"))
    }
}
