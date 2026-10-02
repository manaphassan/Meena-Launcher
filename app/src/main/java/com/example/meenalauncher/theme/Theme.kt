package com.example.meenalauncher.theme

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
    baseTheme: String = "amoled",
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
    "slate" -> darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        background = Color(0xFF141418),
        onBackground = Color.White,
        surface = Color(0xFF1E1E24),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF282830),
        outline = Color(0xFF333333)
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
    baseTheme: String = "amoled",
    accentColor: Color = MeenaCyan,
    content: @Composable () -> Unit,
) {
    val colorScheme = getMeenaColorScheme(baseTheme, accentColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MeenaTypography,
        shapes = MeenaMetroShapes,
        content = content
    )
}
