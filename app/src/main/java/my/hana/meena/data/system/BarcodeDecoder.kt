package my.hana.meena.data.system

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * Reads QR codes and barcodes from a camera frame's brightness (Y) plane with ZXing.
 * Everything runs on the phone; ZXing has no network code.
 *
 * Not thread-safe: use one instance per camera session, from a single analyzer thread.
 */
class BarcodeDecoder {

    data class Result(val content: String, val format: BarcodeFormat)

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to FORMATS,
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.ALSO_INVERTED to true
            )
        )
    }

    private var square = ByteArray(0)
    private var upright = ByteArray(0)

    /**
     * [plane] is the Y plane of the frame as the sensor sees it, [rowStride] bytes per row.
     * Only the centre square (the short side of the frame) is read, turned upright by
     * [rotationDegrees] so barcodes held level in the viewfinder are level here too.
     */
    fun decode(plane: ByteBuffer, width: Int, height: Int, rowStride: Int, rotationDegrees: Int): Result? {
        val side = minOf(width, height)
        if (side <= 0) return null
        val size = side * side
        if (square.size != size) {
            square = ByteArray(size)
            upright = ByteArray(size)
        }
        cropCentreSquare(plane, width, height, rowStride, side, square)
        val pixels = rotateSquare(square, side, rotationDegrees, upright)

        val source = PlanarYUVLuminanceSource(pixels, side, side, 0, 0, side, side, false)
        // The local-contrast binarizer suits most frames; the global one rescues some soft or small codes.
        return tryDecode(BinaryBitmap(HybridBinarizer(source)))
            ?: tryDecode(BinaryBitmap(GlobalHistogramBinarizer(source)))
    }

    private fun tryDecode(bitmap: BinaryBitmap): Result? = try {
        val found = reader.decodeWithState(bitmap)
        found.text?.takeIf { it.isNotBlank() }?.let { Result(it, found.barcodeFormat) }
    } catch (_: ReaderException) {
        null
    } finally {
        reader.reset()
    }

    companion object {
        val FORMATS: List<BarcodeFormat> = listOf(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.AZTEC,
            BarcodeFormat.PDF_417,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.ITF,
            BarcodeFormat.CODABAR
        )

        internal fun cropCentreSquare(
            plane: ByteBuffer, width: Int, height: Int, rowStride: Int, side: Int, out: ByteArray
        ) {
            val left = (width - side) / 2
            val top = (height - side) / 2
            val src = plane.duplicate()
            for (row in 0 until side) {
                src.position((top + row) * rowStride + left)
                src.get(out, row * side, side)
            }
        }

        /** Turns a square image clockwise by [degrees] (0, 90, 180 or 270). */
        internal fun rotateSquare(src: ByteArray, n: Int, degrees: Int, out: ByteArray): ByteArray {
            when (((degrees % 360) + 360) % 360) {
                90 -> for (y in 0 until n) for (x in 0 until n) out[x * n + (n - 1 - y)] = src[y * n + x]
                180 -> for (y in 0 until n) for (x in 0 until n) out[(n - 1 - y) * n + (n - 1 - x)] = src[y * n + x]
                270 -> for (y in 0 until n) for (x in 0 until n) out[(n - 1 - x) * n + y] = src[y * n + x]
                else -> return src
            }
            return out
        }
    }
}
