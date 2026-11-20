package my.hana.meena.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.pow

/**
 * How strong the wallpaper overlay has to be so the faintest text still reads at [TARGET_RATIO]
 * over the photo. Dark pages put light text on a black overlay, judged against the brightest part
 * of the photo; light and paper pages put dark text on a white overlay, judged against the darkest
 * part. The person's dim setting is the minimum; the overlay gets stronger on its own when needed
 * (owner, 20 Nov 2026). Pure maths, so it can be unit tested.
 */
object WallpaperDim {
    const val MIN_PERCENT = 40
    const val MAX_PERCENT = 90
    const val TARGET_RATIO = 4.5f

    /**
     * Share of the visible photo that must pass. The most extreme 1% (a glint, a speck of sky, a
     * deep shadow) is let off, so one small spot doesn't wash out the whole picture.
     */
    const val COVERAGE = 0.99

    /**
     * The overlay, in percent, from [minPercent] up to [MAX_PERCENT], at which every colour in [text]
     * reaches [TARGET_RATIO] over [COVERAGE] of [pixels] (ARGB). [lightOverlay] is true for a white
     * overlay under dark text. [MAX_PERCENT] if even that isn't enough.
     */
    fun dimFor(pixels: IntArray, text: List<Color>, minPercent: Int, lightOverlay: Boolean = false): Int {
        val start = minPercent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        if (pixels.isEmpty() || text.isEmpty()) return start
        val passes: (Int) -> Boolean = if (lightOverlay) {
            val limit = text.maxOf { minBackgroundLuminance(it) }
            if (limit >= 1.0) return MAX_PERCENT
            { percent -> worstLuminance(pixels, percent, lightOverlay = true) >= limit }
        } else {
            val limit = text.minOf { maxBackgroundLuminance(it) }
            if (limit <= 0.0) return MAX_PERCENT
            { percent -> worstLuminance(pixels, percent, lightOverlay = false) <= limit }
        }
        if (passes(start)) return start
        // A stronger overlay only moves the photo further from the text, so halving finds the first pass.
        var low = start
        var high = MAX_PERCENT
        while (high - low > 1) {
            val mid = (low + high) / 2
            if (passes(mid)) high = mid else low = mid
        }
        return high
    }

    /** The most a background may light up under light [text] and still give [ratio]: (Lt + 0.05) / ratio - 0.05. */
    fun maxBackgroundLuminance(text: Color, ratio: Float = TARGET_RATIO): Double =
        (luminance(text) + 0.05) / ratio - 0.05

    /** The least a background must light up under dark [text] to give [ratio]: ratio x (Lt + 0.05) - 0.05. */
    fun minBackgroundLuminance(text: Color, ratio: Float = TARGET_RATIO): Double =
        ratio * (luminance(text) + 0.05) - 0.05

    /**
     * Relative luminance of [pixels] under a black (or, with [lightOverlay], white) overlay of
     * [percent]%, at the side that is hardest to read on: the brightest [COVERAGE] mark under a
     * black overlay, the darkest under a white one. Compose blends the overlay in sRGB, so each
     * channel moves [percent]% of the way to black or white.
     */
    fun worstLuminance(pixels: IntArray, percent: Int, lightOverlay: Boolean, coverage: Double = COVERAGE): Double {
        if (pixels.isEmpty()) return 0.0
        val keep = 1.0 - percent / 100.0
        val add = if (lightOverlay) percent / 100.0 else 0.0
        val values = DoubleArray(pixels.size) { i ->
            val p = pixels[i]
            luminance(
                ((p shr 16) and 0xFF) / 255.0 * keep + add,
                ((p shr 8) and 0xFF) / 255.0 * keep + add,
                (p and 0xFF) / 255.0 * keep + add
            )
        }
        values.sort()
        val share = if (lightOverlay) 1.0 - coverage else coverage
        return values[((values.size - 1) * share).toInt()]
    }

    private fun channel(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color): Double = luminance(c.red.toDouble(), c.green.toDouble(), c.blue.toDouble())

    private fun luminance(r: Double, g: Double, b: Double): Double =
        0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
}

/**
 * The part of [bitmap] the home screen shows (ContentScale.Crop, centred on a [screenWidth] x
 * [screenHeight] screen), shrunk to about 72 pixels wide, as ARGB pixels for [WallpaperDim].
 */
fun wallpaperDimSample(bitmap: android.graphics.Bitmap, screenWidth: Int, screenHeight: Int): IntArray {
    val bw = bitmap.width
    val bh = bitmap.height
    if (bw <= 0 || bh <= 0 || screenWidth <= 0 || screenHeight <= 0) return IntArray(0)
    val scale = max(screenWidth.toFloat() / bw, screenHeight.toFloat() / bh)
    val cw = (screenWidth / scale).toInt().coerceIn(1, bw)
    val ch = (screenHeight / scale).toInt().coerceIn(1, bh)
    val crop = android.graphics.Bitmap.createBitmap(bitmap, (bw - cw) / 2, (bh - ch) / 2, cw, ch)
    val w = 72
    val h = (w.toLong() * ch / cw).toInt().coerceIn(1, 400)
    val small = crop.scale(w, h)
    val pixels = IntArray(w * h)
    small.getPixels(pixels, 0, w, 0, 0, w, h)
    if (small !== crop && small !== bitmap) small.recycle()
    if (crop !== bitmap) crop.recycle()
    return pixels
}
