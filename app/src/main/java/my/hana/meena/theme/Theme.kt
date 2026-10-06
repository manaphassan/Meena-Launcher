package my.hana.meena.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 0dp Corner Radius Geometry (Strict Metro UI Rule)
val MeenaMetroShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

fun getMeenaColorScheme(
    baseTheme: String = "dark",
    accent: Color = MeenaCyan
) = when (baseTheme) {
    "light" -> lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        background = Color(0xFFF7F7F9),
        onBackground = Color(0xFF111111),
        surface = Color.White,
        onSurface = Color(0xFF111111),
        surfaceVariant = Color(0xFFEFEFEF),
        outline = Color(0xFFCCCCCC)
    )
    else -> darkColorScheme( // "amoled" default
        primary = accent,
        onPrimary = Color.White,
        background = MeenaBlack,
        onBackground = MeenaTextWhite,
        surface = MeenaSurface,
        onSurface = MeenaTextWhite,
        surfaceVariant = MeenaDarkSlate,
        outline = MeenaBorder
    )
}

@Composable
fun MeenaLauncherTheme(
    baseTheme: String = "dark",
    accentColor: Color = MeenaCyan,
        fontScalePercent: Int = 100,
    content: @Composable () -> Unit,
) {
    val colorScheme = getMeenaColorScheme(baseTheme, accentColor)
    val typography = androidx.compose.runtime.remember(fontScalePercent, accentColor) {
        getMeenaTypography(
                        fontScalePercent = fontScalePercent,
            titleAccentColor = accentColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = MeenaMetroShapes,
        content = content
    )
}


object MeenaSpacing {
    val s = 6.dp
    val m = 12.dp
    val l = 24.dp
}
