package my.hana.meena

import my.hana.meena.data.system.JakimSolatRepository
import my.hana.meena.data.system.MalaysianEnglish
import my.hana.meena.data.system.SolatZoneMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** The five fixes found while making the store screenshots (15 November 2026). */
class ScreenshotFixesTest {

    // Prayer zone from the phone's address lookup

    @Test
    fun zone_federalTerritories() {
        assertEquals("WLY01", SolatZoneMatcher.zoneFor("Wilayah Persekutuan Kuala Lumpur", listOf(null, "Kuala Lumpur", "KL Sentral")))
        assertEquals("WLY01", SolatZoneMatcher.zoneFor("Federal Territory of Kuala Lumpur", listOf("Kuala Lumpur")))
        assertEquals("WLY01", SolatZoneMatcher.zoneFor("Putrajaya", listOf("Putrajaya")))
        assertEquals("WLY02", SolatZoneMatcher.zoneFor("Labuan Federal Territory", listOf("Victoria")))
    }

    @Test
    fun zone_oneZoneStates() {
        assertEquals("MLK01", SolatZoneMatcher.zoneFor("Melaka", listOf("Alor Gajah")))
        assertEquals("PNG01", SolatZoneMatcher.zoneFor("Penang", listOf("George Town")))
        assertEquals("PLS01", SolatZoneMatcher.zoneFor("Perlis", emptyList()))
    }

    @Test
    fun zone_districtWithinState() {
        assertEquals("SGR01", SolatZoneMatcher.zoneFor("Selangor", listOf("Petaling", "Petaling Jaya", "Seksyen 14")))
        assertEquals("SGR01", SolatZoneMatcher.zoneFor("Selangor", listOf("Shah Alam")))
        assertEquals("SGR03", SolatZoneMatcher.zoneFor("Selangor", listOf("Klang", "Klang")))
        assertEquals("SGR03", SolatZoneMatcher.zoneFor("Selangor", listOf("Kuala Langat", "Banting")))
        assertEquals("JHR02", SolatZoneMatcher.zoneFor("Johor", listOf("Johor Bahru", "Johor Bahru")))
        assertEquals("PRK02", SolatZoneMatcher.zoneFor("Perak", listOf("Kinta", "Ipoh")))
        assertEquals("SWK08", SolatZoneMatcher.zoneFor("Sarawak", listOf("Kuching")))
        assertEquals("SBH07", SolatZoneMatcher.zoneFor("Sabah", listOf("Kota Kinabalu")))
    }

    @Test
    fun zone_unclearOrOutsideMalaysia_isNull() {
        assertNull(SolatZoneMatcher.zoneFor("Selangor", listOf("Somewhere New")))
        assertNull(SolatZoneMatcher.zoneFor("Selangor", emptyList()))
        assertNull(SolatZoneMatcher.zoneFor("Central Region", listOf("Singapore")))
        assertNull(SolatZoneMatcher.zoneFor(null, listOf("Kuala Lumpur")))
    }

    @Test
    fun zoneList_coversEveryState_withUniqueCodes() {
        val codes = JakimSolatRepository.AVAILABLE_ZONES.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertEquals(60, codes.size)
        listOf("JHR", "KDH", "KTN", "MLK", "NGS", "PHG", "PLS", "PNG", "PRK", "SBH", "SGR", "SWK", "TRG", "WLY")
            .forEach { prefix -> assertTrue(prefix, codes.any { it.startsWith(prefix) }) }
    }

    // Inbox times follow the 24-hour setting

    @Test
    fun clockTime_followsTheChosenClock() {
        val tz = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        val at = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = tz }.parse("2026-11-15 15:48")!!.time
        assertEquals("15:48", MalaysianEnglish.clockTime(at, is24Hour = true, timeZone = tz))
        assertEquals("3:48 pm", MalaysianEnglish.clockTime(at, is24Hour = false, timeZone = tz))
    }

    // The forecast is open on a fresh install

    @Test
    fun freshInstall_forecastIsOpen() {
        val forecast = my.hana.meena.data.model.MeenaUserSettings().widgetConfigs["widget-weather-forecast"]
        assertEquals(false, forecast?.isCollapsed)
    }
}
