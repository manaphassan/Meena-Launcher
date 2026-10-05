package my.hana.meena.ui

import my.hana.meena.R
import my.hana.meena.ui.components.weatherCodeToDrawableRes
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastTilesTest {

    @Test
    fun weatherCodeToDrawableRes_correctlyMapsOpenMeteoCodes() {
        // Clear
        assertEquals(R.drawable.ic_weather_clear, weatherCodeToDrawableRes(0))
        
        // Partly Cloudy
        assertEquals(R.drawable.ic_weather_partly_cloudy, weatherCodeToDrawableRes(1))
        assertEquals(R.drawable.ic_weather_partly_cloudy, weatherCodeToDrawableRes(2))
        
        // Cloudy
        assertEquals(R.drawable.ic_weather_cloudy, weatherCodeToDrawableRes(3))
        
        // Fog
        assertEquals(R.drawable.ic_weather_fog, weatherCodeToDrawableRes(45))
        assertEquals(R.drawable.ic_weather_fog, weatherCodeToDrawableRes(48))
        
        // Rain
        assertEquals(R.drawable.ic_weather_rain, weatherCodeToDrawableRes(51))
        assertEquals(R.drawable.ic_weather_rain, weatherCodeToDrawableRes(63))
        assertEquals(R.drawable.ic_weather_rain, weatherCodeToDrawableRes(80))
        
        // Storm
        assertEquals(R.drawable.ic_weather_storm, weatherCodeToDrawableRes(95))
        assertEquals(R.drawable.ic_weather_storm, weatherCodeToDrawableRes(99))
        
        // Snow -> Cloudy (per our implementation based on available vectors)
        assertEquals(R.drawable.ic_weather_cloudy, weatherCodeToDrawableRes(71))
    }
}
