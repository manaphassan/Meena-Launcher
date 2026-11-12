package my.hana.meena

import my.hana.meena.ui.widgets.Age
import my.hana.meena.ui.widgets.AgeUnit
import my.hana.meena.ui.widgets.Freshness
import org.junit.Assert.assertEquals
import org.junit.Test

class FreshnessTest {

    private val now = 1_800_000_000_000L
    private val minute = 60_000L

    @Test
    fun underAMinute_isJustNow() {
        assertEquals(Age(AgeUnit.JUST_NOW, 0), Freshness.ageOf(now - 59_000L, now))
    }

    @Test
    fun futureTimestamp_isJustNow() {
        assertEquals(Age(AgeUnit.JUST_NOW, 0), Freshness.ageOf(now + 5 * minute, now))
    }

    @Test
    fun minutesHoursDays_roundDown() {
        assertEquals(Age(AgeUnit.MINUTES, 1), Freshness.ageOf(now - minute, now))
        assertEquals(Age(AgeUnit.MINUTES, 59), Freshness.ageOf(now - 59 * minute - 59_000L, now))
        assertEquals(Age(AgeUnit.HOURS, 1), Freshness.ageOf(now - 60 * minute, now))
        assertEquals(Age(AgeUnit.HOURS, 23), Freshness.ageOf(now - 24 * 60 * minute + 1, now))
        assertEquals(Age(AgeUnit.DAYS, 1), Freshness.ageOf(now - 24 * 60 * minute, now))
        assertEquals(Age(AgeUnit.DAYS, 3), Freshness.ageOf(now - 3 * 24 * 60 * minute, now))
    }
}
