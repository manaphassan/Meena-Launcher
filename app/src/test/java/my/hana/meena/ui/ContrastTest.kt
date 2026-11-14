package my.hana.meena.ui

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.theme.Meena10AccentSwatches
import my.hana.meena.theme.MeenaCyan
import my.hana.meena.theme.MeenaDarkPalette
import my.hana.meena.theme.MeenaLightPalette
import my.hana.meena.theme.MeenaPaperPalette
import my.hana.meena.theme.calculateContrastRatio
import my.hana.meena.theme.getMeenaColorScheme
import my.hana.meena.theme.meenaPaletteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text colour must reach 4.5:1 on every page colour, in dark, light and paper. */
class ContrastTest {

    private val palettes = listOf(MeenaDarkPalette, MeenaLightPalette, MeenaPaperPalette)

    @Test
    fun allTextColours_readOnAllPageColours_inEveryTheme() {
        val failures = mutableListOf<String>()
        for (p in palettes) {
            val scheme = getMeenaColorScheme(p.id, MeenaCyan)
            val pages = mapOf(
                "background" to p.background, "surface" to p.surface, "darkSlate" to p.darkSlate,
                "scheme.background" to scheme.background, "scheme.surface" to scheme.surface
            )
            val texts = mapOf(
                "text" to p.text, "textSecondary" to p.textSecondary, "textMuted" to p.textMuted,
                "positive" to p.positive, "negative" to p.negative, "warning" to p.warning,
                "onBackground" to scheme.onBackground, "onSurface" to scheme.onSurface
            )
            for ((pageName, page) in pages) for ((textName, text) in texts) {
                val ratio = calculateContrastRatio(text, page)
                if (ratio < 4.5f) failures += "${p.id}: $textName on $pageName = ${"%.2f".format(ratio)}:1"
            }
        }
        assertTrue("contrast below 4.5:1:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun accentAsText_readsOnEveryPage_forEverySwatchAndTheme() {
        val failures = mutableListOf<String>()
        for (p in palettes) for (s in Meena10AccentSwatches) {
            val primary = getMeenaColorScheme(p.id, s.color).primary
            for (page in my.hana.meena.theme.pageColours(p)) {
                val ratio = calculateContrastRatio(primary, page)
                if (ratio < 4.5f) failures += "${p.id}/${s.name}: ${"%.2f".format(ratio)}:1"
            }
        }
        assertTrue("accent text below 4.5:1:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun readableAccent_keepsColoursThatAlreadyRead_andKeepsTheHue() {
        // Default blue on black is 7.56:1, so the dark theme shows it untouched.
        assertEquals(MeenaCyan, getMeenaColorScheme("dark", MeenaCyan).primary)
        // On white it is deepened, not swapped: blue stays the strongest channel.
        val onLight = getMeenaColorScheme("light", MeenaCyan).primary
        assertTrue(onLight != MeenaCyan && onLight.blue > onLight.red && onLight.blue > onLight.green)
    }

    @Test
    fun paper_isBlackAndWhite_andIgnoresTheAccent() {
        val scheme = getMeenaColorScheme("paper", MeenaCyan)
        assertEquals(MeenaPaperPalette.accentOverride, scheme.primary)
        assertTrue(calculateContrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
        assertTrue(MeenaPaperPalette.isLight)
    }

    @Test
    fun paletteLookup_fallsBackToDark() {
        assertEquals("light", meenaPaletteFor("light").id)
        assertEquals("paper", meenaPaletteFor("paper").id)
        assertEquals("dark", meenaPaletteFor("amoled").id)
        assertFalse(meenaPaletteFor("dark").isLight)
    }

    @Test
    fun paper_alwaysUsesCalmMotion() {
        assertTrue(MeenaUserSettings(baseTheme = "paper").reducedMotion)
        assertFalse(MeenaUserSettings(baseTheme = "light").reducedMotion)
    }
}
