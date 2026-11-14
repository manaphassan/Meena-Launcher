package my.hana.meena

import my.hana.meena.data.model.AppsSort
import my.hana.meena.data.system.AppGrouping
import my.hana.meena.data.system.BluetoothDevicesRepository
import my.hana.meena.data.system.BluetoothSignal
import my.hana.meena.data.system.DeviceAppInfo
import my.hana.meena.data.system.LocationPick
import my.hana.meena.data.system.Qibla
import my.hana.meena.data.system.SunPath
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FewFixesTest {

    private fun app(pkg: String, label: String, category: Int = -1) =
        DeviceAppInfo(packageName = pkg, label = label, firstLetter = label.first().uppercaseChar(), version = "1", category = category)

    @Test
    fun appsSort_unknownFallsBackToAz() {
        assertEquals(AppsSort.AZ, AppsSort.of("weird"))
        assertEquals(AppsSort.RECENT, AppsSort.of(AppsSort.RECENT))
    }

    @Test
    fun byCategory_namedAToZ_thenOtherLast() {
        val apps = listOf(app("a", "Zed", 0), app("b", "Chess", 0), app("c", "Notes"), app("d", "Snap", 4))
        val titles = mapOf(0 to "Games", 4 to "Social")
        val sections = AppGrouping.byCategory(apps, { titles[it] }, otherTitle = "other")
        assertEquals(listOf("games", "social", "other"), sections.map { it.title })
        assertEquals(listOf("Chess", "Zed"), sections[0].apps.map { it.label })
        assertNull(sections[0].letter)
    }

    @Test
    fun byRecent_bucketsNewestFirst_andNeverOpenedLast() {
        val now = 10L * 24 * 60 * 60 * 1000
        val startOfToday = now - 60 * 60 * 1000
        val apps = listOf(app("old", "Old"), app("week", "Week"), app("t1", "Today one"), app("t2", "Today two"), app("new", "Never"))
        val lastUsed = mapOf(
            "t1" to now - 10_000,
            "t2" to now - 1_000,
            "week" to now - 3L * 24 * 60 * 60 * 1000,
            "old" to now - 9L * 24 * 60 * 60 * 1000
        )
        val sections = AppGrouping.byRecent(apps, lastUsed, now, startOfToday) { it }
        assertEquals(
            listOf(AppGrouping.BUCKET_TODAY, AppGrouping.BUCKET_WEEK, AppGrouping.BUCKET_EARLIER, AppGrouping.BUCKET_NEVER),
            sections.map { it.title }
        )
        assertEquals(listOf("t2", "t1"), sections[0].apps.map { it.packageName })
    }

    @Test
    fun byLetter_keepsLetters() {
        val sections = AppGrouping.byLetter(listOf(app("b", "Beta"), app("a", "Alpha"), app("a2", "Apex")))
        assertEquals(listOf('A', 'B'), sections.map { it.letter })
        assertEquals(listOf("Alpha", "Apex"), sections[0].apps.map { it.label })
    }

    private data class Fix(val time: Long, val accuracy: Float)

    @Test
    fun locationPick_prefersSharpRecent_elseNewest() {
        val now = 1_000_000_000L
        val sharpOld = Fix(now - LocationPick.RECENT_MS - 1, 20f)
        val roughNew = Fix(now - 1_000, 2_000f)
        val sharpNew = Fix(now - 60_000, 300f)
        assertEquals(sharpNew, LocationPick.best(listOf(sharpOld, roughNew, sharpNew), now, { it.time }, { it.accuracy }))
        val stale = listOf(Fix(now - 3 * LocationPick.RECENT_MS, 10f), Fix(now - 2 * LocationPick.RECENT_MS, 900f))
        assertEquals(stale[1], LocationPick.best(stale, now, { it.time }, { it.accuracy }))
        assertNull(LocationPick.best(emptyList<Fix>(), now, { it.time }, { it.accuracy }))
    }

    @Test
    fun bluetoothSignal_levels() {
        assertEquals(BluetoothSignal.STRONG, BluetoothSignal.level(-50))
        assertEquals(BluetoothSignal.GOOD, BluetoothSignal.level(-70))
        assertEquals(BluetoothSignal.WEAK, BluetoothSignal.level(-90))
    }

    @Test
    fun bluetoothCompanion_findsTheBrandAppOnlyWhenInstalled() {
        val installed = setOf("com.xiaomi.wearable")
        assertEquals("com.xiaomi.wearable", BluetoothDevicesRepository.companionFor("xiaomi smart band 9 e34f") { it in installed })
        assertNull(BluetoothDevicesRepository.companionFor("xiaomi smart band 9") { false })
        assertNull(BluetoothDevicesRepository.companionFor("moondrop pill") { it in installed })
    }

    @Test
    fun qibla_bearingFromKualaLumpurIsWestNorthWest() {
        // Published qibla for Kuala Lumpur is about 292.5 degrees from true north.
        assertEquals(292.5, Qibla.bearing(3.1390, 101.6869), 0.5)
        // From due south of the Kaaba, it's straight north.
        assertEquals(0.0, Qibla.bearing(10.0, Qibla.KAABA_LNG), 0.01)
    }

    @Test
    fun sunPath_kualaLumpurMidNovember() {
        // KL, 14 Nov 2026, local midnight +08:00. Published times are about 07:03 and 19:01 [I],
        // with sunrise in the east-south-east and sunset in the west-south-west.
        val lat = 3.1390
        val lng = 101.6869
        val midnight = java.time.ZonedDateTime.of(2026, 11, 14, 0, 0, 0, 0, java.time.ZoneId.of("Asia/Kuala_Lumpur"))
            .toInstant().toEpochMilli()
        val events = SunPath.events(midnight, lat, lng)
        val riseMinutes = (events.riseMillis!! - midnight) / 60_000.0
        val setMinutes = (events.setMillis!! - midnight) / 60_000.0
        assertEquals(7 * 60 + 3.0, riseMinutes, 6.0)
        assertEquals(19 * 60 + 1.0, setMinutes, 6.0)
        assertEquals(108.0, events.riseAzimuth!!, 2.0)
        assertEquals(252.0, events.setAzimuth!!, 2.0)
        // Around 13:00 the sun is high in the south.
        val noon = SunPath.position(midnight + 13L * 3_600_000, lat, lng)
        assertEquals(180.0, noon.azimuth, 15.0)
        assertTrue(noon.elevation > 65.0)
    }

    @Test
    fun sunPath_polarNightHasNoSunrise() {
        val midnight = java.time.ZonedDateTime.of(2026, 12, 21, 0, 0, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val events = SunPath.events(midnight, 80.0, 0.0)
        assertNull(events.riseMillis)
        assertNull(events.setMillis)
    }

    @Test
    fun qibla_turnSaysWhichWayAndHowFar() {
        assertEquals(-40.0, Qibla.turn(333.0, 293.0), 1e-9)
        assertEquals(20.0, Qibla.turn(350.0, 10.0), 1e-9)
        assertEquals(0.0, Qibla.turn(293.0, 293.0), 1e-9)
    }

    @Test
    fun sunPath_runsFromSunriseToSunset() {
        val lat = 3.1390
        val lng = 101.6869
        val midnight = java.time.ZonedDateTime.of(2026, 11, 14, 0, 0, 0, 0, java.time.ZoneId.of("Asia/Kuala_Lumpur"))
            .toInstant().toEpochMilli()
        val events = SunPath.events(midnight, lat, lng)
        val path = SunPath.path(events.riseMillis!!, events.setMillis!!, lat, lng)
        assertEquals(events.riseAzimuth!!, path.first().azimuth, 0.01)
        assertEquals(events.setAzimuth!!, path.last().azimuth, 0.01)
        assertTrue(path.maxOf { it.elevation } > 65.0)
        assertTrue(SunPath.path(10L, 5L, lat, lng).isEmpty())
    }

    @Test
    fun qibla_smoothTakesTheShortWayRound() {
        assertEquals(1.0, Qibla.smooth(359.0, 3.0, 0.5), 1e-9)
        assertEquals(355.0, Qibla.smooth(5.0, 345.0, 0.5), 1e-9)
        assertEquals(10.0, Qibla.difference(355.0, 5.0), 1e-9)
        assertEquals(350.0, Qibla.normalize(-10.0), 1e-9)
    }
}
