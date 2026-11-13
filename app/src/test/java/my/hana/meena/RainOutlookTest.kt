package my.hana.meena

import my.hana.meena.data.system.RainHour
import my.hana.meena.data.system.RainOutlook
import my.hana.meena.data.system.RainOutlookCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RainOutlookTest {

    private val hour = 3_600_000L

    // 12:20; Open-Meteo's "13:00" hour covers 12:00 to 13:00.
    private val noon = 1_794_542_400_000L
    private val now = noon + 20 * 60_000L

    private fun hours(vararg probs: Int) =
        probs.mapIndexed { i, p -> RainHour(endMillis = noon + i * hour, probability = p, millimetres = 0.1f) }

    @Test
    fun dry_whenNoHourIsLikely() {
        // The 12:00 hour has already ended and is ignored.
        assertEquals(RainOutlook.Dry, RainOutlookCalculator.compute(hours(90, 20, 10, 40, 95), now))
    }

    @Test
    fun allWet_whenEveryHourAheadIsLikely() {
        assertEquals(RainOutlook.AllWet, RainOutlookCalculator.compute(hours(0, 90, 91, 85), now))
    }

    @Test
    fun wetUntil_endsAtTheLastLikelyHour() {
        // Likely in the hours ending 13:00 and 14:00, dry after: "likely till 14:00".
        assertEquals(RainOutlook.WetUntil(noon + 2 * hour), RainOutlookCalculator.compute(hours(0, 90, 70, 20), now))
    }

    @Test
    fun wetFrom_startsAtTheBeginningOfTheFirstLikelyHour() {
        // Dry until 14:00, then the hour ending 15:00 is likely: "likely from 14:00".
        assertEquals(RainOutlook.WetFrom(noon + 2 * hour), RainOutlookCalculator.compute(hours(0, 10, 30, 80), now))
    }

    @Test
    fun hidesWhenTheForecastIsTooOld() {
        assertNull(RainOutlookCalculator.compute(hours(90, 90), now + 2 * hour))
        assertNull(RainOutlookCalculator.compute(emptyList(), now))
    }
}
