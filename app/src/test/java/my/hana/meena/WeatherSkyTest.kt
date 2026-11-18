package my.hana.meena

import my.hana.meena.ui.components.Sky
import my.hana.meena.ui.components.WeatherGlyph
import my.hana.meena.ui.hubs.compassIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherSkyTest {

    @Test
    fun rain_getsHeavierWithTheCodeAndTheGauge() {
        assertEquals(0, Sky.particles(0, 0f)) // clear
        assertEquals(0, Sky.particles(3, 2f)) // cloudy: a reading alone doesn't make drops
        assertTrue(Sky.particles(61, 0f) < Sky.particles(65, 0f)) // light vs heavy rain
        assertEquals(180, Sky.particles(61, 3f)) // 3 mm raises light rain
        assertEquals(240, Sky.particles(65, 50f)) // capped
    }

    @Test
    fun wind_pushesRainTheWayItBlows() {
        // From the west (270): blows east, so drops lean right.
        assertEquals(1f, Sky.windPush(270, 40f), 0.001f)
        // From the east (90): lean left, half strength at 20 km/h.
        assertEquals(-0.5f, Sky.windPush(90, 20f), 0.001f)
        // From the north: no sideways push on a flat screen.
        assertEquals(0f, Sky.windPush(0, 50f), 0.001f)
        assertEquals(1f, Sky.windPush(270, 200f), 0.001f) // clamped
    }

    @Test
    fun airQuality_bandsFollowUsAqi() {
        assertEquals(-1, Sky.airBand(null))
        assertEquals(-1, Sky.airBand(0))
        assertEquals(0, Sky.airBand(50))
        assertEquals(1, Sky.airBand(51))
        assertEquals(2, Sky.airBand(150))
        assertEquals(3, Sky.airBand(151))
        assertEquals(4, Sky.airBand(300))
        assertEquals(5, Sky.airBand(301))
        assertEquals(0, Sky.haze(40))
        assertTrue(Sky.haze(200) > Sky.haze(80))
    }

    @Test
    fun recipe_combinesTheReadings() {
        val storm = Sky.recipe(95, isDay = false, precipitationMm = 0f, windKmh = 30f, windDirection = 270, cloudCover = 100, aqi = 120)
        assertEquals(WeatherGlyph.STORM, storm.glyph)
        assertTrue(storm.particles > 0)
        assertTrue(storm.windPush > 0f)
        assertEquals(2, storm.airBand)
        assertEquals(4, storm.clouds)
    }

    @Test
    fun compass_namesTheNearestPoint() {
        assertEquals(0, compassIndex(0))
        assertEquals(0, compassIndex(350))
        assertEquals(1, compassIndex(40))
        assertEquals(2, compassIndex(90))
        assertEquals(6, compassIndex(270))
        assertEquals(7, compassIndex(315))
        assertEquals(0, compassIndex(-10))
    }
}
