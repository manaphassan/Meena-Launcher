package my.hana.meena

import my.hana.meena.data.system.ClockInfo
import my.hana.meena.data.system.MalaysianEnglish
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
    fun alarmSoon_saysTodayOrTomorrow() {
        assertEquals("tomorrow 3:30 am", ClockInfo.formatAlarm(at("2026-11-13 03:30"), now, false, tz))
        assertEquals("tomorrow 03:30", ClockInfo.formatAlarm(at("2026-11-13 03:30"), now, true, tz))
        assertEquals("today 23:45", ClockInfo.formatAlarm(at("2026-11-12 23:45"), now, true, tz))
    }

    @Test
    fun alarmLaterThisWeek_showsFullWeekday_thenDateAfterAWeek() {
        assertEquals("wednesday 07:00", ClockInfo.formatAlarm(at("2026-11-18 07:00"), now, true, tz))
        assertEquals("19 november 07:00", ClockInfo.formatAlarm(at("2026-11-19 07:00"), now, true, tz))
    }

    @Test
    fun hijri_usesJakimMonthNames() {
        assertEquals("3 jamadilakhir 1448h", MalaysianEnglish.hijriDate(3, 6, 1448))
        assertEquals("1 syawal 1448h", MalaysianEnglish.hijriDate(1, 10, 1448))
        assertNull(MalaysianEnglish.hijriDate(1, 13, 1448))
    }

    @Test
    fun dayName_wholeWords() {
        assertEquals("today", MalaysianEnglish.dayName(at("2026-11-12 08:00"), now, tz))
        assertEquals("tomorrow", MalaysianEnglish.dayName(at("2026-11-13 08:00"), now, tz))
        assertEquals("sunday", MalaysianEnglish.dayName(at("2026-11-15 08:00"), now, tz))
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
