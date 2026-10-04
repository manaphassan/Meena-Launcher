package my.hana.meena.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import my.hana.meena.R
import java.util.Locale

// Google Fonts Provider with Play Services certificate support
private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/**
 * Returns a high-fidelity, grounded font family based on user settings.
 * Supports authentic Swiss geometric fonts (Plus Jakarta Sans, Inter, Outfit)
 * with graceful fallback to system Sans-Serif.
 */
fun getMeenaFontFamily(name: String = "Segoe UI"): FontFamily {
    val targetName = when (name.lowercase(Locale.ROOT).trim()) {
        "inter" -> "Inter"
        "outfit" -> "Outfit"
        "plus jakarta sans" -> "Plus Jakarta Sans"
        "roboto flex" -> "Roboto Flex"
        // Segoe UI is a proprietary Microsoft typeface; "Plus Jakarta Sans" & "Inter" provide the exact geometric Swiss metrics
        "segoe ui", "segoe ui variable" -> "Plus Jakarta Sans"
        "system", "system sans-serif" -> return FontFamily.SansSerif
        "monospace" -> return FontFamily.Monospace
        else -> "Plus Jakarta Sans"
    }

    return try {
        val googleFont = GoogleFont(targetName)
        FontFamily(
            Font(googleFont = googleFont, fontProvider = GoogleFontProvider, weight = FontWeight.ExtraLight),
            Font(googleFont = googleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Light),
            Font(googleFont = googleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
            Font(googleFont = googleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
            Font(googleFont = googleFont, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
            Font(googleFont = googleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
        )
    } catch (e: Exception) {
        FontFamily.SansSerif
    }
}

/**
 * Factory for Metro typography hierarchy scaling with font family and user scale percentage.
 */
fun getMeenaTypography(
    fontFamilyName: String = "Segoe UI",
    fontScalePercent: Int = 100,
    titleAccentColor: Color = MeenaCyan
): Typography {
    val scale = (fontScalePercent / 100f).coerceIn(0.8f, 1.4f)
    val activeFont = getMeenaFontFamily(fontFamilyName)

    return Typography(
        // Panoramic Pivot Header (42sp Light / ExtraLight)
        displayLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.ExtraLight,
            fontSize = (42 * scale).sp,
            lineHeight = (46 * scale).sp,
            letterSpacing = ((-1.5) * scale).sp,
            color = MeenaTextWhite
        ),
        // Large Hero Clock (64sp ExtraLight)
        displayMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.ExtraLight,
            fontSize = (64 * scale).sp,
            lineHeight = (68 * scale).sp,
            letterSpacing = ((-2.0) * scale).sp,
            color = MeenaTextWhite
        ),
        // Sub-Pivot or Section Headline (24sp Light)
        headlineMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (24 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = ((-0.5) * scale).sp,
            color = MeenaTextWhite
        ),
        // Widget Section Titles (16sp Light / Normal, Lowercase, Spaced)
        titleMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = (0.5 * scale).sp,
            color = titleAccentColor
        ),
        // Standard Content Body (16sp Light per spec §2.4)
        bodyLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (16 * scale).sp,
            lineHeight = (22 * scale).sp,
            letterSpacing = (0.2 * scale).sp,
            color = MeenaTextWhite
        ),
        bodyMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (19 * scale).sp,
            letterSpacing = (0.1 * scale).sp,
            color = MeenaTextSecondary
        ),
        // Telemetry & Captions (12sp minimum per hard rule §2.4)
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = (0.5 * scale).sp,
            color = MeenaTextMuted
        )
    )
}

// Default static typography instance for backward compatibility
val MeenaTypography = getMeenaTypography()

