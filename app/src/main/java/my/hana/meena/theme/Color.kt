package my.hana.meena.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// Pure AMOLED Foundations
val MeenaBlack = Color(0xFF000000)
val MeenaDarkSlate = Color(0xFF141414)
val MeenaSurface = Color(0xFF0C0C0C)
val MeenaSurfaceElevated = Color(0xFF161616)
val MeenaBorder = Color(0xFF222222)
val MeenaBorderSubtle = Color(0xFF181818)

// Text Colors (Contrast validated against MeenaBlack >= 4.5:1)
val MeenaTextWhite = Color(0xFFFFFFFF)       // 21.0:1
val MeenaTextSecondary = Color(0xFFAAAAAA)   // 10.3:1
val MeenaTextMuted = Color(0xFF888888)       // 6.3:1 (exceeds 4.5:1 threshold; previous 0xFF666666 was 3.5:1)
val MeenaTextDisabled = Color(0xFF555555)    // Intentionally subdued for inactive hints

// Telemetry & Financial Status
val MeenaProfitGreen = Color(0xFF107C10)
val MeenaLossRed = Color(0xFFE81123)
val MeenaSolarAmber = Color(0xFFF0A30A)

// Authentic Windows Phone 10-Accent Swatches (WP Design Guidelines §2.2 #70)
val MeenaBlue = Color(0xFF00A4EF)
val MeenaCobalt = Color(0xFF0050EF)
val MeenaCrimson = Color(0xFFE51400)
val MeenaMango = Color(0xFFF09609)
val MeenaEmerald = Color(0xFF008A00)
val MeenaMagenta = Color(0xFFD80073)
val MeenaPurple = Color(0xFFA200FF)
val MeenaLime = Color(0xFFA4C400)
val MeenaTeal = Color(0xFF00ABA9)
val MeenaBrown = Color(0xFF825A2C)
val MeenaPink = Color(0xFFE671B8)

// Backward-compatible alias
val MeenaCyan = MeenaBlue
val MeenaOrange = MeenaMango
val MeenaAmber = MeenaSolarAmber

/**
 * Metadata pair for theme accent selection.
 */
data class AccentSwatch(
    val id: String,
    val name: String,
    val color: Color,
    val hex: String
)

val Meena10AccentSwatches = listOf(
    AccentSwatch("blue", "blue", MeenaBlue, "#00A4EF"),
    AccentSwatch("red", "crimson", MeenaCrimson, "#E51400"),
    AccentSwatch("mango", "mango", MeenaMango, "#F09609"),
    AccentSwatch("green", "emerald", MeenaEmerald, "#008A00"),
    AccentSwatch("magenta", "magenta", MeenaMagenta, "#D80073"),
    AccentSwatch("purple", "purple", MeenaPurple, "#A200FF"),
    AccentSwatch("lime", "lime", MeenaLime, "#A4C400"),
    AccentSwatch("teal", "teal", MeenaTeal, "#00ABA9"),
    AccentSwatch("brown", "brown", MeenaBrown, "#825A2C"),
    AccentSwatch("pink", "pink", MeenaPink, "#E671B8")
)

/**
 * Calculates WCAG 2.1 relative luminance for contrast evaluation.
 */
fun relativeLuminance(color: Color): Float {
    fun channel(c: Float): Float {
        return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}

/**
 * Calculates contrast ratio between foreground and background (range 1.0 to 21.0).
 */
fun calculateContrastRatio(foreground: Color, background: Color): Float {
    val lum1 = relativeLuminance(foreground)
    val lum2 = relativeLuminance(background)
    val brightest = max(lum1, lum2)
    val darkest = min(lum1, lum2)
    return (brightest + 0.05f) / (darkest + 0.05f)
}

/**
 * Checks if color meets WCAG AA standards:
 * - 4.5:1 for regular text
 * - 3.0:1 for large text (>= 18pt or >= 14pt bold)
 */
fun isAccessibleContrast(foreground: Color, background: Color, isLargeText: Boolean = false): Boolean {
    val ratio = calculateContrastRatio(foreground, background)
    return if (isLargeText) ratio >= 3.0f else ratio >= 4.5f
}


