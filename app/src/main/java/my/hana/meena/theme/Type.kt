package my.hana.meena.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import my.hana.meena.R

val SelawikFontFamily = FontFamily(
    Font(R.font.selawik_light, FontWeight.Light),
    Font(R.font.selawik_regular, FontWeight.Normal),
    Font(R.font.selawik_semibold, FontWeight.Medium)
)

/**
 * Factory for Metro typography hierarchy scaling with font family and user scale percentage.
 */
fun getMeenaTypography(
    fontScalePercent: Int = 100,
    titleAccentColor: Color = MeenaCyan
): Typography {
    val scale = (fontScalePercent / 100f).coerceIn(0.8f, 1.4f)
    val activeFont = SelawikFontFamily

    return Typography(
        // page title 42sp light
        displayLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (42 * scale).sp,
            lineHeight = (46 * scale).sp,
            letterSpacing = ((-1.5) * scale).sp,
            color = MeenaTextWhite
        ),
        // displayMedium: 64sp Light (Replace ExtraLight with Light)
        displayMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (64 * scale).sp,
            lineHeight = (68 * scale).sp,
            letterSpacing = ((-2.0) * scale).sp,
            color = MeenaTextWhite
        ),
        // section 24sp light
        headlineMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (24 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = ((-0.5) * scale).sp,
            color = MeenaTextWhite
        ),
        // titleMedium (was normal, I'll keep it Normal)
        titleMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = (0.5 * scale).sp,
            color = MeenaTextWhite
        ),
        // body 15-17sp (16sp)
        bodyLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
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
        displaySmall = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (48 * scale).sp,
            lineHeight = (52 * scale).sp,
            letterSpacing = (-1.0 * scale).sp,
            color = Color.Unspecified
        ),
        headlineLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (32 * scale).sp,
            lineHeight = (36 * scale).sp,
            letterSpacing = (-0.5 * scale).sp,
            color = Color.Unspecified
        ),
        headlineSmall = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (20 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = (0 * scale).sp,
            color = Color.Unspecified
        ),
        titleLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Light,
            fontSize = (22 * scale).sp,
            lineHeight = (26 * scale).sp,
            letterSpacing = (0 * scale).sp,
            color = Color.Unspecified
        ),
        titleSmall = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (18 * scale).sp,
            letterSpacing = (0.3 * scale).sp,
            color = Color.Unspecified
        ),
        bodySmall = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = (0.2 * scale).sp,
            color = Color.Unspecified
        ),
        labelLarge = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (18 * scale).sp,
            letterSpacing = (0.3 * scale).sp,
            color = Color.Unspecified
        ),
        labelMedium = TextStyle(
            fontFamily = activeFont,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = (0.4 * scale).sp,
            color = Color.Unspecified
        ),
        // captions at least 12sp
        labelSmall = TextStyle(
            fontFamily = activeFont, // Changed from Monospace to activeFont
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = (0.5 * scale).sp,
            color = MeenaTextMuted
        )
    )
}

val MeenaTypography = getMeenaTypography()
