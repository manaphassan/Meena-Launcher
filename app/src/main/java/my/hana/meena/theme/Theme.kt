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
): androidx.compose.material3.ColorScheme {
    val p = meenaPaletteFor(baseTheme)
    val primary = p.accentOverride ?: accent
    return if (p.isLight) {
        lightColorScheme(
            primary = primary,
            onPrimary = p.onAccent,
            background = p.background,
            onBackground = p.text,
            surface = p.background,
            onSurface = p.text,
            surfaceVariant = p.darkSlate,
            onSurfaceVariant = p.textMuted,
            outline = p.border
        )
    } else {
        darkColorScheme(
            primary = primary,
            onPrimary = p.onAccent,
            background = p.background,
            onBackground = p.text,
            surface = p.surface,
            onSurface = p.text,
            surfaceVariant = p.darkSlate,
            onSurfaceVariant = p.textMuted,
            outline = p.border
        )
    }
}

@Composable
fun MeenaLauncherTheme(
    baseTheme: String = "dark",
    accentColor: Color = MeenaCyan,
    fontScalePercent: Int = 100,
    content: @Composable () -> Unit,
) {
    val palette = meenaPaletteFor(baseTheme)
    val accent = palette.accentOverride ?: accentColor
    val colorScheme = getMeenaColorScheme(baseTheme, accentColor)
    val typography = androidx.compose.runtime.remember(fontScalePercent, accent, palette) {
        getMeenaTypography(
            fontScalePercent = fontScalePercent,
            titleAccentColor = accent,
            textColor = palette.text,
            secondaryColor = palette.textSecondary,
            mutedColor = palette.textMuted
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalMeenaPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MeenaMetroShapes,
            content = content
        )
    }
}


object MeenaSpacing {
    val s = 6.dp
    val m = 12.dp
    val l = 24.dp
}
