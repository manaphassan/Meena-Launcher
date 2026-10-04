package my.hana.meena.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite

/**
 * Authentic Windows Phone Metro UI toggle switch (§2.2 #74, Rule 7):
 * - Strictly 0dp square corners (no pill or circle thumb)
 * - Rectangular track:
 *   - On: Solid accent fill with white rectangular thumb on the right
 *   - Off: Dark outline track with white rectangular thumb on the left
 * - "on" / "off" label beside the switch in lowercase
 * - Minimum 48dp touch target
 */
@Composable
fun MetroToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabel: Boolean = true
) {
    val trackWidth = 52.dp
    val trackHeight = 24.dp
    val thumbWidth = 10.dp
    val thumbHeight = 18.dp
    val padding = 3.dp

    val maxOffset = trackWidth - thumbWidth - (padding * 2)

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "metroToggleOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MeenaBlack,
        animationSpec = tween(durationMillis = 150),
        label = "metroToggleTrackColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MeenaTextMuted,
        animationSpec = tween(durationMillis = 150),
        label = "metroToggleBorderColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        // Flat rectangular track
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackHeight)
                .background(trackColor)
                .border(width = 2.dp, color = borderColor)
                .padding(padding),
            contentAlignment = Alignment.CenterStart
        ) {
            // Flat rectangular thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(thumbWidth)
                    .height(thumbHeight)
                    .background(MeenaTextWhite)
            )
        }

        if (showLabel) {
            Text(
                text = if (checked) "on" else "off",
                style = MaterialTheme.typography.bodyMedium,
                color = if (checked) MeenaTextWhite else MeenaTextMuted
            )
        }
    }
}

/**
 * Standard Metro toggle row with title, optional description, and toggle on the right.
 */
@Composable
fun MetroToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title.lowercase(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MeenaTextWhite else MeenaTextMuted
            )
            if (description != null) {
                Text(
                    text = description.lowercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
        }

        MetroToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            showLabel = true
        )
    }
}
