package com.k.sekiro.musico.playmusic.data.exchange

import android.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * The small pairing payload carried by the "send with songs" QR: Wi-Fi credentials for the
 * private link the sender stood up, plus enough to fetch and verify the real
 * `TransferManifest` over it.
 */
@Serializable
data class TransferPairingPayload(
    @SerialName("ssid") val ssid: String,
    @SerialName("pass") val pass: String,
    @SerialName("host") val host: String,
    @SerialName("port") val port: Int,
    /** One-time bearer token, checked on every request against [com.k.sekiro.musico.playmusic.data.exchange.TransferHttpServer]. */
    @SerialName("tok") val tok: String,
    /** sha256 of the `TransferManifest` JSON - the receiver rejects a manifest that doesn't match. */
    @SerialName("man") val man: String,
    @SerialName("n") val trackCount: Int,
    @SerialName("bytes") val totalBytes: Long,
)

/**
 * Encodes/decodes a [TransferPairingPayload] to and from the string carried by the
 * `MUSICO-XFER-1:` QR. Wire format: `"MUSICO-XFER-1:" + base64(deflate(utf8(json)))`, the same
 * shape as `PlaylistQrCodec`'s `MUSICO1:` format but a separate marker/namespace, so the two
 * payload kinds can be told apart before either is decoded (see [isTransferPayload]).
 *
 * `deflate`/`inflate` are duplicated from `PlaylistQrCodec` rather than shared across packages -
 * both are ~15-line, stable, self-contained helpers, and widening `PlaylistQrCodec`'s `internal`
 * visibility on already-shipped code isn't worth it for this.
 */
object TransferPairingCodec {

    private const val MARKER = "MUSICO-XFER-1:"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** @return `true` if [raw] looks like a transfer-pairing QR, before attempting to [decode] it. */
    fun isTransferPayload(raw: String): Boolean = raw.trim().startsWith(MARKER)

    fun encode(payload: TransferPairingPayload): String {
        val raw = json.encodeToString(TransferPairingPayload.serializer(), payload).toByteArray(Charsets.UTF_8)
        val compressed = deflate(raw)
        return MARKER + Base64.encodeToString(compressed, Base64.NO_WRAP)
    }

    /** @return the decoded payload, or `null` if [raw] is not a recognizable/valid pairing QR. */
    fun decode(raw: String): TransferPairingPayload? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(MARKER)) return null
        return try {
            val compressed = Base64.decode(trimmed.removePrefix(MARKER), Base64.NO_WRAP)
            val jsonText = inflate(compressed).toString(Charsets.UTF_8)
            json.decodeFromString(TransferPairingPayload.serializer(), jsonText)
        } catch (_: Exception) {
            null
        }
    }

    internal fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION).apply {
            setInput(data)
            finish()
        }
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()
        return out.toByteArray()
    }

    internal fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater().apply { setInput(data) }
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0 && inflater.needsInput()) break
            out.write(buffer, 0, n)
        }
        inflater.end()
        return out.toByteArray()
    }
}
