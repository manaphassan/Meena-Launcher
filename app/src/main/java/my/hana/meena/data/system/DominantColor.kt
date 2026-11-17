package my.hana.meena.data.system

import android.graphics.Bitmap

/**
 * Extracts the single most prominent non-neutral colour from an icon bitmap.
 * Uses fast pixel quantisation — no external library needed.
 * Returns an ARGB [Int] (0 when no usable colour is found).
 */
object DominantColor {

    /** Sample at most this many pixels to keep the extraction fast. */
    private const val MAX_SAMPLE = 2500

    fun extract(bitmap: Bitmap): Int {
        val w = bitmap.width
        val h = bitmap.height
        val step = maxOf(1, (w * h) / MAX_SAMPLE)
        val buckets = HashMap<Int, Int>(64)

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var i = 0
        while (i < pixels.size) {
            val px = pixels[i]
            i += step
            val a = (px ushr 24) and 0xFF
            if (a < 128) continue
            val r = (px ushr 16) and 0xFF
            val g = (px ushr 8) and 0xFF
            val b = px and 0xFF
            // Skip near-white, near-black and low-saturation greys.
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            if (max < 35 || min > 220) continue
            if (max - min < 25) continue // grey
            // Quantise to 5-bit per channel so similar shades land in the same bucket.
            val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
            buckets[key] = (buckets[key] ?: 0) + 1
        }

        val winner = buckets.maxByOrNull { it.value }?.key ?: return 0
        val rr = ((winner ushr 10) and 0x1F) * 8 + 4
        val gg = ((winner ushr 5) and 0x1F) * 8 + 4
        val bb = (winner and 0x1F) * 8 + 4
        return (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
    }
}
