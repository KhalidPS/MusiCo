package com.k.sekiro.musico.playmusic.presenation.exchange

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.k.sekiro.musico.playmusic.domain.exchange.PlaylistExport
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/** Raised when a [PlaylistExport] is too large to fit in a single QR code. */
class QrTooLargeException(cause: Throwable? = null) : Exception(cause)

/**
 * Encodes/decodes a [PlaylistExport] to and from the string carried by a QR code, and renders the
 * QR bitmap.
 *
 * Wire format: `"MUSICO1:" + base64(deflate(utf8(json)))`. The `MUSICO1:` marker is the transport
 * version - a future multi-frame format can claim `MUSICO2:` (e.g.
 * `MUSICO2:<index>/<total>:<base64chunk>`) without ambiguity, and [decode] already rejects
 * unknown markers cleanly.
 *
 * [decode] also accepts a bare JSON string so the "export/import as file" fallback (used when a
 * playlist is too big for one QR) can share this path.
 */
object PlaylistQrCodec {

    private const val MARKER_V1 = "MUSICO1:"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(export: PlaylistExport): String {
        val raw = json.encodeToString(PlaylistExport.serializer(), export).toByteArray(Charsets.UTF_8)
        val compressed = deflate(raw)
        return MARKER_V1 + Base64.encodeToString(compressed, Base64.NO_WRAP)
    }

    /** Plain, uncompressed JSON - used when writing the `.json` file fallback. */
    fun encodeJson(export: PlaylistExport): String =
        json.encodeToString(PlaylistExport.serializer(), export)

    /** @return the decoded payload, or `null` if [raw] is not a recognizable/valid export. */
    fun decode(raw: String): PlaylistExport? {
        val trimmed = raw.trim()
        return try {
            when {
                trimmed.startsWith(MARKER_V1) -> {
                    val compressed = Base64.decode(trimmed.removePrefix(MARKER_V1), Base64.NO_WRAP)
                    val jsonText = inflate(compressed).toString(Charsets.UTF_8)
                    json.decodeFromString(PlaylistExport.serializer(), jsonText)
                }

                trimmed.startsWith("{") ->
                    json.decodeFromString(PlaylistExport.serializer(), trimmed)

                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * @throws QrTooLargeException if [content] does not fit a QR code at the highest error
     * correction this method uses.
     */
    fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "ISO-8859-1",
        )
        val matrix = try {
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        } catch (e: WriterException) {
            throw QrTooLargeException(e)
        } catch (e: IllegalArgumentException) {
            // ZXing throws this when the data exceeds the largest QR version's capacity.
            throw QrTooLargeException(e)
        }
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
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
