package my.hana.meena.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.hana.meena.theme.MeenaBorder

/**
 * Authentic Windows Phone 7/8 Metro indeterminate dotted progress indicator (§2.4):
 * - Five accent-colored dots travel across the screen in waves
 * - Non-linear staggered movement
 * - Strictly 0dp square corners (dots are tiny squares or flat rectangles)
 */
@Composable
fun MetroDottedProgress(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 4.dp,
    barHeight: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "metroDottedProgress")

    val dotCount = 5
    val dotAnimations = (0 until dotCount).map { index ->
        val delay = index * 180
        val anim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, delayMillis = delay, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dotAnim_$index"
        )
        anim
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        val widthPx = maxWidth

        dotAnimations.forEach { progress ->
            // WP Metro easing: slow at start, fast in center, slow at end
            val easedProgress = if (progress < 0.5f) {
                2f * progress * progress
            } else {
                -1f + (4f - 2f * progress) * progress
            }

            // Map progress to travel from -10dp to widthPx + 10dp
            val xPos = (widthPx + 20.dp) * easedProgress - 10.dp

            // Only render dot while in visible flight range
            if (progress in 0.01f..0.99f) {
                Box(
                    modifier = Modifier
                        .offset(x = xPos)
                        .size(dotSize)
                        .background(color)
                )
            }
        }
    }
}

/**
 * Authentic Windows Phone flat hairline determinate progress line (§2.4):
 * - 0dp corner radius
 * - Thin line (3dp or custom height)
 * - Accent color fill with dark border track
 */
@Composable
fun MetroLineProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MeenaBorder,
    height: Dp = 3.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .fillMaxHeight()
                .background(color)
        )
    }
}
