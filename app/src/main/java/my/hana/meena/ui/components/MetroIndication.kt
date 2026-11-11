package my.hana.meena.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Windows Phone Metro UI tactile press indication (§2.4, Rule 7):
 * - Scales element down to 0.96 on press
 * - Tilts toward the touch point (perspective 3D), like the original Metro tiles
 * - Strictly no Material ripple
 * - Spring animation for physical tile-like responsiveness
 */
fun Modifier.metroClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    tiltDegrees: Float = 6f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var size by remember { mutableStateOf(IntSize.Zero) }
    // Normalised press point in -1..1 on each axis; the element tilts toward the finger.
    var pressX by remember { mutableFloatStateOf(0f) }
    var pressY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press && size.width > 0 && size.height > 0) {
                pressX = (interaction.pressPosition.x / size.width * 2f - 1f).coerceIn(-1f, 1f)
                pressY = (interaction.pressPosition.y / size.height * 2f - 1f).coerceIn(-1f, 1f)
            }
        }
    }

    val spec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1.0f, spec, label = "metroScale")
    val tiltX by animateFloatAsState(if (isPressed) -pressY * tiltDegrees else 0f, spec, label = "metroTiltX")
    val tiltY by animateFloatAsState(if (isPressed) pressX * tiltDegrees else 0f, spec, label = "metroTiltY")

    this
        .onSizeChanged { size = it }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationX = tiltX
            rotationY = tiltY
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
