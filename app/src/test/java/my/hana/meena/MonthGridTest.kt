package my.hana.meena

import my.hana.meena.data.system.MonthGridBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MonthGridTest {

    private val tz = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

    @Test
    fun november2026_startsOnSundayWithIsoWeeks() {
        val grid = MonthGridBuilder.build(2026, Calendar.NOVEMBER, tz)
        // Nov 1 2026 is a Sunday: six empty slots, then 1.
        assertEquals(listOf(null, null, null, null, null, null, 1), grid.weeks.first().days)
        assertEquals(44, grid.weeks.first().weekNumber)
        assertEquals(45, grid.weeks[1].weekNumber) // Mon 2 Nov
        assertEquals(listOf(30, null, null, null, null, null, null), grid.weeks.last().days)
        assertEquals(49, grid.weeks.last().weekNumber)
        assertEquals(6, grid.weeks.size)
    }

    @Test
    fun january2027_firstWeekBelongsToPreviousIsoYear() {
        // Jan 1 2027 is a Friday, so its week is ISO week 53 of 2026.
        val grid = MonthGridBuilder.build(2027, Calendar.JANUARY, tz)
        assertEquals(53, grid.weeks.first().weekNumber)
        assertNull(grid.weeks.first().days[0])
        assertEquals(1, grid.weeks.first().days[4])
    }

    @Test
    fun eventDays_keepsOnlyThisMonth() {
        val cal = Calendar.getInstance(tz)
        fun at(y: Int, m: Int, d: Int) = cal.apply { clear(); set(y, m, d, 9, 0) }.timeInMillis
        val days = MonthGridBuilder.eventDays(
            listOf(at(2026, Calendar.NOVEMBER, 14), at(2026, Calendar.NOVEMBER, 18), at(2026, Calendar.DECEMBER, 4)),
            2026, Calendar.NOVEMBER, tz
        )
        assertEquals(setOf(14, 18), days)
    }
}
