package my.hana.meena.ui.navigation

import kotlin.math.roundToInt

/**
 * Calculates the exact horizontal scroll offset for the panoramic pivot header
 * based on dynamically measured title widths and inter-item spacing.
 *
 * Replaces the legacy fixed 135dp offset with true Metro-style 1:1 parallax tracking.
 */
object ParallaxHelper {

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
}
