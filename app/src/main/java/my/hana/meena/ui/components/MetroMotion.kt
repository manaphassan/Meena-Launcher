package my.hana.meena.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Windows Phone Metro UI authentic motion system (Charter §2, §7 Phase 4.5):
 * 1. Turnstile: pages turn around their left edge like a door.
 *    - Enter: the new page turns in from 30 degrees to flat, fading in
 *    - Exit: the old page turns away to -45 degrees, fading out (mirrored when going back)
 * 2. Staggered Feather-In: List items cascade in with progressive delays and subtle perspective rise.
 * 3. Reduced Motion: Strict override when settings.reducedMotion == true, snapping with zero lag.
 */

private const val TURNSTILE_ENTER_MS = 300
private const val TURNSTILE_EXIT_MS = 240
private const val TURNSTILE_ENTER_DEGREES = 30f
private const val TURNSTILE_EXIT_DEGREES = 45f

/** Fade timing for the turnstile; the turn itself comes from [turnstile]. */
fun metroTurnstileTransition(
    forward: Boolean = true,
    reducedMotion: Boolean = false
): ContentTransform {
    if (reducedMotion) {
        return fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
    }
    return metroTurnstileEnter(reducedMotion) togetherWith metroTurnstileExit(reducedMotion)
}

/** Enter half of the turnstile, for AnimatedVisibility; pair it with [turnstile] on the content. */
fun metroTurnstileEnter(reducedMotion: Boolean = false): EnterTransition =
    if (reducedMotion) fadeIn(animationSpec = snap())
    else fadeIn(animationSpec = tween(TURNSTILE_ENTER_MS, easing = FastOutSlowInEasing))

/** Exit half of the turnstile; it also keeps the page on screen while it turns away. */
fun metroTurnstileExit(reducedMotion: Boolean = false): ExitTransition =
    if (reducedMotion) fadeOut(animationSpec = snap())
    else fadeOut(animationSpec = tween(TURNSTILE_EXIT_MS, easing = FastOutLinearInEasing))

/**
 * Turns this page around its left edge as it comes and goes. [forward] is the direction of travel:
 * forward pages turn in from the right and leave to the left; going back mirrors both.
 */
@Composable
fun AnimatedVisibilityScope.turnstile(forward: Boolean = true, reducedMotion: Boolean = false): Modifier {
    if (reducedMotion) return Modifier
    val sign = if (forward) 1f else -1f
    val rotation by transition.animateFloat(
        transitionSpec = {
            if (targetState == EnterExitState.PostExit) tween(TURNSTILE_EXIT_MS, easing = FastOutLinearInEasing)
            else tween(TURNSTILE_ENTER_MS, easing = LinearOutSlowInEasing)
        },
        label = "turnstile"
    ) { state ->
        when (state) {
            EnterExitState.PreEnter -> sign * TURNSTILE_ENTER_DEGREES
            EnterExitState.Visible -> 0f
            EnterExitState.PostExit -> -sign * TURNSTILE_EXIT_DEGREES
        }
    }
    return Modifier.graphicsLayer {
        transformOrigin = TransformOrigin(0f, 0.5f)
        cameraDistance = 24f * density
        rotationY = rotation
    }
}

/** Sub-page switcher with the turnstile: the old page turns away as the new one turns in. */
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
        Box(modifier = turnstile(forward = isForward, reducedMotion = reducedMotion)) {
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
