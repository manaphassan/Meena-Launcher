package my.hana.meena.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Windows Phone Metro UI tactile press indication (§2.4, Rule 7):
 * - Scales element down to 0.96 on press
 * - Applies a slight perspective 3D tilt
 * - Strictly no Material ripple
 * - Spring animation for physical tile-like responsiveness
 */
fun Modifier.metroClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    tiltDegrees: Float = 1.8f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "metroScale"
    )

    val tilt by animateFloatAsState(
        targetValue = if (isPressed) tiltDegrees else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "metroTilt"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationX = tilt
            cameraDistance = 16f * density
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Strictly no ripple
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick
        )
}

/**
 * Visual-only modifier for press tilt & scale where pointer gestures are handled separately.
 */
fun Modifier.metroPressEffect(isPressed: Boolean, tiltDegrees: Float = 1.8f): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "metroPressScale"
    )

    val tilt by animateFloatAsState(
        targetValue = if (isPressed) tiltDegrees else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "metroPressTilt"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        rotationX = tilt
        cameraDistance = 16f * density
    }
}
