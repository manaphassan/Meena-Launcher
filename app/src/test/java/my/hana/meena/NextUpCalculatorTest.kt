package my.hana.meena

import my.hana.meena.data.system.DeviceCalendarEvent
import my.hana.meena.data.system.NextUpCalculator
import my.hana.meena.data.system.PrayerSlotInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NextUpCalculatorTest {
    private fun at(h: Int, m: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun slot(name: String, h: Int, m: Int, solar: Boolean = false) =
        PrayerSlotInfo(name = name, time = "", sub = "", hour24 = h, minute = m, isSolar = solar)

    private fun event(title: String, start: Long, allDay: Boolean = false) =
        DeviceCalendarEvent(id = 1, title = title, startTime = start, endTime = start + 3_600_000, isAllDay = allDay)

    @Test fun noData_returnsNull() {
        assertNull(NextUpCalculator.compute(at(10, 0), emptyList(), emptyList()))
    }

    @Test fun prayerSoonerThanEvent_wins() {
        val r = NextUpCalculator.compute(at(17, 0), listOf(event("standup", at(18, 30))), listOf(slot("asar", 17, 42)))!!
        assertTrue(r.isPrayer); assertEquals("asar", r.title); assertEquals(42, r.minutesUntil(at(17, 0)))
    }

    @Test fun eventSoonerThanPrayer_wins() {
        val r = NextUpCalculator.compute(at(10, 0), listOf(event("standup", at(10, 30))), listOf(slot("zohor", 13, 10)))!!
        assertFalse(r.isPrayer); assertEquals("standup", r.title)
    }

    @Test fun allDayAndSolarAndPastAreIgnored() {
        val now = at(10, 0)
        val r = NextUpCalculator.compute(
            now,
            listOf(event("holiday", now + 1000, allDay = true), event("old", now - 1000)),
            listOf(slot("syuruk", 10, 5, solar = true), slot("subuh", 5, 40))
        )
        assertNull(r)
    }

    @Test fun formatRemaining() {
        assertEquals("in 42m", NextUpCalculator.formatRemaining(42))
        assertEquals("in 1h 05m", NextUpCalculator.formatRemaining(65))
    }
}
