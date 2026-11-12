package my.hana.meena

import my.hana.meena.data.system.ClockInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ClockInfoTest {

    private val tz = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    private fun at(text: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = tz }.parse(text)!!.time

    private val now = at("2026-11-12 23:30") // a thursday

    @Test
    fun alarmThisWeek_showsWeekdayAndTime() {
        assertEquals("fri 3:30 am", ClockInfo.formatAlarm(at("2026-11-13 03:30"), now, false, Locale.US, tz))
        assertEquals("fri 03:30", ClockInfo.formatAlarm(at("2026-11-13 03:30"), now, true, Locale.US, tz))
    }

    @Test
    fun alarmAWeekOrMoreAway_showsDate() {
        assertEquals("19 nov 07:00", ClockInfo.formatAlarm(at("2026-11-19 07:00"), now, true, Locale.US, tz))
        assertEquals("wed 07:00", ClockInfo.formatAlarm(at("2026-11-18 07:00"), now, true, Locale.US, tz))
    }

    @Test
    fun placeName_hidesPlaceholdersAndDefault() {
        assertNull(ClockInfo.placeName("kuala lumpur", isRealFix = false))
        assertNull(ClockInfo.placeName("current location", isRealFix = true))
        assertNull(ClockInfo.placeName("detecting location...", isRealFix = true))
        assertNull(ClockInfo.placeName("  ", isRealFix = true))
        assertEquals("banting", ClockInfo.placeName("Banting", isRealFix = true))
    }
}
