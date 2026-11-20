package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import my.hana.meena.theme.MeenaSpacing

/**
 * A flat, square, filled button with the Meena press (scale 0.96 and a tilt toward the finger,
 * no ripple). Replaces Material's Button, which brings its own ripple. At least 48dp tall.
 */
@Composable
fun MetroButton(
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .semantics { role = Role.Button }
            .metroClickable(tiltDegrees = 3f, onClick = onClick)
            .background(containerColor)
            .heightIn(min = 48.dp)
            .padding(horizontal = MeenaSpacing.l, vertical = MeenaSpacing.m),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** A 48dp tap target around a small glyph, with the Meena press instead of IconButton's ripple. */
@Composable
fun MetroIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { role = Role.Button }
            .metroClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
