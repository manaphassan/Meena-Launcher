package my.hana.meena

import my.hana.meena.data.system.StripEvent
import my.hana.meena.data.system.WeekStripBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WeekStripTest {

    private val la = TimeZone.getTimeZone("America/Los_Angeles")

    private fun millis(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    // Fri 13 Nov 2026, 9:00 in Los Angeles.
    private val now = millis(la, 2026, Calendar.NOVEMBER, 13, 9)

    @Test
    fun sevenDays_startingToday_withWeekendFlags() {
        val days = WeekStripBuilder.build(now, emptyList(), la)
        assertEquals((13..19).toList(), days.map { it.dayOfMonth })
        assertTrue(days[0].isToday)
        assertTrue(days.drop(1).none { it.isToday })
        assertEquals(listOf(false, true, true, false, false, false, false), days.map { it.isWeekend })
        assertEquals(millis(la, 2026, Calendar.NOVEMBER, 13), days[0].startMillis)
    }

    @Test
    fun allDayEvents_areCountedByTheirUtcDate() {
        // The provider stores an all-day event on 15 Nov as 00:00 UTC, which is 14 Nov in LA.
        val allDay = StripEvent(millis(TimeZone.getTimeZone("UTC"), 2026, Calendar.NOVEMBER, 15), isAllDay = true)
        val timed = StripEvent(millis(la, 2026, Calendar.NOVEMBER, 14, 10), isAllDay = false)
        val days = WeekStripBuilder.build(now, listOf(allDay, timed), la)
        assertEquals(1, days.first { it.dayOfMonth == 14 }.eventCount)
        assertEquals(1, days.first { it.dayOfMonth == 15 }.eventCount)
    }

    @Test
    fun eventsOutsideTheSevenDays_areIgnored() {
        val before = StripEvent(millis(la, 2026, Calendar.NOVEMBER, 12, 10), isAllDay = false)
        val after = StripEvent(millis(la, 2026, Calendar.NOVEMBER, 20, 10), isAllDay = false)
        val days = WeekStripBuilder.build(now, listOf(before, after), la)
        assertEquals(0, days.sumOf { it.eventCount })
    }

    @Test
    fun daysPastTheCalendarRange_haveNoEventInfo() {
        val late = StripEvent(millis(la, 2026, Calendar.NOVEMBER, 18, 10), isAllDay = false)
        val days = WeekStripBuilder.build(now, listOf(late), la, knownDays = 3)
        assertEquals(listOf(true, true, true, false, false, false, false), days.map { it.hasEventInfo })
        assertEquals(0, days.first { it.dayOfMonth == 18 }.eventCount)
    }

    @Test
    fun monthBoundary_rollsOver() {
        val days = WeekStripBuilder.build(millis(la, 2026, Calendar.DECEMBER, 29, 9), emptyList(), la)
        assertEquals(listOf(29, 30, 31, 1, 2, 3, 4), days.map { it.dayOfMonth })
        assertEquals(2027, days.last().year)
    }
}
