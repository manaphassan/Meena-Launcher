package my.hana.meena.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The shapes the weather glyph can take, from Open-Meteo's WMO weather codes. */
enum class WeatherGlyph { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

/** WMO codes as documented at https://open-meteo.com/en/docs ("weather variable documentation"). */
fun weatherGlyphFor(code: Int): WeatherGlyph = when (code) {
    0 -> WeatherGlyph.CLEAR
    1, 2 -> WeatherGlyph.PARTLY
    3 -> WeatherGlyph.CLOUDY
    45, 48 -> WeatherGlyph.FOG
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherGlyph.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherGlyph.SNOW
    95, 96, 99 -> WeatherGlyph.STORM
    else -> WeatherGlyph.CLOUDY
}

/**
 * Thin-stroke, single-colour weather glyph with a small, calm loop: the sun's rays turn, clouds
 * drift, rain and snow fall, lightning flickers, fog slides. With [animate] off it is still.
 * Decorative: the condition is always written next to it.
 */
@Composable
fun AnimatedWeatherIcon(
    code: Int,
    isDay: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    animate: Boolean = true
) {
    val glyph = weatherGlyphFor(code)
    val phase: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "weatherGlyph").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(loopMillis(glyph), easing = LinearEasing), RepeatMode.Restart),
            label = "weatherPhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Spacer(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val strokeWidth = u.coerceIn(1.dp.toPx(), 2.dp.toPx())
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val highCloud = cloudPath(u, dx = 0f, dy = -3f)
                val cloud = cloudPath(u, dx = 0f, dy = 0f)
                val sideCloud = cloudPath(u, dx = 2f, dy = 2f)
                val moon = Path().apply {
                    op(circle(u, 12f, 12f, 7f), circle(u, 15.5f, 9f, 5.5f), PathOperation.Difference)
                }
                val bolt = Path().apply {
                    moveTo(13.5f * u, 14.5f * u)
                    lineTo(11f * u, 18.5f * u)
                    lineTo(13.5f * u, 18.5f * u)
                    lineTo(11f * u, 22.5f * u)
                }
                onDrawBehind {
                    val p = phase.value
                    val wave = sin(2 * PI * p).toFloat()
                    when (glyph) {
                        WeatherGlyph.CLEAR -> if (isDay) {
                            // Rays repeat every 45 degrees, so turning by 45 per loop looks endless.
                            drawSun(u, 12f, 12f, 4.5f, 7f, 9.5f, p * 45f, color, stroke)
                        } else {
                            rotate(wave * 6f, pivot = Offset(12f * u, 12f * u)) { drawPath(moon, color, style = stroke) }
                            val twinkle = 0.4f + 0.6f * ((cos(2 * PI * p).toFloat() + 1f) / 2f)
                            drawSpark(u, 19f, 5f, 1.2f, color.copy(alpha = color.alpha * twinkle), strokeWidth)
                        }

                        WeatherGlyph.PARTLY -> {
                            val dx = wave * 0.8f * u
                            translate(left = dx) {
                                clipPath(sideCloud, ClipOp.Difference) {
                                    translate(left = -dx) { drawSun(u, 8.5f, 8.5f, 3f, 5f, 6.5f, p * 45f, color, stroke) }
                                }
                                drawPath(sideCloud, color, style = stroke)
                            }
                        }

                        WeatherGlyph.CLOUDY -> translate(left = wave * 0.8f * u) { drawPath(cloud, color, style = stroke) }

                        WeatherGlyph.FOG -> for (i in 0 until 3) {
                            val shift = sin(2 * PI * (p + i / 3f)).toFloat() * 1.5f
                            val y = 8f + i * 4f
                            val start = if (i == 1) 3f else 5f
                            val end = if (i == 1) 19f else 21f
                            drawLine(color, Offset((start + shift) * u, y * u), Offset((end + shift) * u, y * u), strokeWidth, StrokeCap.Round)
                        }

                        WeatherGlyph.RAIN -> {
                            drawPath(highCloud, color, style = stroke)
                            for (i in 0 until 3) {
                                val local = (p + i / 3f) % 1f
                                val x = 9f + i * 4f
                                val y = 15.5f + local * 4f
                                drawLine(
                                    color.copy(alpha = color.alpha * (1f - local)),
                                    Offset(x * u, y * u),
                                    Offset((x - 1f) * u, (y + 3f) * u),
                                    strokeWidth,
                                    StrokeCap.Round
                                )
                            }
                        }

                        WeatherGlyph.SNOW -> {
                            drawPath(highCloud, color, style = stroke)
                            for (i in 0 until 3) {
                                val local = (p + i / 3f) % 1f
                                val x = 9f + i * 4f + sin(2 * PI * local).toFloat() * 0.6f
                                val y = 16.5f + local * 5f
                                drawSpark(u, x, y, 1f, color.copy(alpha = color.alpha * (1f - local)), strokeWidth)
                            }
                        }

                        WeatherGlyph.STORM -> {
                            drawPath(highCloud, color, style = stroke)
                            // Two quick flickers at the start of each loop, dim the rest of the time.
                            val flash = if (!animate || p < 0.06f || p in 0.12f..0.16f) 1f else 0.35f
                            drawPath(bolt, color.copy(alpha = color.alpha * flash), style = stroke)
                        }
                    }
                }
            }
    )
}

private fun loopMillis(glyph: WeatherGlyph): Int = when (glyph) {
    WeatherGlyph.CLEAR -> 3_000
    WeatherGlyph.RAIN -> 1_200
    WeatherGlyph.SNOW -> 3_000
    WeatherGlyph.STORM -> 4_000
    else -> 6_000
}

private fun circle(u: Float, cx: Float, cy: Float, r: Float): Path =
    Path().apply { addOval(Rect(center = Offset(cx * u, cy * u), radius = r * u)) }

/** One outline around three puffs and a flat base, in a 24-unit box shifted by [dx], [dy]. */
private fun cloudPath(u: Float, dx: Float, dy: Float): Path {
    val parts = listOf(
        circle(u, 12.5f + dx, 11.5f + dy, 4.5f),
        circle(u, 17f + dx, 14f + dy, 3f),
        Path().apply { addRect(Rect((8.5f + dx) * u, (14f + dy) * u, (17f + dx) * u, (17f + dy) * u)) }
    )
    return parts.fold(circle(u, 8.5f + dx, 14f + dy, 3f)) { acc, next ->
        Path().apply { op(acc, next, PathOperation.Union) }
    }
}

private fun DrawScope.drawSun(
    u: Float,
    cx: Float,
    cy: Float,
    radius: Float,
    rayFrom: Float,
    rayTo: Float,
    turnDegrees: Float,
    color: Color,
    stroke: Stroke
) {
    val center = Offset(cx * u, cy * u)
    drawCircle(color, radius = radius * u, center = center, style = stroke)
    rotate(turnDegrees, pivot = center) {
        for (i in 0 until 8) {
            val a = i * PI / 4
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(color, center + dir * (rayFrom * u), center + dir * (rayTo * u), stroke.width, StrokeCap.Round)
        }
    }
}

/** A small plus-shaped spark: a star at night, a flake in snow. */
private fun DrawScope.drawSpark(u: Float, cx: Float, cy: Float, r: Float, color: Color, width: Float) {
    drawLine(color, Offset((cx - r) * u, cy * u), Offset((cx + r) * u, cy * u), width, StrokeCap.Round)
    drawLine(color, Offset(cx * u, (cy - r) * u), Offset(cx * u, (cy + r) * u), width, StrokeCap.Round)
}
