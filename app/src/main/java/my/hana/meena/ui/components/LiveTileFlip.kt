package my.hana.meena.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import my.hana.meena.data.system.LiveFace
import my.hana.meena.theme.MeenaTextWhite

/** Seconds a face stays up, and how long before the first turn (staggered per tile). */
private const val FRONT_HOLD_MS = 7_000L
private const val BACK_HOLD_MS = 4_000L
private const val STAGGER_MS = 1_300L
private const val HALF_TURN_MS = 180

/**
 * A Windows Phone style live tile: the front (icon and name) turns over on its top edge to show
 * each live face, then turns back. It only moves while Meena is on screen and in the foreground,
 * and stays on the front when there is nothing to show or motion is reduced.
 */
@Composable
fun LiveTileFlip(
    faces: List<LiveFace>,
    animate: Boolean,
    index: Int,
    color: androidx.compose.ui.graphics.Color,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    modifier: Modifier = Modifier,
    front: @Composable BoxScope.() -> Unit
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // Hold still while a finger is on the tile, so a long press always reaches the tile menu.
    val isPressed by interactionSource.collectIsPressedAsState()
    val running = animate && faces.isNotEmpty() && !isPressed && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    var shown by remember { mutableIntStateOf(-1) } // -1 = front
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(running, faces) {
        if (!running) {
            // Finish any half turn on the face already showing instead of jumping.
            rotation.animateTo(0f, tween(HALF_TURN_MS, easing = LinearOutSlowInEasing))
            if (!(animate && faces.isNotEmpty())) shown = -1
            return@LaunchedEffect
        }
        if (shown >= faces.size) shown = -1
        delay(FRONT_HOLD_MS / 2 + index * STAGGER_MS)
        while (true) {
            val next = if (shown + 1 < faces.size) shown + 1 else -1
            rotation.animateTo(90f, tween(HALF_TURN_MS, easing = FastOutLinearInEasing))
            shown = next
            rotation.snapTo(-90f)
            rotation.animateTo(0f, tween(HALF_TURN_MS, easing = LinearOutSlowInEasing))
            delay(if (shown == -1) FRONT_HOLD_MS else BACK_HOLD_MS)
        }
    }

    // The touch target (modifier) never rotates; only the painted tile inside it does.
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationX = rotation.value
                    cameraDistance = 12f * density
                }
                .background(color)
        ) {
            val face = faces.getOrNull(shown)
            if (face == null) front() else LiveFaceContent(face)
        }
    }
}

@Composable
private fun BoxScope.LiveFaceContent(face: LiveFace) {
    val isCount = face.primary.all { it.isDigit() }
    // A time like "03:30" or a count is one big line, which leaves two lines for the words under it.
    val isShort = isCount || face.primary.length <= 8
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(my.hana.meena.theme.MeenaSpacing.s)
    ) {
        Text(
            text = face.primary,
            style = if (isShort) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isShort) FontWeight.Light else FontWeight.Normal,
            fontSize = when {
                isCount -> 28.sp
                isShort -> 22.sp
                else -> 13.sp
            },
            lineHeight = if (isShort) 30.sp else 16.sp,
            color = MeenaTextWhite,
            maxLines = if (isShort) 1 else 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    Text(
        text = face.secondary,
        style = MaterialTheme.typography.labelSmall,
        color = MeenaTextWhite,
        // A short first line leaves room for two lines here; an event title takes two lines itself.
        maxLines = if (isShort) 2 else 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(my.hana.meena.theme.MeenaSpacing.s)
    )
}
