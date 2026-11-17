package my.hana.meena

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import my.hana.meena.data.system.BarcodeDecoder
import my.hana.meena.data.system.ScannerRepository
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

class BarcodeDecoderTest {

    private val white = 0xFF.toByte()
    private val black = 0x00.toByte()

    /** A sensor-style Y plane: [width] x [height] pixels with [rowStride] bytes per row. */
    private fun frame(width: Int, height: Int, rowStride: Int = width, fill: Byte = white) =
        ByteArray(rowStride * height) { fill }

    private fun stamp(plane: ByteArray, rowStride: Int, matrix: BitMatrix, left: Int, top: Int, inverted: Boolean = false) {
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            val dark = matrix[x, y] != inverted
            plane[(top + y) * rowStride + left + x] = if (dark) black else white
        }
    }

    /** Turns a matrix a quarter turn anticlockwise, as the sensor sees a level code when the phone is upright. */
    private fun quarterTurnAnticlockwise(m: BitMatrix): BitMatrix {
        val out = BitMatrix(m.height, m.width)
        for (y in 0 until m.height) for (x in 0 until m.width) if (m[x, y]) out.set(y, m.width - 1 - x)
        return out
    }

    private fun decode(plane: ByteArray, width: Int, height: Int, rowStride: Int, rotation: Int) =
        BarcodeDecoder().decode(ByteBuffer.wrap(plane), width, height, rowStride, rotation)

    @Test
    fun qrCode_isRead_fromPaddedFrame() {
        val qr = MultiFormatWriter().encode("https://example.com/meena", BarcodeFormat.QR_CODE, 240, 240)
        val stride = 704 // wider than 640, like real camera buffers
        val plane = frame(640, 480, stride)
        stamp(plane, stride, qr, left = 200, top = 120)

        val result = decode(plane, 640, 480, stride, rotation = 90)

        assertEquals("https://example.com/meena", result?.content)
        assertEquals(BarcodeFormat.QR_CODE, result?.format)
    }

    @Test
    fun invertedQrCode_isRead() {
        val qr = MultiFormatWriter().encode("light on dark", BarcodeFormat.QR_CODE, 240, 240)
        val plane = frame(640, 480, fill = black)
        stamp(plane, 640, qr, left = 200, top = 120, inverted = true)

        assertEquals("light on dark", decode(plane, 640, 480, 640, rotation = 0)?.content)
    }

    @Test
    fun levelBarcode_isRead_whenTheSensorSeesItSideways() {
        // Sensor frames are landscape; with the phone upright, a level barcode appears vertical in them.
        val code = MultiFormatWriter().encode("MEENA-0200", BarcodeFormat.CODE_128, 360, 80)
        val sideways = quarterTurnAnticlockwise(code)
        val plane = frame(640, 480)
        stamp(plane, 640, sideways, left = (640 - sideways.width) / 2, top = (480 - sideways.height) / 2)

        val result = decode(plane, 640, 480, 640, rotation = 90)

        assertEquals("MEENA-0200", result?.content)
        assertEquals(BarcodeFormat.CODE_128, result?.format)
    }

    @Test
    fun emptyFrame_readsNothing() {
        assertNull(decode(frame(640, 480), 640, 480, 640, rotation = 90))
    }

    @Test
    fun codeOutsideTheCentreSquare_isIgnored() {
        val qr = MultiFormatWriter().encode("edge", BarcodeFormat.QR_CODE, 70, 70)
        val plane = frame(640, 480)
        stamp(plane, 640, qr, left = 0, top = 0) // the centre square starts at x = 80

        assertNull(decode(plane, 640, 480, 640, rotation = 0))
    }

    @Test
    fun rotateSquare_turnsClockwise() {
        // 1 2
        // 3 4
        val src = byteArrayOf(1, 2, 3, 4)
        assertArrayEquals(byteArrayOf(3, 1, 4, 2), BarcodeDecoder.rotateSquare(src, 2, 90, ByteArray(4)))
        assertArrayEquals(byteArrayOf(4, 3, 2, 1), BarcodeDecoder.rotateSquare(src, 2, 180, ByteArray(4)))
        assertArrayEquals(byteArrayOf(2, 4, 1, 3), BarcodeDecoder.rotateSquare(src, 2, 270, ByteArray(4)))
        assertArrayEquals(src, BarcodeDecoder.rotateSquare(src, 2, 0, ByteArray(4)))
    }

    @Test
    fun formatNames_areShortAndLowercase() {
        assertEquals("qr", ScannerRepository.formatName(BarcodeFormat.QR_CODE))
        assertEquals("ean-13", ScannerRepository.formatName(BarcodeFormat.EAN_13))
        assertEquals("data matrix", ScannerRepository.formatName(BarcodeFormat.DATA_MATRIX))
        assertEquals("barcode", ScannerRepository.formatName(BarcodeFormat.MAXICODE))
        for (format in BarcodeDecoder.FORMATS) {
            val name = ScannerRepository.formatName(format)
            assertEquals(name.lowercase(), name)
        }
    }
}
