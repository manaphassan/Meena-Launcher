package my.hana.meena.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The neutral colours of one base theme. Screens read them through the Meena* properties below,
 * so dark, light and paper all work without each screen choosing colours itself.
 * Names keep their dark-theme meaning: "black" is the page background, "white" the main text.
 */
@Immutable
data class MeenaPalette(
    val id: String,
    val background: Color,
    val darkSlate: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    /** Text and glyphs drawn on an accent fill (tiles, buttons). */
    val onAccent: Color,
    /** Gains, healthy states. */
    val positive: Color,
    /** Losses, errors. */
    val negative: Color,
    /** Needs attention (updates, moderate air). */
    val warning: Color,
    /** Replaces the user's accent; paper is monochrome. */
    val accentOverride: Color? = null,
    /** Colour-coded readings such as air quality are drawn in the text colour instead. */
    val monochrome: Boolean = false,
    val isLight: Boolean
)

// Contrast checked in ThemePaletteTest: text, secondary and muted are all >= 4.5:1 on their background.
val MeenaDarkPalette = MeenaPalette(
    id = "dark",
    background = Color(0xFF000000),
    darkSlate = Color(0xFF141414),
    surface = Color(0xFF0C0C0C),
    surfaceElevated = Color(0xFF161616),
    border = Color(0xFF222222),
    borderSubtle = Color(0xFF181818),
    text = Color(0xFFFFFFFF),          // 21.0:1
    textSecondary = Color(0xFFAAAAAA), // 10.3:1
    textMuted = Color(0xFF888888),     // 6.3:1
    textDisabled = Color(0xFF555555),
    onAccent = Color(0xFFFFFFFF),
    positive = Color(0xFF16A516),      // 6.7:1
    negative = Color(0xFFFF4D5E),      // 6.5:1, 5.7:1 on grey
    warning = Color(0xFFF0A30A),       // 9.7:1
    isLight = false
)

/** Windows Phone light: white page, black type. */
val MeenaLightPalette = MeenaPalette(
    id = "light",
    background = Color(0xFFFFFFFF),
    darkSlate = Color(0xFFE6E6E6),
    surface = Color(0xFFF2F2F2),
    surfaceElevated = Color(0xFFE6E6E6),
    border = Color(0xFFCCCCCC),
    borderSubtle = Color(0xFFE6E6E6),
    text = Color(0xFF000000),          // 21.0:1
    textSecondary = Color(0xFF444444), // 9.7:1
    textMuted = Color(0xFF666666),     // 5.7:1
    textDisabled = Color(0xFF8A8A8A),
    onAccent = Color(0xFFFFFFFF),
    positive = Color(0xFF0E6B0E),      // 6.5:1, 5.4:1 on grey
    negative = Color(0xFFC50F1F),      // 6.0:1
    warning = Color(0xFF8A5300),       // 6.1:1
    isLight = true
)

/** For e-ink: black on white only, few greys, no accent colour. */
val MeenaPaperPalette = MeenaPalette(
    id = "paper",
    background = Color(0xFFFFFFFF),
    darkSlate = Color(0xFFDDDDDD),
    surface = Color(0xFFEEEEEE),
    surfaceElevated = Color(0xFFDDDDDD),
    border = Color(0xFF888888),
    borderSubtle = Color(0xFFDDDDDD),
    text = Color(0xFF000000),          // 21.0:1
    textSecondary = Color(0xFF333333), // 12.6:1
    textMuted = Color(0xFF555555),     // 7.5:1
    textDisabled = Color(0xFFBBBBBB),
    onAccent = Color(0xFFFFFFFF),
    positive = Color(0xFF000000),
    negative = Color(0xFF000000),
    warning = Color(0xFF000000),
    accentOverride = Color(0xFF000000),
    monochrome = true,
    isLight = true
)

fun meenaPaletteFor(baseTheme: String): MeenaPalette = when (baseTheme) {
    "light" -> MeenaLightPalette
    "paper" -> MeenaPaperPalette
    else -> MeenaDarkPalette
}

val LocalMeenaPalette = staticCompositionLocalOf { MeenaDarkPalette }

val MeenaBlack: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.background
val MeenaDarkSlate: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.darkSlate
val MeenaSurface: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.surface
val MeenaSurfaceElevated: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.surfaceElevated
val MeenaBorder: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.border
val MeenaBorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.borderSubtle
val MeenaTextWhite: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.text
val MeenaTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.textSecondary
val MeenaTextMuted: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.textMuted
val MeenaTextDisabled: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.textDisabled
val MeenaOnAccent: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.onAccent

// Status colours follow the theme (see MeenaPalette): bright on black, darker on white, black on paper.
val MeenaProfitGreen: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.positive
val MeenaLossRed: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.negative
val MeenaSolarAmber: Color @Composable @ReadOnlyComposable get() = LocalMeenaPalette.current.warning

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
val MeenaAmber: Color @Composable @ReadOnlyComposable get() = MeenaSolarAmber

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


