package my.hana.meena.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.pow

/**
 * How dark the wallpaper overlay has to be so the faintest text still reads at [TARGET_RATIO]
 * over the brightest part of the photo. The person's dim setting is the minimum; bright photos
 * get darker on their own (owner, 20 Nov 2026). Pure maths, so it can be unit tested.
 */
object WallpaperDim {
    const val MIN_PERCENT = 40
    const val MAX_PERCENT = 90
    const val TARGET_RATIO = 4.5f

    /**
     * Share of the visible photo that must pass. The brightest 1% (a glint or a speck of sky)
     * is let off, so one small highlight doesn't black out the whole picture.
     */
    const val COVERAGE = 0.99

    /**
     * The dim, in percent, from [minPercent] up to [MAX_PERCENT], at which every colour in [text]
     * reaches [TARGET_RATIO] over [COVERAGE] of [pixels] (ARGB). [MAX_PERCENT] if even that isn't enough.
     */
    fun dimFor(pixels: IntArray, text: List<Color>, minPercent: Int): Int {
        val start = minPercent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        if (pixels.isEmpty() || text.isEmpty()) return start
        val limit = text.minOf { maxBackgroundLuminance(it) }
        if (limit <= 0.0) return MAX_PERCENT
        if (passes(pixels, start, limit)) return start
        // Darker never makes the photo brighter, so the first passing percent can be found by halving.
        var low = start
        var high = MAX_PERCENT
        while (high - low > 1) {
            val mid = (low + high) / 2
            if (passes(pixels, mid, limit)) high = mid else low = mid
        }
        return high
    }

    /** The most a background may light up under [text] and still give [ratio]: (Lt + 0.05) / ratio - 0.05. */
    fun maxBackgroundLuminance(text: Color, ratio: Float = TARGET_RATIO): Double =
        (luminance(text.red.toDouble(), text.green.toDouble(), text.blue.toDouble()) + 0.05) / ratio - 0.05

    /**
     * Relative luminance at [COVERAGE] of [pixels] under a black overlay of [percent]%.
     * Compose draws the overlay in sRGB, so each channel is simply scaled by (1 - percent / 100).
     */
    fun dimmedLuminance(pixels: IntArray, percent: Int, coverage: Double = COVERAGE): Double {
        if (pixels.isEmpty()) return 0.0
        val keep = 1.0 - percent / 100.0
        val values = DoubleArray(pixels.size) { i ->
            val p = pixels[i]
            luminance(
                ((p shr 16) and 0xFF) / 255.0 * keep,
                ((p shr 8) and 0xFF) / 255.0 * keep,
                (p and 0xFF) / 255.0 * keep
            )
        }
        values.sort()
        return values[((values.size - 1) * coverage).toInt()]
    }

    private fun passes(pixels: IntArray, percent: Int, limit: Double) = dimmedLuminance(pixels, percent) <= limit

    private fun channel(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

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
    val small = android.graphics.Bitmap.createScaledBitmap(crop, w, h, true)
    val pixels = IntArray(w * h)
    small.getPixels(pixels, 0, w, 0, 0, w, h)
    if (small !== crop && small !== bitmap) small.recycle()
    if (crop !== bitmap) crop.recycle()
    return pixels
}
