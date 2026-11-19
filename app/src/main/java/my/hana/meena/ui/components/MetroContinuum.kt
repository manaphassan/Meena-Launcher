package my.hana.meena.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Continuum, the Windows Phone motion where the item you tap flies up and becomes the title of
 * the next page. Built on Compose shared bounds:
 * https://developer.android.com/develop/ui/compose/animation/shared-elements
 *
 * A screen opts in by providing [LocalContinuumScope] (from a SharedTransitionLayout) around a
 * [MetroTurnstileContainer], which provides [LocalContinuumPage]. Anywhere inside, two texts
 * marked with [continuum] and the same key on the old and new page are joined. Without both
 * scopes (or with reduced motion, when no scope is given) the modifier does nothing.
 *
 * Outside settings the same pairing works for a home sub-page: the sub-page's AnimatedVisibility
 * provides [LocalContinuumPage], and the title it came from uses [continuumSource].
 */
val LocalContinuumScope: ProvidableCompositionLocal<SharedTransitionScope?> = compositionLocalOf { null }
val LocalContinuumPage: ProvidableCompositionLocal<AnimatedVisibilityScope?> = compositionLocalOf { null }

private data class ContinuumKey(val key: Any)

private const val CONTINUUM_MS = 320

private val continuumBounds = BoundsTransform { _, _ -> tween(CONTINUUM_MS, easing = FastOutSlowInEasing) }

/** Marks a title as the same thing on two pages, so it flies from one to the other. */
@Composable
fun Modifier.continuum(key: Any): Modifier {
    val shared = LocalContinuumScope.current ?: return this
    val page = LocalContinuumPage.current ?: return this
    return with(shared) {
        this@continuum.sharedBounds(
            sharedContentState = rememberSharedContentState(ContinuumKey(key)),
            animatedVisibilityScope = page,
            enter = fadeIn(tween(CONTINUUM_MS)),
            exit = fadeOut(tween(CONTINUUM_MS)),
            boundsTransform = continuumBounds,
            // Text grows from a row title to a page title by scaling, not by re-wrapping.
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillHeight, Alignment.CenterStart)
        )
    }
}

/**
 * The other end of [continuum] for a title that stays put while a page opens over it (a home
 * widget under a sub-page), so it has no page of its own to come and go with. It hides while
 * [pageOpen] and flies back into place when the page closes. Needs only [LocalContinuumScope].
 */
@Composable
fun Modifier.continuumSource(key: Any, pageOpen: Boolean): Modifier {
    val shared = LocalContinuumScope.current ?: return this
    return with(shared) {
        this@continuumSource
            .sharedElementWithCallerManagedVisibility(
                rememberSharedContentState(ContinuumKey(key)),
                !pageOpen,
                continuumBounds
            )
            // Laid out at its final size while it flies, so the text never re-wraps.
            .skipToLookaheadSize()
    }
}
