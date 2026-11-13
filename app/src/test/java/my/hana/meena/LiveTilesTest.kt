package my.hana.meena

import my.hana.meena.data.system.DeviceCalendarEvent
import my.hana.meena.data.system.LiveFace
import my.hana.meena.data.system.LiveNotificationItem
import my.hana.meena.data.system.LiveTileWords
import my.hana.meena.data.system.LiveTiles
import my.hana.meena.data.system.NextAlarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class LiveTilesTest {

    private val tz = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    private fun at(text: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = tz }.parse(text)!!.time

    private val now = at("2026-11-13 08:30") // friday
    private val words = LiveTileWords(
        new = "new",
        andMore = { name, more -> "$name and $more more" },
        alarm = { "alarm $it" },
        onNow = "on now"
    )

    private fun notif(pkg: String, title: String, chat: Boolean, minutesAgo: Long) = LiveNotificationItem(
        key = "$pkg-$title-$minutesAgo", packageName = pkg, appName = pkg, title = title, text = "secret message",
        timestamp = now - minutesAgo * 60_000, formattedTime = "", isConversation = chat
    )

    private fun faces(
        pkg: String,
        notifications: List<LiveNotificationItem> = emptyList(),
        events: List<DeviceCalendarEvent> = emptyList(),
        alarm: NextAlarm? = null,
        showNames: Boolean = true
    ) = LiveTiles.facesFor(pkg, notifications, setOf("cal.app"), events, alarm, now, showNames, true, words, tz)

    @Test
    fun chats_showCountAndNames_neverTheMessage() {
        val list = listOf(
            notif("chat.app", "Wife", true, 1),
            notif("chat.app", "Family", true, 5),
            notif("chat.app", "Wife", true, 9)
        )
        val result = faces("chat.app", list)
        assertEquals(listOf(LiveFace("3", "wife and 1 more")), result)
        assertTrue(result.none { it.primary.contains("secret") || it.secondary.contains("secret") })
    }

    @Test
    fun namesOff_orNotAChat_showsCountOnly() {
        val chat = listOf(notif("chat.app", "Wife", true, 1))
        assertEquals(listOf(LiveFace("1", "new")), faces("chat.app", chat, showNames = false))
        val other = listOf(notif("news.app", "Rainy morning, cloudy later", false, 1))
        assertEquals(listOf(LiveFace("1", "new")), faces("news.app", other))
    }

    @Test
    fun calendarApp_showsNextUnfinishedEvent() {
        val past = DeviceCalendarEvent(1, "Breakfast", at("2026-11-13 07:00"), at("2026-11-13 08:00"), false)
        val running = DeviceCalendarEvent(2, "Standup", at("2026-11-13 08:15"), at("2026-11-13 08:45"), false)
        val later = DeviceCalendarEvent(3, "Lunch", at("2026-11-13 13:00"), at("2026-11-13 14:00"), false)
        assertEquals(listOf(LiveFace("standup", "on now")), faces("cal.app", events = listOf(past, later, running)))
        assertEquals(listOf(LiveFace("lunch", "today 13:00")), faces("cal.app", events = listOf(past, later)))
        // Other apps never get the calendar face.
        assertEquals(emptyList<LiveFace>(), faces("chat.app", events = listOf(later)))
    }

    @Test
    fun allDayEvent_readsItsUtcDate() {
        val utc = TimeZone.getTimeZone("UTC")
        val start =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = utc }.parse("2026-11-14 00:00")!!.time
        val birthday = DeviceCalendarEvent(4, "Isa's birthday", start, start + 86_400_000L, true)
        assertEquals(listOf(LiveFace("isa's birthday", "tomorrow")), faces("cal.app", events = listOf(birthday)))
    }

    @Test
    fun alarm_onlyOnTheAppThatSetIt() {
        val alarm = NextAlarm(at("2026-11-14 03:30"), "clock.app")
        assertEquals(listOf(LiveFace("03:30", "alarm tomorrow")), faces("clock.app", alarm = alarm))
        assertEquals(emptyList<LiveFace>(), faces("chat.app", alarm = alarm))
        assertEquals(emptyList<LiveFace>(), faces("clock.app", alarm = NextAlarm(at("2026-11-14 03:30"), null)))
    }
}
