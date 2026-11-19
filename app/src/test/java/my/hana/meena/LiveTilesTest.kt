package my.hana.meena

import my.hana.meena.data.system.DeviceCalendarEvent
import my.hana.meena.data.system.LiveFace
import my.hana.meena.data.system.LiveNotificationItem
import my.hana.meena.data.system.LiveTileWords
import my.hana.meena.data.system.LiveTiles
import my.hana.meena.data.system.NextAlarm
import my.hana.meena.data.system.PlayingNow
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
        onNow = "on now",
        fromTo = { from, to -> "$from to $to" },
        inTime = { h, m -> "in ${h}h ${m}m" }
    )

    private fun notif(pkg: String, title: String, chat: Boolean, minutesAgo: Long) = LiveNotificationItem(
        key = "$pkg-$title-$minutesAgo", packageName = pkg, appName = pkg, title = title, text = "secret message",
        timestamp = now - minutesAgo * 60_000, formattedTime = "", isConversation = chat
    )

    private fun email(sender: String, subject: String, minutesAgo: Long) = LiveNotificationItem(
        key = "mail-$sender-$minutesAgo", packageName = "mail.app", appName = "mail", title = sender, text = subject,
        timestamp = now - minutesAgo * 60_000, formattedTime = "", isEmail = true
    )

    private fun faces(
        pkg: String,
        notifications: List<LiveNotificationItem> = emptyList(),
        events: List<DeviceCalendarEvent> = emptyList(),
        alarm: NextAlarm? = null,
        showNames: Boolean = true,
        wide: Boolean = false,
        playing: PlayingNow? = null
    ) = LiveTiles.facesFor(
        pkg,
        notifications,
        setOf("cal.app"),
        events,
        alarm,
        now,
        showNames,
        true,
        words,
        tz,
        wide,
        playing
    )

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

    @Test
    fun chatNames_loseTheirEmoji_andEmojiOnlyNamesAreDropped() {
        val group =
            listOf(notif("chat.app", "Cuci Mata Shopping \uD83D\uDE03\uD83D\uDE03\uD83D\uDE03 (10 messages)", true, 1))
        assertEquals(listOf(LiveFace("1", "cuci mata shopping (10 messages)")), faces("chat.app", group))
        val onlyEmoji = listOf(notif("chat.app", "\u2764\uFE0F\uD83D\uDD25", true, 1))
        assertEquals(listOf(LiveFace("1", "new")), faces("chat.app", onlyEmoji))
    }

    @Test
    fun stripEmoji_keepsLettersAndTypography() {
        val p = my.hana.meena.data.system.PlainText
        assertEquals("kuih raya 26° © 2026", p.stripEmoji("kuih \uD83C\uDF6A raya \uD83D\uDC4D\uD83C\uDFFD 26° © 2026"))
        assertEquals("family", p.stripEmoji("family \uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67"))
        assertEquals("malaysia", p.stripEmoji("\uD83C\uDDF2\uD83C\uDDFE malaysia"))
        assertEquals("jawi جاوي", p.stripEmoji("jawi جاوي ⭐"))
    }

    // Wide tiles

    @Test
    fun wideChat_showsTwoNames_thenHowManyMore_neverTheMessage() {
        val list = listOf(
            notif("chat.app", "Wife", true, 1),
            notif("chat.app", "Family", true, 5),
            notif("chat.app", "Ali", true, 7),
            notif("chat.app", "Wife", true, 9)
        )
        val result = faces("chat.app", list, wide = true)
        assertEquals(listOf(LiveFace("4", "wife", "family and 1 more")), result)
        assertTrue(result.none { (it.primary + it.secondary + it.third).contains("secret") })
        assertEquals(listOf(LiveFace("2", "wife", "family")), faces("chat.app", list.take(2), wide = true))
        assertEquals(listOf(LiveFace("1", "wife")), faces("chat.app", list.take(1), wide = true))
    }

    @Test
    fun wideTile_namesOff_showsCountOnly() {
        val chat = listOf(notif("chat.app", "Wife", true, 1), notif("chat.app", "Ali", true, 2))
        assertEquals(listOf(LiveFace("2", "new")), faces("chat.app", chat, showNames = false, wide = true))
        val mail = listOf(email("Siti Aminah", "Invoice for November", 1))
        assertEquals(listOf(LiveFace("1", "new")), faces("mail.app", mail, showNames = false, wide = true))
    }

    @Test
    fun wideEmail_showsNewestSenderAndSubject_oneCellStaysACount() {
        val mail = listOf(email("Siti Aminah", "Invoice for November", 1), email("Bank", "Statement \uD83D\uDCB0", 30))
        assertEquals(listOf(LiveFace("2", "siti aminah", "invoice for november")), faces("mail.app", mail, wide = true))
        assertEquals(listOf(LiveFace("2", "new")), faces("mail.app", mail))
    }

    @Test
    fun wideCalendar_addsTheEndTimeAndPlace() {
        val running = DeviceCalendarEvent(2, "Standup", at("2026-11-13 08:15"), at("2026-11-13 08:45"), false)
        val later =
            DeviceCalendarEvent(3, "Lunch", at("2026-11-13 13:00"), at("2026-11-13 14:00"), false, location = "Bilik 2")
        assertEquals(
            listOf(LiveFace("lunch", "today 13:00 to 14:00", "bilik 2")),
            faces("cal.app", events = listOf(later), wide = true)
        )
        assertEquals(
            listOf(LiveFace("standup", "on now")),
            faces("cal.app", events = listOf(running, later), wide = true)
        )
    }

    @Test
    fun wideAlarm_saysHowLongIsLeft_onlyUnderTwelveHours() {
        val soon = NextAlarm(at("2026-11-13 17:50"), "clock.app")
        assertEquals(
            listOf(LiveFace("17:50", "alarm today", "in 9h 20m")),
            faces("clock.app", alarm = soon, wide = true)
        )
        val tomorrow = NextAlarm(at("2026-11-14 03:30"), "clock.app")
        assertEquals(listOf(LiveFace("03:30", "alarm tomorrow")), faces("clock.app", alarm = tomorrow, wide = true))
        // One-cell tiles keep the two lines they had.
        assertEquals(listOf(LiveFace("17:50", "alarm today")), faces("clock.app", alarm = soon))
    }

    @Test
    fun musicApp_showsWhatsPlaying_onItsOwnTileOnly() {
        val playing = PlayingNow("music.app", "Aku Yang Asing", "Hujan", hasCover = true)
        assertEquals(listOf(LiveFace("aku yang asing", "hujan", cover = true)), faces("music.app", playing = playing))
        assertEquals(
            listOf(LiveFace("aku yang asing", "hujan", cover = true)),
            faces("music.app", playing = playing, wide = true)
        )
        assertEquals(emptyList<LiveFace>(), faces("chat.app", playing = playing))
        assertEquals(emptyList<LiveFace>(), faces("music.app"))
    }
}
