package my.hana.meena

import androidx.compose.ui.graphics.Color
import my.hana.meena.theme.MeenaDarkPalette
import my.hana.meena.theme.WallpaperDim
import my.hana.meena.theme.calculateContrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wallpaper dim is chosen so faint text reads at 4.5:1 over the photo (20 Nov 2026). */
class WallpaperDimTest {

    private val text = listOf(MeenaDarkPalette.textMuted, MeenaDarkPalette.positive, MeenaDarkPalette.negative)

    private fun photo(r: Int, g: Int, b: Int, size: Int = 1000) =
        IntArray(size) { (0xFF shl 24) or (r shl 16) or (g shl 8) or b }

    /** The colour of [r],[g],[b] under the black overlay, as Compose draws it. */
    private fun dimmed(r: Int, g: Int, b: Int, percent: Int): Color {
        val keep = 1f - percent / 100f
        return Color(r / 255f * keep, g / 255f * keep, b / 255f * keep)
    }

    private fun worstRatio(r: Int, g: Int, b: Int, percent: Int) =
        text.minOf { calculateContrastRatio(it, dimmed(r, g, b, percent)) }

    @Test
    fun brightPhotos_areDarkenedJustEnough() {
        // White (worst case) and the sand of the owner's beach photo.
        for ((r, g, b) in listOf(Triple(255, 255, 255), Triple(225, 205, 175))) {
            val dim = WallpaperDim.dimFor(photo(r, g, b), text, minPercent = 60)
            assertTrue("dim $dim for $r,$g,$b", dim in 61..90)
            assertTrue(worstRatio(r, g, b, dim) >= 4.5f)
            assertTrue("one percent less would be enough", worstRatio(r, g, b, dim - 1) < 4.5f)
        }
    }

    @Test
    fun darkPhotos_keepThePersonsSetting() {
        assertEquals(60, WallpaperDim.dimFor(photo(10, 10, 10), text, minPercent = 60))
        assertEquals(80, WallpaperDim.dimFor(photo(10, 10, 10), text, minPercent = 80))
        assertEquals(40, WallpaperDim.dimFor(photo(10, 10, 10), text, minPercent = 10))
    }

    @Test
    fun aSmallHighlight_doesNotBlackOutThePhoto() {
        val mostlyDark = photo(10, 10, 10).also { for (i in 0 until 5) it[i] = 0xFFFFFFFF.toInt() }
        assertEquals(60, WallpaperDim.dimFor(mostlyDark, text, minPercent = 60))
        // A tenth of the photo bright is not a highlight.
        val partBright = photo(10, 10, 10).also { for (i in 0 until 100) it[i] = 0xFFFFFFFF.toInt() }
        assertTrue(WallpaperDim.dimFor(partBright, text, minPercent = 60) > 60)
    }

    @Test
    fun nothingToMeasure_keepsTheSetting() {
        assertEquals(70, WallpaperDim.dimFor(IntArray(0), text, minPercent = 70))
        assertEquals(70, WallpaperDim.dimFor(photo(255, 255, 255), emptyList(), minPercent = 70))
    }

    @Test
    fun neverDarkerThanTheCap() {
        // Text too dark to ever reach 4.5:1 on a black page gets the darkest dim, not more.
        assertEquals(90, WallpaperDim.dimFor(photo(255, 255, 255), listOf(Color(0xFF222222)), minPercent = 60))
    }
}
