package my.hana.meena.ui.navigation

import kotlin.math.roundToInt

/**
 * Calculates continuous horizontal scroll offsets and multi-speed parallax layers
 * for Meena's Windows Phone Metro Panorama layout.
 *
 * Implements:
 * 1. Title span calculations guaranteeing authentic right-edge peek affordance.
 * 2. Continuous real-time header sync with pager drag position.
 * 3. Multi-speed background canvas parallax for brand and background layers.
 */
object ParallaxHelper {

    /**
     * Calculates the horizontal canvas step distance (span) between consecutive titles
     * to guarantee that at page N, title N is at [startMarginPx] and title N+1 peeks at [screenWidthPx - peekWidthPx].
     */
    fun calculateTitleSpan(
        screenWidthPx: Int,
        startMarginPx: Int,
        peekWidthPx: Int
    ): Int {
        val span = screenWidthPx - peekWidthPx - startMarginPx
        return span.coerceAtLeast(startMarginPx * 2)
    }

    /**
     * Calculates the horizontal scroll offset for the panoramic header
     * based on measured title widths and inter-item spacing (legacy/pivot fallback).
     */
    fun calculateTitleScrollOffset(
        continuousPage: Float,
        titleWidths: List<Int>,
        spacingPx: Int,
        maxScroll: Int
    ): Int {
        if (titleWidths.isEmpty() || maxScroll <= 0) return 0

        val clampedPage = continuousPage.coerceIn(0f, (titleWidths.size - 1).toFloat())
        val baseIndex = clampedPage.toInt().coerceIn(0, titleWidths.lastIndex)
        val fraction = clampedPage - baseIndex

        var currentOffset = 0
        for (i in 0 until baseIndex) {
            currentOffset += titleWidths[i] + spacingPx
        }

        val nextSpan = if (baseIndex < titleWidths.lastIndex) {
            titleWidths[baseIndex] + spacingPx
        } else {
            0
        }

        val targetScroll = (currentOffset + fraction * nextSpan).roundToInt()
        return targetScroll.coerceIn(0, maxScroll)
    }

    /**
     * Calculates title scroll offset using a guaranteed panoramic span per page.
     * At page N, title N sits precisely at [startMarginPx], and title N+1 peeks at [screenWidthPx - peekWidthPx].
     */
    fun calculatePanoramicScrollOffset(
        continuousPage: Float,
        pageCount: Int,
        titleSpanPx: Int,
        maxScroll: Int
    ): Int {
        if (pageCount <= 1 || maxScroll <= 0) return 0
        val clampedPage = continuousPage.coerceIn(0f, (pageCount - 1).toFloat())
        val targetScroll = (clampedPage * titleSpanPx).roundToInt()
        return targetScroll.coerceIn(0, maxScroll)
    }

    /**
     * Calculates slow multi-speed background parallax offset for the brand header ("meena").
     * Moves at [speedRatio] (default ~0.32x) relative to page scroll for deep 3-layer Metro parallax.
     */
    fun calculateBrandParallaxOffset(
        continuousPage: Float,
        screenWidthPx: Int,
        speedRatio: Float = 0.32f
    ): Float {
        return -continuousPage * (screenWidthPx * speedRatio * 0.25f)
    }
}
