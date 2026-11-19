package my.hana.meena.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import my.hana.meena.data.system.LiveFace
import my.hana.meena.data.system.MediaPlayerRepository
import my.hana.meena.data.system.PrivacyVeil
import my.hana.meena.theme.MeenaTextWhite

/** Seconds a face stays up, and how long before the first turn (staggered per tile). */
private const val FRONT_HOLD_MS = 7_000L
private const val BACK_HOLD_MS = 4_000L
private const val STAGGER_MS = 1_300L
private const val HALF_TURN_MS = 180
private const val PEEK_MS = 380

/** Quick start, long settle: the next face slides up and comes to rest gently. */
private val PeekEasing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1f)

/**
 * A Windows Phone style live tile. A one-cell tile turns over on its top edge to show each live
 * face, then turns back; a [wide] tile peeks instead: the next face slides up from the bottom and
 * pushes the current one off the top, as wide tiles did on Windows Phone, and a wide face uses
 * the extra room. It only moves while Meena is on screen and in the foreground, and stays on the
 * front when there is nothing to show, when motion is reduced, and while shake to hide is on.
 */
@Composable
fun LiveTileFlip(
    faces: List<LiveFace>,
    animate: Boolean,
    index: Int,
    color: androidx.compose.ui.graphics.Color,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    front: @Composable BoxScope.() -> Unit
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // Hold still while a finger is on the tile, so a long press always reaches the tile menu.
    val isPressed by interactionSource.collectIsPressedAsState()
    // Shake to hide covers names and subjects everywhere, so tiles go back to their front.
    val veiled by PrivacyVeil.hidden.collectAsState()
    val canShow = animate && faces.isNotEmpty() && !veiled
    val running = canShow && !isPressed && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    var shown by remember { mutableIntStateOf(-1) } // -1 = front
    val rotation = remember { Animatable(0f) }
    // Peek: the face leaving and how far the next one has come up (0 = just starting, 1 = in place).
    var leaving by remember { mutableIntStateOf(-1) }
    var peeking by remember { mutableStateOf(false) }
    val rise = remember { Animatable(1f) }

    LaunchedEffect(running, faces, wide) {
        if (!running) {
            if (!canShow) shown = -1
            peeking = false
            rise.snapTo(1f)
            // Finish any half turn on the face already showing instead of jumping.
            rotation.animateTo(0f, tween(HALF_TURN_MS, easing = LinearOutSlowInEasing))
            return@LaunchedEffect
        }
        if (shown >= faces.size) shown = -1
        delay(FRONT_HOLD_MS / 2 + index * STAGGER_MS)
        while (true) {
            val next = if (shown + 1 < faces.size) shown + 1 else -1
            if (wide) {
                leaving = shown
                shown = next
                rise.snapTo(0f)
                peeking = true
                rise.animateTo(1f, tween(PEEK_MS, easing = PeekEasing))
                peeking = false
            } else {
                rotation.animateTo(90f, tween(HALF_TURN_MS, easing = FastOutLinearInEasing))
                shown = next
                rotation.snapTo(-90f)
                rotation.animateTo(0f, tween(HALF_TURN_MS, easing = LinearOutSlowInEasing))
            }
            delay(if (shown == -1) FRONT_HOLD_MS else BACK_HOLD_MS)
        }
    }

    // The touch target (modifier) never moves; only the painted tile inside it does.
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationX = rotation.value
                    cameraDistance = 12f * density
                }
                .clipToBounds()
                .background(color)
        ) {
            if (peeking) {
                Box(modifier = Modifier.matchParentSize().graphicsLayer { translationY = -rise.value * size.height }) {
                    FaceOrFront(faces.getOrNull(leaving), wide, front)
                }
                Box(
                    modifier = Modifier.matchParentSize()
                        .graphicsLayer { translationY = (1f - rise.value) * size.height }) {
                    FaceOrFront(faces.getOrNull(shown), wide, front)
                }
            } else {
                FaceOrFront(faces.getOrNull(shown), wide, front)
            }
        }
    }
}

@Composable
private fun BoxScope.FaceOrFront(face: LiveFace?, wide: Boolean, front: @Composable BoxScope.() -> Unit) {
    when {
        face == null -> front()
        face.cover -> CoverFace(face, wide)
        else -> LiveFaceContent(face, wide)
    }
}

/** The playing track: on a wide tile the cover fills the left square beside the words; one cell is the cover alone. */
@Composable
private fun BoxScope.CoverFace(face: LiveFace, wide: Boolean) {
    val playing by MediaPlayerRepository.nowPlaying.collectAsState()
    val art = playing?.art
    if (art == null) {
        LiveFaceContent(face, wide)
        return
    }
    if (!wide) {
        Image(
            bitmap = art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    Row(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(my.hana.meena.theme.MeenaSpacing.s),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
        ) {
            Text(
                text = face.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                color = MeenaTextWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (face.secondary.isNotBlank()) {
                Text(
                    text = face.secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BoxScope.LiveFaceContent(face: LiveFace, wide: Boolean) {
    val isCount = face.primary.all { it.isDigit() }
    // A time like "03:30" or a count is one big line, which leaves two lines for the words under it.
    val isShort = isCount || face.primary.length <= 8
    if (wide) {
        // Wide: the big line on top, then up to two lines under it, all on the left.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(my.hana.meena.theme.MeenaSpacing.s)
        ) {
            Text(
                text = face.primary,
                style = if (isShort) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isShort) FontWeight.Light else FontWeight.Normal,
                fontSize = if (isShort) 28.sp else 15.sp,
                lineHeight = if (isShort) 30.sp else 19.sp,
                color = MeenaTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            listOf(face.secondary, face.third).filter { it.isNotBlank() }.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }
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
