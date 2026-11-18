package my.hana.meena.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/*
 * Thin-stroke, single-colour icons that move: drawn in a 24-unit box like the weather glyph,
 * so they match Meena's other icons. Each one is still until something happens, so nothing
 * loops on the home screen and drains the battery.
 */

private fun DrawScope.u() = size.minDimension / 24f
private fun DrawScope.p(x: Float, y: Float) = Offset(x * u(), y * u())
private fun DrawScope.thin() = Stroke(width = (1.6f * u()).coerceIn(1.dp.toPx(), 2.dp.toPx()), cap = StrokeCap.Round, join = StrokeJoin.Round)

/** The quick actions on today. */
enum class QuickActionKind { PHONE, MESSAGES, EMAIL, CAMERA, CALCULATOR }

/**
 * A quick action icon. It settles in when today opens ([index] staggers them) and acts out its
 * job while pressed: the phone tips and rings, the message dots bounce, the envelope opens, the
 * camera shutter closes, the calculator types a digit.
 */
@Composable
fun QuickActionIcon(
    kind: QuickActionKind,
    color: Color,
    interactionSource: MutableInteractionSource,
    index: Int,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "quickActionPress"
    )
    val appear = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) { appear.snapTo(1f); return@LaunchedEffect }
        delay(index * 70L)
        appear.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                alpha = appear.value
                val s = 0.6f + 0.4f * appear.value
                scaleX = s; scaleY = s
            }
    ) {
        val stroke = thin()
        when (kind) {
            QuickActionKind.PHONE -> {
                rotate(-18f * press, pivot = p(12f, 12f)) {
                    val handset = Path().apply {
                        moveTo(7f * u(), 3.5f * u())
                        lineTo(10f * u(), 3.5f * u())
                        lineTo(11f * u(), 8f * u())
                        lineTo(9f * u(), 9.5f * u())
                        cubicTo(10f * u(), 12f * u(), 12f * u(), 14f * u(), 14.5f * u(), 15f * u())
                        lineTo(16f * u(), 13f * u())
                        lineTo(20.5f * u(), 14f * u())
                        lineTo(20.5f * u(), 17f * u())
                        cubicTo(20.5f * u(), 19f * u(), 19f * u(), 20.5f * u(), 17f * u(), 20.5f * u())
                        cubicTo(10f * u(), 20.5f * u(), 3.5f * u(), 14f * u(), 3.5f * u(), 7f * u())
                        cubicTo(3.5f * u(), 5f * u(), 5f * u(), 3.5f * u(), 7f * u(), 3.5f * u())
                        close()
                    }
                    drawPath(handset, color, style = stroke)
                }
                // Ring lines, only while pressed.
                if (press > 0.05f) {
                    val c = color.copy(alpha = color.alpha * press.coerceIn(0f, 1f))
                    drawArc(c, -80f, 70f, false, p(14f, 3f), Size(7f * u(), 7f * u()), style = stroke)
                    drawArc(c, -80f, 70f, false, p(12f, 1f), Size(11f * u(), 11f * u()), style = stroke)
                }
            }

            QuickActionKind.MESSAGES -> {
                val bubble = Path().apply {
                    moveTo(3f * u(), 4f * u()); lineTo(21f * u(), 4f * u()); lineTo(21f * u(), 16f * u())
                    lineTo(11f * u(), 16f * u()); lineTo(7f * u(), 20f * u()); lineTo(7f * u(), 16f * u())
                    lineTo(3f * u(), 16f * u()); close()
                }
                drawPath(bubble, color, style = stroke)
                for (i in 0 until 3) {
                    val lift = ((press * 3f - i).coerceIn(0f, 1f)) * 2.5f
                    val dot = 1.8f * u()
                    drawRect(color, p(7.1f + i * 4f, 9.1f - lift), Size(dot, dot))
                }
            }

            QuickActionKind.EMAIL -> {
                drawRect(color, p(3f, 7f), Size(18f * u(), 12f * u()), style = stroke)
                // The flap opens upward as the finger presses.
                val apex = 13.5f - 12.5f * press.coerceIn(0f, 1.2f)
                drawLine(color, p(3f, 7f), p(12f, apex), stroke.width, StrokeCap.Round)
                drawLine(color, p(21f, 7f), p(12f, apex), stroke.width, StrokeCap.Round)
            }

            QuickActionKind.CAMERA -> {
                drawRect(color, p(3f, 7f), Size(18f * u(), 12f * u()), style = stroke)
                val hump = Path().apply {
                    moveTo(8f * u(), 7f * u()); lineTo(9.5f * u(), 4.5f * u())
                    lineTo(14.5f * u(), 4.5f * u()); lineTo(16f * u(), 7f * u())
                }
                drawPath(hump, color, style = stroke)
                // The shutter closes while pressed.
                val lens = (4f - 2.4f * press.coerceIn(0f, 1f)) * u()
                drawCircle(color, lens, p(12f, 13f), style = stroke)
                if (press > 0.05f) {
                    val c = color.copy(alpha = color.alpha * press.coerceIn(0f, 1f))
                    drawLine(c, p(17.5f, 9.5f), p(19.5f, 9.5f), stroke.width, StrokeCap.Round)
                    drawLine(c, p(18.5f, 8.5f), p(18.5f, 10.5f), stroke.width, StrokeCap.Round)
                }
            }

            QuickActionKind.CALCULATOR -> {
                drawRect(color, p(5f, 3f), Size(14f * u(), 18f * u()), style = stroke)
                drawRect(color, p(7.5f, 5.5f), Size(9f * u(), 3.5f * u()), style = stroke)
                // A digit appears on the display as you press.
                val typed = 6f * press.coerceIn(0f, 1f)
                if (typed > 0.1f) drawLine(color, p(15f - typed, 7.25f), p(15f, 7.25f), stroke.width, StrokeCap.Round)
                for (row in 0 until 3) for (col in 0 until 3) {
                    val k = 1.6f * u()
                    drawRect(color, p(8.2f + col * 3.5f, 11.5f + row * 3f), Size(k, k), style = stroke)
                }
            }
        }
    }
}

/**
 * An arrow that points where the wind is blowing, turning smoothly when the reading changes and
 * swaying a little with gusts ([strength] 0 calm to 1 gale). [fromDegrees] is where the wind comes
 * from, as weather services give it.
 */
@Composable
fun WindArrow(fromDegrees: Int, strength: Float, color: Color, animate: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val heading by animateFloatAsState(
        targetValue = (fromDegrees + 180f) % 360f,
        animationSpec = tween(if (animate) 900 else 0, easing = FastOutSlowInEasing),
        label = "windHeading"
    )
    val sway = if (animate && strength > 0.02f) {
        rememberInfiniteTransition(label = "windSway").animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween((1600 - 900 * strength).toInt(), easing = LinearEasing), RepeatMode.Reverse),
            label = "windSwayValue"
        ).value
    } else 0f
    Canvas(modifier = modifier.size(size)) {
        val stroke = thin()
        rotate(heading + sway * (3f + 12f * strength), pivot = p(12f, 12f)) {
            drawLine(color, p(12f, 20f), p(12f, 4f), stroke.width, StrokeCap.Round)
            drawLine(color, p(8f, 8f), p(12f, 4f), stroke.width, StrokeCap.Round)
            drawLine(color, p(16f, 8f), p(12f, 4f), stroke.width, StrokeCap.Round)
        }
    }
}

/** Play and pause as one outline that folds between the two shapes. */
@Composable
fun AnimatedPlayPause(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier, size: Dp = 36.dp, animate: Boolean = true) {
    val t by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(if (animate) 220 else 0, easing = FastOutSlowInEasing),
        label = "playPause"
    )
    Canvas(modifier = modifier.size(size)) {
        // Play is two halves of the triangle; pause is two bars. Corners move from one to the other.
        val playLeft = listOf(7f to 5f, 13f to 8.5f, 13f to 15.5f, 7f to 19f)
        val pauseLeft = listOf(6f to 5f, 10f to 5f, 10f to 19f, 6f to 19f)
        val playRight = listOf(13f to 8.5f, 19f to 12f, 19f to 12f, 13f to 15.5f)
        val pauseRight = listOf(14f to 5f, 18f to 5f, 18f to 19f, 14f to 19f)
        val stroke = thin()
        for ((from, to) in listOf(playLeft to pauseLeft, playRight to pauseRight)) {
            val path = Path()
            from.indices.forEach { i ->
                val x = from[i].first + (to[i].first - from[i].first) * t
                val y = from[i].second + (to[i].second - from[i].second) * t
                if (i == 0) path.moveTo(x * u(), y * u()) else path.lineTo(x * u(), y * u())
            }
            path.close()
            drawPath(path, color, style = stroke)
        }
    }
}
