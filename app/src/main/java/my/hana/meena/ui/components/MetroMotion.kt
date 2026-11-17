package my.hana.meena.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Windows Phone Metro UI authentic motion system (Charter §2, §7 Phase 4.5):
 * 1. Turnstile: Navigation transition with 3D Y-axis rotation around the left edge.
 *    - Enter: rotationY from +30 to 0 degrees with fade-in (page swings in like a door)
 *    - Exit: fade-out only for now
 * 2. Staggered Feather-In: List items cascade in with progressive delays and subtle perspective rise.
 * 3. Reduced Motion: Strict override when settings.reducedMotion == true, snapping with zero lag.
 */

/**
 * AnimatedContent ContentTransform that provides the authentic Windows Phone Turnstile transition.
 * Uses fade only — the actual 3D rotation is applied by [MetroTurnstileContainer]'s graphicsLayer.
 */
fun metroTurnstileTransition(
    forward: Boolean = true,
    reducedMotion: Boolean = false
): ContentTransform {
    if (reducedMotion) {
        return fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
    }

    val enterDuration = 300
    val exitDuration = 240

    return if (forward) {
        fadeIn(
            animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)
        ) togetherWith fadeOut(
            animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)
        )
    } else {
        fadeIn(
            animationSpec = tween(enterDuration, easing = FastOutSlowInEasing)
        ) togetherWith fadeOut(
            animationSpec = tween(exitDuration, easing = FastOutSlowInEasing)
        )
    }
}

/**
 * Turnstile container that wraps sub-page or view transitions with authentic 3D Y-axis pivot.
 * The entering page rotates from 30 degrees (or -30 going back) to 0; the exiting page only fades.
 */
@Composable
fun <T> MetroTurnstileContainer(
    targetState: T,
    modifier: Modifier = Modifier,
    isForward: Boolean = true,
    reducedMotion: Boolean = false,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            metroTurnstileTransition(
                forward = isForward,
                reducedMotion = reducedMotion
            )
        },
        label = "metroTurnstileContainer"
    ) { state ->
        // Animate rotationY for the 3D turnstile effect
        val rotationAnim = remember { Animatable(if (reducedMotion) 0f else if (isForward) 30f else -30f) }

        LaunchedEffect(state) {
            if (!reducedMotion) {
                rotationAnim.snapTo(if (isForward) 30f else -30f)
                rotationAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                )
            }
        }

        Box(
            modifier = Modifier.graphicsLayer {
                if (!reducedMotion) {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    cameraDistance = 24f * density
                    rotationY = rotationAnim.value
                }
            }
        ) {
            content(state)
        }
    }
}

/**
 * Staggered feather-in modifier for list items, cards, and tiles.
 * Cascades element into view with a progressive delay, slight vertical rise, and subtle 3D pitch.
 * When reducedMotion is true, immediately returns without delay or translation.
 */
fun Modifier.metroFeatherIn(
    index: Int,
    reducedMotion: Boolean = false,
    delayPerItemMs: Int = 25,
    durationMs: Int = 220,
    slideOffsetDp: Dp = 20.dp
): Modifier = composed {
    if (reducedMotion) return@composed this

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val totalDelay = (index * delayPerItemMs).toLong().coerceAtMost(350L)
        if (totalDelay > 0) {
            delay(totalDelay)
        }
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = FastOutSlowInEasing
            )
        )
    }

    this.graphicsLayer {
        val progress = animatable.value
        alpha = progress
        translationY = (1f - progress) * slideOffsetDp.toPx()
        transformOrigin = TransformOrigin(0f, 0.5f)
        rotationY = (1f - progress) * 8f
        cameraDistance = 20f * density
    }
}
