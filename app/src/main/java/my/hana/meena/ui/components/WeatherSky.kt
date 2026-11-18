package my.hana.meena.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.isActive
import my.hana.meena.theme.LocalMeenaPalette
import my.hana.meena.theme.MeenaAirGood
import my.hana.meena.theme.MeenaAirHazardous
import my.hana.meena.theme.MeenaAirModerate
import my.hana.meena.theme.MeenaAirSensitive
import my.hana.meena.theme.MeenaAirUnhealthy
import my.hana.meena.theme.MeenaAirVeryUnhealthy
import my.hana.meena.theme.MeenaSkyClearDay
import my.hana.meena.theme.MeenaSkyClearNight
import my.hana.meena.theme.MeenaSkyCloud
import my.hana.meena.theme.MeenaSkyRain
import my.hana.meena.theme.MeenaSkySnow
import my.hana.meena.theme.MeenaSkyStorm
import my.hana.meena.theme.MeenaTextWhite
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/** The colour of the air quality band from [Sky.airBand]. */
fun airBandColor(band: Int): Color = when (band) {
    0 -> MeenaAirGood
    1 -> MeenaAirModerate
    2 -> MeenaAirSensitive
    3 -> MeenaAirUnhealthy
    4 -> MeenaAirVeryUnhealthy
    5 -> MeenaAirHazardous
    else -> Color.Transparent
}

private fun skyColor(glyph: WeatherGlyph, isDay: Boolean): Color = when (glyph) {
    WeatherGlyph.CLEAR, WeatherGlyph.PARTLY -> if (isDay) MeenaSkyClearDay else MeenaSkyClearNight
    WeatherGlyph.CLOUDY, WeatherGlyph.FOG -> MeenaSkyCloud
    WeatherGlyph.RAIN -> MeenaSkyRain
    WeatherGlyph.STORM -> MeenaSkyStorm
    WeatherGlyph.SNOW -> MeenaSkySnow
}

/** Everything that moves, kept between frames. Not state: the frame clock drives redraws. */
private class SkyMotion(seed: Int) {
    val rnd = Random(seed)
    var width = 0f
    var height = 0f
    var t = 0f
    var lastNanos = 0L

    var px = FloatArray(0); var py = FloatArray(0); var pv = FloatArray(0); var pp = FloatArray(0)
    var hx = FloatArray(0); var hy = FloatArray(0); var hp = FloatArray(0)
    var sx = FloatArray(0); var sy = FloatArray(0); var sp = FloatArray(0)
    var cloudX = FloatArray(0); var cloudY = FloatArray(0); var cloudScale = FloatArray(0)

    var nextFlash = 3f
    var flashAt = -10f
    var flashX = 0.5f
    var bolt = emptyList<Offset>()
    val ripples = ArrayList<Pair<Offset, Float>>()
    var shooting: Pair<Offset, Float>? = null
    var sun = Offset.Unspecified

    fun setUp(w: Float, h: Float, recipe: SkyRecipe) {
        if (w == width && h == height && px.size == recipe.particles) return
        width = w; height = h
        val n = recipe.particles
        px = FloatArray(n) { rnd.nextFloat() * w * 1.4f - w * 0.2f }
        py = FloatArray(n) { rnd.nextFloat() * h }
        pv = FloatArray(n) { 0.75f + rnd.nextFloat() * 0.5f }
        pp = FloatArray(n) { rnd.nextFloat() * 2f * PI.toFloat() }
        hx = FloatArray(recipe.haze) { rnd.nextFloat() * w }
        hy = FloatArray(recipe.haze) { rnd.nextFloat() * h }
        hp = FloatArray(recipe.haze) { rnd.nextFloat() * 2f * PI.toFloat() }
        val stars = if (!recipe.isDay && (recipe.glyph == WeatherGlyph.CLEAR || recipe.glyph == WeatherGlyph.PARTLY)) 45 else 0
        sx = FloatArray(stars) { rnd.nextFloat() * w }
        sy = FloatArray(stars) { rnd.nextFloat() * h * 0.55f }
        sp = FloatArray(stars) { rnd.nextFloat() * 2f * PI.toFloat() }
        cloudX = FloatArray(recipe.clouds) { rnd.nextFloat() * w }
        cloudY = FloatArray(recipe.clouds) { h * (0.06f + 0.12f * it) + rnd.nextFloat() * h * 0.05f }
        cloudScale = FloatArray(recipe.clouds) { 0.7f + rnd.nextFloat() * 0.6f }
        sun = Offset(w * 0.78f, h * 0.17f)
    }

    fun newBolt(x: Float) {
        val points = ArrayList<Offset>()
        var cx = x
        var cy = 0f
        while (cy < height * 0.55f) {
            points += Offset(cx, cy)
            cx += (rnd.nextFloat() - 0.5f) * width * 0.12f
            cy += height * (0.04f + rnd.nextFloat() * 0.05f)
        }
        bolt = points
    }
}

/**
 * The weather page's moving sky, built from real readings (see [Sky.recipe]): rain or snow as
 * heavy as the forecast says and slanted by the wind, clouds drifting with it, lightning in a
 * storm, the sun or the stars when it's clear, and haze when the air is poor. The page tints
 * toward the condition at the top and toward the air quality colour at the bottom.
 *
 * Touch it: drops and flakes move out of the way of a finger, the sun leans toward it, a tap
 * sends a ripple, calls lightning in a storm and a shooting star on a clear night.
 * With [animate] off it draws one still frame.
 */
@Composable
fun WeatherSky(recipe: SkyRecipe, animate: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalMeenaPalette.current
    val ink = MeenaTextWhite
    val sky = skyColor(recipe.glyph, recipe.isDay)
    val air = airBandColor(recipe.airBand)
    val skyAlpha = if (palette.isLight) 0.16f else 0.32f
    val airAlpha = if (recipe.airBand < 0) 0f else (if (palette.isLight) 0.10f else 0.18f) * (1f + recipe.airBand * 0.25f)

    val motion = remember(recipe) { SkyMotion(recipe.hashCode()) }
    val cloudShape = remember { unitCloud() }
    var frame by remember { mutableLongStateOf(0L) }
    var touch by remember { mutableStateOf<Offset?>(null) }

    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val running = animate && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        motion.lastNanos = 0L
        while (isActive) withFrameNanos { frame = it }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(recipe) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touch = down.position
                    var moved = 0f
                    var last = down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        moved += (change.position - last).getDistance()
                        last = change.position
                        touch = change.position
                        if (!change.pressed) break
                    }
                    touch = null
                    if (moved < 24f) {
                        motion.ripples += down.position to motion.t
                        if (recipe.glyph == WeatherGlyph.STORM) {
                            motion.flashAt = motion.t
                            motion.newBolt(down.position.x)
                        } else if (!recipe.isDay && recipe.glyph == WeatherGlyph.CLEAR) {
                            motion.shooting = down.position to motion.t
                        }
                    }
                }
            }
    ) {
        frame // read so every frame redraws
        motion.setUp(size.width, size.height, recipe)
        val dt = if (!running || motion.lastNanos == 0L) 0f
        else ((frame - motion.lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        motion.lastNanos = frame
        motion.t += dt
        val t = motion.t
        val d = density

        // Tints: the condition at the top, the air at the bottom.
        drawRect(Brush.verticalGradient(listOf(sky.copy(alpha = skyAlpha), Color.Transparent), endY = size.height * 0.7f))
        if (airAlpha > 0f) {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, air.copy(alpha = airAlpha)), startY = size.height * 0.35f))
        }

        val gust = recipe.windPush + sin(t * 0.7f) * 0.25f * recipe.windStrength
        drawSunOrStars(motion, recipe, ink, t, touch, dt)
        drawClouds(motion, cloudShape, ink, t, recipe, dt)
        when (recipe.glyph) {
            WeatherGlyph.RAIN, WeatherGlyph.STORM -> drawRain(motion, ink, gust, touch, dt, d)
            WeatherGlyph.SNOW -> drawSnow(motion, ink, gust, touch, t, dt, d)
            WeatherGlyph.FOG -> drawFog(ink, t)
            else -> Unit
        }
        drawHaze(motion, air.takeIf { recipe.airBand >= 0 } ?: ink, t, dt, d)
        if (recipe.glyph == WeatherGlyph.STORM) drawLightning(motion, ink, t, running)
        drawRipples(motion, ink, t, d)
    }
}

private fun DrawScope.drawRain(m: SkyMotion, ink: Color, gust: Float, touch: Offset?, dt: Float, d: Float) {
    val fall = 900f * d / 2.75f // px per second at speed 1
    val reach = 72f * d
    for (i in m.px.indices) {
        val vy = fall * m.pv[i] * 1.3f
        val vx = vy * gust * 0.55f
        m.py[i] += vy * dt
        m.px[i] += vx * dt
        if (touch != null) {
            val dx = m.px[i] - touch.x
            if (abs(dx) < reach && m.py[i] in (touch.y - reach * 2)..touch.y) m.px[i] += sign(dx) * (reach - abs(dx)) * 0.25f
        }
        if (m.py[i] > size.height) {
            m.py[i] = -20f * d
            m.px[i] = m.rnd.nextFloat() * size.width * 1.4f - size.width * 0.2f
        }
        val len = 14f * d * m.pv[i]
        val tail = Offset(m.px[i] - vx / vy * len, m.py[i] - len)
        drawLine(ink.copy(alpha = 0.28f), tail, Offset(m.px[i], m.py[i]), 1.2f * d, StrokeCap.Round)
    }
}

private fun DrawScope.drawSnow(m: SkyMotion, ink: Color, gust: Float, touch: Offset?, t: Float, dt: Float, d: Float) {
    val fall = 70f * d
    val reach = 80f * d
    val flake = 3f * d
    for (i in m.px.indices) {
        m.py[i] += fall * m.pv[i] * dt
        m.px[i] += (sin(t * 1.3f + m.pp[i]) * 22f * d + gust * fall * 1.6f) * dt
        if (touch != null) {
            val delta = Offset(m.px[i], m.py[i]) - touch
            val dist = delta.getDistance()
            if (dist in 0.1f..reach) {
                val push = (reach - dist) / reach * 220f * d * dt
                m.px[i] += delta.x / dist * push
                m.py[i] += delta.y / dist * push
            }
        }
        if (m.py[i] > size.height) { m.py[i] = -flake; m.px[i] = m.rnd.nextFloat() * size.width }
        if (m.px[i] < -size.width * 0.2f) m.px[i] += size.width * 1.4f
        if (m.px[i] > size.width * 1.2f) m.px[i] -= size.width * 1.4f
        // Square flakes, in keeping with square corners everywhere else.
        drawRect(ink.copy(alpha = 0.45f), Offset(m.px[i], m.py[i]), Size(flake, flake))
    }
}

private fun DrawScope.drawFog(ink: Color, t: Float) {
    for (i in 0 until 7) {
        val y = size.height * (0.18f + i * 0.1f)
        val shift = sin(t * (0.15f + i * 0.03f) + i) * size.width * 0.12f
        drawLine(ink.copy(alpha = 0.10f), Offset(-size.width * 0.2f + shift, y), Offset(size.width * 0.9f + shift, y), 10f * density, StrokeCap.Round)
    }
}

private fun DrawScope.drawHaze(m: SkyMotion, color: Color, t: Float, dt: Float, d: Float) {
    val speck = 2f * d
    for (i in m.hx.indices) {
        m.hx[i] += sin(t * 0.4f + m.hp[i]) * 6f * d * dt
        m.hy[i] += cos(t * 0.3f + m.hp[i]) * 4f * d * dt
        drawRect(color.copy(alpha = 0.35f), Offset(m.hx[i], m.hy[i]), Size(speck, speck))
    }
}

private fun DrawScope.drawClouds(m: SkyMotion, shape: Path, ink: Color, t: Float, recipe: SkyRecipe, dt: Float) {
    val speed = (6f + 40f * recipe.windStrength) * density * (if (recipe.windPush < 0) -1f else 1f)
    for (i in m.cloudX.indices) {
        m.cloudX[i] += speed * dt * (0.6f + 0.2f * i)
        val scale = size.width / 24f * 0.55f * m.cloudScale[i]
        val w = 24f * scale
        if (m.cloudX[i] > size.width + w * 0.2f) m.cloudX[i] = -w
        if (m.cloudX[i] < -w * 1.2f) m.cloudX[i] = size.width
        withTransform({
            translate(m.cloudX[i], m.cloudY[i])
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(shape, ink.copy(alpha = 0.16f), style = Stroke(width = 1.5f * density / scale))
        }
    }
}

private fun DrawScope.drawSunOrStars(m: SkyMotion, recipe: SkyRecipe, ink: Color, t: Float, touch: Offset?, dt: Float) {
    val clear = recipe.glyph == WeatherGlyph.CLEAR || recipe.glyph == WeatherGlyph.PARTLY
    if (!clear) return
    if (recipe.isDay) {
        val home = Offset(size.width * 0.78f, size.height * 0.17f)
        val target = if (touch != null) home + (touch - home) * 0.15f else home
        m.sun = if (m.sun == Offset.Unspecified) home else m.sun + (target - m.sun) * (dt * 4f).coerceAtMost(1f)
        val r = 34f * density
        val color = ink.copy(alpha = if (recipe.glyph == WeatherGlyph.PARTLY) 0.22f else 0.34f)
        drawCircle(color, r, m.sun, style = Stroke(1.5f * density))
        rotate(t * 8f, pivot = m.sun) {
            for (i in 0 until 12) {
                val a = i * PI / 6
                val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                val pulse = 1f + 0.12f * sin(t * 2f + i)
                drawLine(color, m.sun + dir * (r * 1.35f), m.sun + dir * (r * 1.35f + 16f * density * pulse), 1.5f * density, StrokeCap.Round)
            }
        }
    } else {
        for (i in m.sx.indices) {
            val twinkle = 0.25f + 0.35f * ((sin(t * 1.5f + m.sp[i]) + 1f) / 2f)
            val s = 1.5f * density
            drawRect(ink.copy(alpha = twinkle), Offset(m.sx[i] - s / 2, m.sy[i] - s / 2), Size(s, s))
        }
        val shot = m.shooting
        if (shot != null) {
            val age = t - shot.second
            if (age > 0.9f) m.shooting = null else {
                val head = shot.first + Offset(age * 420f, age * 160f) * density
                drawLine(ink.copy(alpha = 0.6f * (1f - age / 0.9f)), head - Offset(60f, 23f) * density, head, 1.5f * density, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawLightning(m: SkyMotion, ink: Color, t: Float, running: Boolean) {
    if (running && t >= m.nextFlash) {
        m.flashAt = t
        m.newBolt(size.width * (0.15f + m.rnd.nextFloat() * 0.7f))
        m.nextFlash = t + 3f + m.rnd.nextFloat() * 6f
    }
    val age = t - m.flashAt
    if (age !in 0f..0.45f) return
    val strength = 1f - age / 0.45f
    // A second, smaller flicker, like real lightning.
    val flicker = if (age in 0.12f..0.2f) 0.4f else 1f
    drawRect(ink.copy(alpha = 0.10f * strength * flicker))
    for (i in 0 until m.bolt.size - 1) {
        drawLine(ink.copy(alpha = 0.7f * strength * flicker), m.bolt[i], m.bolt[i + 1], 2f * density, StrokeCap.Round)
    }
}

private fun DrawScope.drawRipples(m: SkyMotion, ink: Color, t: Float, d: Float) {
    m.ripples.removeAll { t - it.second > 0.8f }
    for ((at, start) in m.ripples) {
        val age = (t - start) / 0.8f
        drawCircle(ink.copy(alpha = 0.4f * (1f - age)), 8f * d + 64f * d * age, at, style = Stroke(1.2f * d))
    }
}

/** The same three-puff cloud as the weather glyph, in a 24-unit box. */
private fun unitCloud(): Path {
    fun circle(cx: Float, cy: Float, r: Float) = Path().apply { addOval(Rect(center = Offset(cx, cy), radius = r)) }
    val parts = listOf(
        circle(12.5f, 11.5f, 4.5f),
        circle(17f, 14f, 3f),
        Path().apply { addRect(Rect(8.5f, 14f, 17f, 17f)) }
    )
    return parts.fold(circle(8.5f, 14f, 3f)) { acc, next -> Path().apply { op(acc, next, PathOperation.Union) } }
}
