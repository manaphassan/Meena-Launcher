package my.hana.meena.ui

import androidx.compose.ui.graphics.Color
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaCyan
import my.hana.meena.theme.MeenaDarkSlate
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextDisabled
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.theme.calculateContrastRatio
import my.hana.meena.theme.getMeenaColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ContrastTest: Evaluates WCAG contrast ratios across all theme token pairs.
 *
 * Uses calculateContrastRatio() from theme/Color.kt to evaluate every text/background
 * token pair in both themes (dark/amoled and light).
 *
 * Threshold: At least 4.5:1 (WCAG AA for normal text), except MeenaTextDisabled which
 * is intentionally subdued for inactive elements.
 *
 * In Phase 0, current baseline failures (such as dark text tokens on light theme backgrounds
 * or onPrimary white on cyan accent) are audited and reported for resolution in Phase 6.
 */
class ContrastTest {

    data class ContrastResult(
        val theme: String,
        val textToken: String,
        val bgToken: String,
        val ratio: Float,
        val passes: Boolean,
        val isExempt: Boolean = false
    )

    private fun evaluatePairs(): List<ContrastResult> {
        val results = mutableListOf<ContrastResult>()

        val darkScheme = getMeenaColorScheme("amoled", MeenaCyan)
        val lightScheme = getMeenaColorScheme("light", MeenaCyan)

        // 1. Dark Theme (AMOLED)
        val darkBackgrounds = listOf(
            "background" to darkScheme.background,
            "surface" to darkScheme.surface,
            "surfaceVariant" to darkScheme.surfaceVariant
        )
        val darkTexts = listOf(
            "onBackground" to darkScheme.onBackground,
            "onSurface" to darkScheme.onSurface,
            "MeenaTextWhite" to MeenaTextWhite,
            "MeenaTextSecondary" to MeenaTextSecondary,
            "MeenaTextMuted" to MeenaTextMuted,
            "MeenaTextDisabled" to MeenaTextDisabled
        )

        for ((bgName, bgCol) in darkBackgrounds) {
            for ((textName, textCol) in darkTexts) {
                val ratio = calculateContrastRatio(textCol, bgCol)
                val isExempt = textName == "MeenaTextDisabled"
                val passes = isExempt || ratio >= 4.5f
                results.add(
                    ContrastResult(
                        theme = "dark",
                        textToken = textName,
                        bgToken = bgName,
                        ratio = ratio,
                        passes = passes,
                        isExempt = isExempt
                    )
                )
            }
        }

        // onPrimary on primary in dark theme
        val darkOnPrimaryRatio = calculateContrastRatio(darkScheme.onPrimary, darkScheme.primary)
        results.add(
            ContrastResult(
                theme = "dark",
                textToken = "onPrimary",
                bgToken = "primary",
                ratio = darkOnPrimaryRatio,
                passes = darkOnPrimaryRatio >= 4.5f
            )
        )

        // 2. Light Theme
        val lightBackgrounds = listOf(
            "background" to lightScheme.background,
            "surface" to lightScheme.surface,
            "surfaceVariant" to lightScheme.surfaceVariant
        )
        val lightTexts = listOf(
            "onBackground" to lightScheme.onBackground,
            "onSurface" to lightScheme.onSurface,
            "MeenaTextWhite" to MeenaTextWhite,
            "MeenaTextSecondary" to MeenaTextSecondary,
            "MeenaTextMuted" to MeenaTextMuted,
            "MeenaTextDisabled" to MeenaTextDisabled
        )

        for ((bgName, bgCol) in lightBackgrounds) {
            for ((textName, textCol) in lightTexts) {
                val ratio = calculateContrastRatio(textCol, bgCol)
                val isExempt = textName == "MeenaTextDisabled"
                val passes = isExempt || ratio >= 4.5f
                results.add(
                    ContrastResult(
                        theme = "light",
                        textToken = textName,
                        bgToken = bgName,
                        ratio = ratio,
                        passes = passes,
                        isExempt = isExempt
                    )
                )
            }
        }

        // onPrimary on primary in light theme
        val lightOnPrimaryRatio = calculateContrastRatio(lightScheme.onPrimary, lightScheme.primary)
        results.add(
            ContrastResult(
                theme = "light",
                textToken = "onPrimary",
                bgToken = "primary",
                ratio = lightOnPrimaryRatio,
                passes = lightOnPrimaryRatio >= 4.5f
            )
        )

        return results
    }

    @Test
    fun coreTokens_meetContrastRequirements() {
        val darkScheme = getMeenaColorScheme("amoled", MeenaCyan)
        val lightScheme = getMeenaColorScheme("light", MeenaCyan)

        // Core onBackground and onSurface against background
        val darkOnBg = calculateContrastRatio(darkScheme.onBackground, darkScheme.background)
        val darkOnSurface = calculateContrastRatio(darkScheme.onSurface, darkScheme.surface)
        assertTrue("Dark onBackground contrast must be >= 4.5:1, was $darkOnBg", darkOnBg >= 4.5f)
        assertTrue("Dark onSurface contrast must be >= 4.5:1, was $darkOnSurface", darkOnSurface >= 4.5f)

        // Dark MeenaText tokens against MeenaBlack
        val whiteRatio = calculateContrastRatio(MeenaTextWhite, MeenaBlack)
        val secondaryRatio = calculateContrastRatio(MeenaTextSecondary, MeenaBlack)
        val mutedRatio = calculateContrastRatio(MeenaTextMuted, MeenaBlack)
        assertTrue("MeenaTextWhite on black >= 4.5:1", whiteRatio >= 4.5f)
        assertTrue("MeenaTextSecondary on black >= 4.5:1", secondaryRatio >= 4.5f)
        assertTrue("MeenaTextMuted on black >= 4.5:1", mutedRatio >= 4.5f)

        // Light onBackground and onSurface against background
        val lightOnBg = calculateContrastRatio(lightScheme.onBackground, lightScheme.background)
        val lightOnSurface = calculateContrastRatio(lightScheme.onSurface, lightScheme.surface)
        assertTrue("Light onBackground contrast must be >= 4.5:1, was $lightOnBg", lightOnBg >= 4.5f)
        assertTrue("Light onSurface contrast must be >= 4.5:1, was $lightOnSurface", lightOnSurface >= 4.5f)
    }

    @Test
    fun auditAllThemeTokenPairs_andReportFailures() {
        val results = evaluatePairs()

        println("=== Contrast Audit: All Token Pairs ===")
        val failures = mutableListOf<ContrastResult>()

        for (r in results) {
            val status = when {
                r.isExempt -> "[EXEMPT]"
                r.passes -> "[PASS]"
                else -> "[FAIL]"
            }
            val formatted = String.format("%-6s | %-6s | %-20s on %-16s = %.2f:1", status, r.theme, r.textToken, r.bgToken, r.ratio)
            println(formatted)

            if (!r.passes && !r.isExempt) {
                failures.add(r)
            }
        }

        println("\n=== Total Failures Detected at Baseline: ${failures.size} ===")
        for (f in failures) {
            println(String.format("  FAIL: %s %s on %s (%.2f:1 < 4.5:1)", f.theme, f.textToken, f.bgToken, f.ratio))
        }

        // In Phase 0, baseline failures are audited and reported for resolution in Phase 6.
        // Confirm that failures do not exceed the 11 known baseline failure pairs.
        assertTrue(
            "Found unexpected new contrast failures: ${failures.size} (expected at most 11 baseline failures)",
            failures.size <= 11
        )
    }
}
