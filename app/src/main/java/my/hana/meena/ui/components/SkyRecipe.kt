package my.hana.meena.ui.components

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * What the weather page's sky should do, worked out from the real readings. Pure Kotlin so it
 * is unit tested; [WeatherSky] draws it.
 */
data class SkyRecipe(
    val glyph: WeatherGlyph,
    val isDay: Boolean,
    /** Falling drops or flakes on screen, 0 when dry. */
    val particles: Int,
    /** Sideways push from the wind, -1 (to the left) to 1 (to the right). */
    val windPush: Float,
    /** 0 calm to 1 gale: how much the wind gusts and how fast clouds move. */
    val windStrength: Float,
    /** Drifting specks of haze, from the air quality. */
    val haze: Int,
    /** Air quality band, 0 good to 5 hazardous; -1 when there is no reading. */
    val airBand: Int,
    val clouds: Int
)

object Sky {

    /** Particles for this weather code, raised when the gauge reads more rain. */
    fun particles(code: Int, precipitationMm: Float): Int {
        val base = when (code) {
            51 -> 40; 53 -> 60; 55 -> 80; 56, 57 -> 60
            61 -> 80; 63 -> 130; 65 -> 200; 66, 67 -> 120
            80 -> 100; 81 -> 160; 82 -> 220
            71 -> 50; 73 -> 90; 75 -> 140; 77 -> 60; 85 -> 80; 86 -> 140
            95 -> 200; 96, 99 -> 240
            else -> 0
        }
        if (base == 0) return 0
        return min(240, max(base, (precipitationMm * 60f).toInt()))
    }

    /**
     * Where the drops slant. Wind direction is where it blows *from* (Open-Meteo, degrees from
     * north), so wind from the west pushes rain to the right. Only the east-west part shows on a
     * flat screen; full push at 40 km/h and above.
     */
    fun windPush(directionDegrees: Int, speedKmh: Float): Float {
        val towardEast = -sin(directionDegrees * PI / 180.0).toFloat()
        return (towardEast * (speedKmh / 40f)).coerceIn(-1f, 1f)
    }

    fun windStrength(speedKmh: Float): Float = (speedKmh / 60f).coerceIn(0f, 1f)

    /** US AQI bands, https://www.airnow.gov/aqi/aqi-basics/ */
    fun airBand(aqi: Int?): Int = when {
        aqi == null || aqi <= 0 -> -1
        aqi <= 50 -> 0
        aqi <= 100 -> 1
        aqi <= 150 -> 2
        aqi <= 200 -> 3
        aqi <= 300 -> 4
        else -> 5
    }

    /** Haze specks: none when the air is good, thickening as it gets worse. */
    fun haze(aqi: Int?): Int = if (aqi == null || aqi <= 50) 0 else min(90, (aqi - 50) / 3)

    fun clouds(glyph: WeatherGlyph, cloudCover: Int): Int = when (glyph) {
        WeatherGlyph.CLEAR -> 0
        WeatherGlyph.PARTLY -> 2
        WeatherGlyph.FOG -> 0
        else -> if (cloudCover >= 70) 4 else 3
    }

    fun recipe(
        code: Int, isDay: Boolean, precipitationMm: Float, windKmh: Float,
        windDirection: Int, cloudCover: Int, aqi: Int?
    ): SkyRecipe {
        val glyph = weatherGlyphFor(code)
        return SkyRecipe(
            glyph = glyph,
            isDay = isDay,
            particles = particles(code, precipitationMm),
            windPush = windPush(windDirection, windKmh),
            windStrength = windStrength(windKmh),
            haze = haze(aqi),
            airBand = airBand(aqi),
            clouds = clouds(glyph, cloudCover)
        )
    }
}
