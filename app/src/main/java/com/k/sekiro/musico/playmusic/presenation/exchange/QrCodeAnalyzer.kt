package com.k.sekiro.musico.playmusic.presenation.exchange

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * CameraX analyzer that decodes QR codes off the luma plane of each frame. Fires [onDecoded] at
 * most once - after the first successful decode it ignores every subsequent frame, so the caller
 * doesn't have to race to unbind the camera.
 */
class QrCodeAnalyzer(
    private val onDecoded: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()
    private val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    )

    @Volatile
    private var done = false

    override fun analyze(image: ImageProxy) {
        if (done) {
            image.close()
            return
        }
        try {
            val plane = image.planes.firstOrNull() ?: return
            val data = ByteArray(plane.buffer.remaining())
            plane.buffer.get(data)

            val rowStride = plane.rowStride.coerceAtLeast(1)
            val dataHeight = data.size / rowStride
            if (dataHeight == 0) return

            val source = PlanarYUVLuminanceSource(
                data,
                rowStride,
                dataHeight,
                0,
                0,
                minOf(image.width, rowStride),
                minOf(image.height, dataHeight),
                false,
            )
            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)
            done = true
            onDecoded(result.text)
        } catch (_: NotFoundException) {
            // no QR in this frame - normal
        } catch (_: Exception) {
            // malformed frame / decoder hiccup - skip it
        } finally {
            image.close()
        }
    }
}
