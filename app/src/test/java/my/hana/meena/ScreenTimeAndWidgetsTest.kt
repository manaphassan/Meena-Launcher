package my.hana.meena

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.ScreenTimeCalc
import my.hana.meena.data.system.ScreenTimeCalc.Event
import my.hana.meena.data.system.ScreenTimeCalc.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimeAndWidgetsTest {

    private val min = 60_000L

    private fun start(pkg: String, at: Long, cls: String = "Main") = Event(pkg, cls, Kind.START, at)
    private fun stop(pkg: String, at: Long, cls: String = "Main") = Event(pkg, cls, Kind.STOP, at)

    @Test
    fun simpleSessions_addUp() {
        val events = listOf(
            start("chat", 0), stop("chat", 10 * min),
            start("chat", 20 * min), stop("chat", 25 * min),
            start("maps", 30 * min), stop("maps", 33 * min)
        )
        val total = ScreenTimeCalc.foregroundMillis(events, 0, 60 * min)
        assertEquals(15 * min, total["chat"])
        assertEquals(3 * min, total["maps"])
    }

    @Test
    fun twoActivitiesOfOneApp_areNotCountedTwice() {
        val events = listOf(
            start("chat", 0, "List"),
            start("chat", 2 * min, "Thread"), // opens a chat on top
            stop("chat", 3 * min, "List"),
            stop("chat", 8 * min, "Thread")
        )
        assertEquals(8 * min, ScreenTimeCalc.foregroundMillis(events, 0, 60 * min)["chat"])
    }

    @Test
    fun appOpenAtMidnight_countsFromMidnight() {
        val events = listOf(stop("video", 5 * min))
        assertEquals(5 * min, ScreenTimeCalc.foregroundMillis(events, 0, 60 * min)["video"])
    }

    @Test
    fun appStillOpen_countsUntilNow() {
        val events = listOf(start("reader", 50 * min))
        assertEquals(10 * min, ScreenTimeCalc.foregroundMillis(events, 0, 60 * min)["reader"])
    }

    @Test
    fun screenOff_endsTheSession() {
        val events = listOf(
            start("game", 0),
            Event("", "", Kind.SCREEN_OFF, 4 * min),
            stop("game", 30 * min) // Android sends the pause late; it must not add 26 minutes
        )
        assertEquals(4 * min, ScreenTimeCalc.foregroundMillis(events, 0, 60 * min)["game"])
    }

    @Test
    fun ignoredPackages_andOutOfOrderEvents() {
        val events = listOf(
            stop("chat", 9 * min), start("my.hana.meena", 0), start("chat", 1 * min), stop("my.hana.meena", 1 * min)
        )
        val total = ScreenTimeCalc.foregroundMillis(events, 0, 60 * min, ignore = setOf("my.hana.meena"))
        assertEquals(8 * min, total["chat"])
        assertNull(total["my.hana.meena"])
    }

    @Test
    fun latePauseOfAnEarlierWindow_isNotCountedFromMidnight() {
        // Seen on a TECNO KL8: the photo picker's first event today was a pause at 07:29 for a
        // window opened earlier, and the old code counted it from midnight (7h 30m).
        val events = listOf(
            start("chat", 1 * min), stop("chat", 3 * min),
            stop("photopicker", 7 * 60 * min + 29 * min)
        )
        val total = ScreenTimeCalc.foregroundMillis(events, 0, 24 * 60 * min)
        assertNull(total["photopicker"])
        assertEquals(2 * min, total["chat"])
    }

    @Test
    fun missingPause_isEndedByTheNextApp() {
        val events = listOf(
            start("photopicker", 0), // no stop follows
            start("chat", 2 * min), stop("chat", 5 * min),
            start("maps", 10 * min), stop("maps", 12 * min)
        )
        val total = ScreenTimeCalc.foregroundMillis(events, 0, 24 * 60 * min)
        assertEquals(2 * min, total["photopicker"])
        assertEquals(3 * min, total["chat"])
        assertEquals(2 * min, total["maps"])
    }

    @Test
    fun total_neverExceedsTimeSinceMidnight() {
        val events = listOf(
            start("a", 0), start("b", 1 * min), start("c", 2 * min), start("a", 3 * min, "Other")
        )
        val end = 10 * min
        val total = ScreenTimeCalc.foregroundMillis(events, 0, end).values.sum()
        assertTrue("total $total > $end", total <= end)
        assertEquals(end, total) // something was always in front
    }

    @Test
    fun minutesByHour_splitsSessionsAcrossHours() {
        val hour = 60 * min
        val sessions = listOf(
            ScreenTimeCalc.Session("chat", 8 * hour + 50 * min, 9 * hour + 20 * min), // 10 in 08, 20 in 09
            ScreenTimeCalc.Session("maps", 9 * hour + 30 * min, 9 * hour + 35 * min)
        )
        val byHour = ScreenTimeCalc.minutesByHour(sessions, dayStart = 0)
        assertEquals(24, byHour.size)
        assertEquals(10, byHour[8])
        assertEquals(25, byHour[9])
        assertEquals(0, byHour[10])
        assertEquals(35, byHour.sum())
    }

    @Test
    fun minutesByHour_neverShowsMoreThanAnHour() {
        val hour = 60 * min
        // Two apps side by side (split screen) both count, but one hour can't hold more than 60 minutes.
        val sessions = listOf(
            ScreenTimeCalc.Session("a", 3 * hour, 4 * hour),
            ScreenTimeCalc.Session("b", 3 * hour, 4 * hour)
        )
        assertEquals(60, ScreenTimeCalc.minutesByHour(sessions, dayStart = 0)[3])
    }

    @Test
    fun pinnedWidget_isAddedOnceAndTurnsTheWidgetOn() {
        val base = MeenaUserSettings()
        assertTrue(base.widgetConfigs["widget-app-widgets"]?.isEnabled == false)
        val once = PinItemActivity.withHostedWidget(base, 42)
        val twice = PinItemActivity.withHostedWidget(once, 42)
        assertEquals(listOf(42), twice.hostedWidgetIds)
        assertTrue(twice.widgetConfigs["widget-app-widgets"]?.isEnabled == true)
    }
}
