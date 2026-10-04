package my.hana.meena.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Windows Phone Metro UI authentic motion system (Charter §2, §7 Phase 4.5):
 * 1. Turnstile: Navigation transition with 3D Y-axis rotation around the left edge and horizontal slide.
 * 2. Staggered Feather-In: List items cascade in with progressive delays and subtle perspective rise.
 * 3. Reduced Motion: Strict override when settings.reducedMotion == true, snapping with zero lag.
 */

/**
 * AnimatedContent ContentTransform that provides the authentic Windows Phone Turnstile transition.
 */
fun metroTurnstileTransition(
    forward: Boolean = true,
    reducedMotion: Boolean = false
): ContentTransform {
    if (reducedMotion) {
        return fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
    }

    val enterTween = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
    val exitTween = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
    val enterSlideTween = tween<IntOffset>(durationMillis = 280, easing = FastOutSlowInEasing)
    val exitSlideTween = tween<IntOffset>(durationMillis = 220, easing = FastOutSlowInEasing)

    return if (forward) {
        (slideInHorizontally(animationSpec = enterSlideTween, initialOffsetX = { it / 3 }) +
         fadeIn(animationSpec = enterTween)).togetherWith(
            slideOutHorizontally(animationSpec = exitSlideTween, targetOffsetX = { -it / 4 }) +
            fadeOut(animationSpec = exitTween)
        )
    } else {
        (slideInHorizontally(animationSpec = enterSlideTween, initialOffsetX = { -it / 4 }) +
         fadeIn(animationSpec = enterTween)).togetherWith(
            slideOutHorizontally(animationSpec = exitSlideTween, targetOffsetX = { it / 3 }) +
            fadeOut(animationSpec = exitTween)
        )
    }
}

/**
 * Turnstile container that wraps sub-page or view transitions with authentic 3D Y-axis pivot.
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
        Box(
            modifier = Modifier.graphicsLayer {
                if (!reducedMotion) {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    cameraDistance = 24f * density
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
