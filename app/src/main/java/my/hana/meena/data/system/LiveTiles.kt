package my.hana.meena.data.system

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * One back face of a live tile: a big line and a small line, and on a wide tile an optional
 * [third] line. [cover] means the face is the playing track, shown with the cover the music
 * app shares. Only real data, never a sample.
 */
data class LiveFace(
    val primary: String,
    val secondary: String,
    val third: String = "",
    val cover: Boolean = false
)

/** What the music widget already knows about the track playing now, for that app's tile. */
data class PlayingNow(val packageName: String, val title: String, val artist: String, val hasCover: Boolean)

/**
 * Removes emoji from text Meena shows on tiles (GEMINI.md rule 7: no emoji). Covers the emoji
 * blocks, dingbats, flags, skin tones and the joiners and selectors that glue emoji together.
 * Letters, digits, punctuation, ° and © are kept. Spaces left behind are collapsed.
 */
object PlainText {
    fun stripEmoji(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            if (!isEmojiPart(cp)) out.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return out.toString().replace(Regex("""\s{2,}"""), " ").trim()
    }

    private fun isEmojiPart(cp: Int): Boolean =
        cp in 0x1F000..0x1FAFF || // emoticons, pictographs, transport, flags, skin tones, extended
            cp in 0x2600..0x27BF || // misc symbols and dingbats
            cp in 0x2B00..0x2BFF || // stars, arrows used as emoji
            cp in 0xE0020..0xE007F || // tag characters (subdivision flags)
            cp == 0x200D || cp == 0xFE0F || cp == 0xFE0E || cp == 0x20E3 ||
            cp == 0x3030 || cp == 0x303D || cp == 0x3297 || cp == 0x3299
}

/** What the tiles can say about the next alarm: when it rings and which app set it. */
data class NextAlarm(val triggerMillis: Long, val creatorPackage: String?)

/** Words the faces use, resolved from strings.xml by the caller. */
data class LiveTileWords(
    val new: String,
    /** Takes the first name and how many more, e.g. "%1\$s and %2\$d more". */
    val andMore: (String, Int) -> String,
    val alarm: (String) -> String,
    val onNow: String,
    /** Start and end time, e.g. "%1\$s to %2\$s". */
    val fromTo: (String, String) -> String,
    /** Hours and minutes until an alarm, e.g. "in %1\$dh %2\$dm". */
    val inTime: (Int, Int) -> String
)

/**
 * Builds the back faces for a favourite tile from what Meena already reads on the phone:
 * notifications (count and, for chats, who they are from), the next calendar event for the
 * calendar app, the next alarm for the app that set it, and what's playing for the music app.
 * Android has no way for other apps to send tile content to a launcher, so nothing else is shown.
 *
 * A [wide] tile has room for more of the same: two chat names, an email's sender and subject,
 * an event's end time and place, how long until the alarm. Names and subjects only appear when
 * [showNames] is on, and chat messages never do.
 */
object LiveTiles {

    fun facesFor(
        packageName: String,
        notifications: List<LiveNotificationItem>,
        calendarPackages: Set<String>,
        events: List<DeviceCalendarEvent>,
        alarm: NextAlarm?,
        nowMillis: Long,
        showNames: Boolean,
        is24Hour: Boolean,
        words: LiveTileWords,
        timeZone: TimeZone = TimeZone.getDefault(),
        wide: Boolean = false,
        playing: PlayingNow? = null
    ): List<LiveFace> {
        val faces = mutableListOf<LiveFace>()

        if (playing != null && playing.packageName == packageName) {
            faces += LiveFace(
                PlainText.stripEmoji(playing.title).lowercase(Locale.UK),
                PlainText.stripEmoji(playing.artist).lowercase(Locale.UK),
                cover = playing.hasCover
            )
        }

        val mine = notifications.filter { it.packageName == packageName }.sortedByDescending { it.timestamp }
        if (mine.isNotEmpty() && wide) {
            faces += wideCountFace(mine, showNames, words)
        } else if (mine.isNotEmpty()) {
            // Names only come from chats (sender or group); other apps' titles can be message text.
            val names = if (showNames) {
                mine.filter { it.isConversation }.map { PlainText.stripEmoji(it.title) }.filter { it.isNotBlank() }.distinct()
            } else emptyList()
            val secondary = when {
                names.isEmpty() -> words.new
                names.size == 1 -> names.first()
                else -> words.andMore(names.first(), names.size - 1)
            }
            faces += LiveFace(mine.size.toString(), secondary.lowercase(Locale.UK))
        }

        if (packageName in calendarPackages) {
            nextEvent(events, nowMillis)?.let { event ->
                val title = PlainText.stripEmoji(event.title).lowercase(Locale.UK)
                val start = whenText(event, nowMillis, is24Hour, words, timeZone)
                faces += if (!wide) {
                    LiveFace(title, start)
                } else {
                    // Timed events that haven't started say when they end too; all-day and running ones don't.
                    val whenWide = if (event.isAllDay || start == words.onNow) start
                    else words.fromTo(start, timeOf(event.endTime, is24Hour, timeZone))
                    LiveFace(title, whenWide, PlainText.stripEmoji(event.location).lowercase(Locale.UK))
                }
            }
        }

        if (alarm != null && alarm.creatorPackage == packageName && alarm.triggerMillis > nowMillis) {
            val time = SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.UK)
                .apply { this.timeZone = timeZone }.format(Date(alarm.triggerMillis)).lowercase(Locale.UK)
            val days = MalaysianEnglish.daysBetween(nowMillis, alarm.triggerMillis, timeZone)
            val day = if (days >= 7) {
                SimpleDateFormat("d MMMM", Locale.UK).apply { this.timeZone = timeZone }
                    .format(Date(alarm.triggerMillis)).lowercase(Locale.UK)
            } else {
                MalaysianEnglish.dayName(alarm.triggerMillis, nowMillis, timeZone)
            }
            // Under 12 hours away, a wide tile also says how long is left.
            val minutesLeft = ((alarm.triggerMillis - nowMillis) / 60_000L).toInt()
            val left = if (wide && minutesLeft < 12 * 60) words.inTime(minutesLeft / 60, minutesLeft % 60) else ""
            faces += LiveFace(time, words.alarm(day), left)
        }

        return faces
    }

    /**
     * The count with who it's from, for a wide tile: up to two chat names (the second line ends
     * "and N more" when there are more), or an email's sender and subject. Only with [showNames].
     */
    private fun wideCountFace(mine: List<LiveNotificationItem>, showNames: Boolean, words: LiveTileWords): LiveFace {
        val count = mine.size.toString()
        if (!showNames) return LiveFace(count, words.new)
        fun plain(text: String) = PlainText.stripEmoji(text).lowercase(Locale.UK)
        val names = mine.filter { it.isConversation }.map { plain(it.title) }.filter { it.isNotBlank() }.distinct()
        if (names.isNotEmpty()) {
            return when (names.size) {
                1 -> LiveFace(count, names[0])
                2 -> LiveFace(count, names[0], names[1])
                else -> LiveFace(count, names[0], plain(words.andMore(names[1], names.size - 2)))
            }
        }
        val email = mine.firstOrNull { it.isEmail } ?: return LiveFace(count, words.new)
        val sender = plain(email.title)
        return LiveFace(count, sender.ifBlank { words.new }, plain(email.text))
    }

    private fun timeOf(millis: Long, is24Hour: Boolean, timeZone: TimeZone): String =
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.UK)
            .apply { this.timeZone = timeZone }.format(Date(millis)).lowercase(Locale.UK)

    /** The next event that hasn't finished yet: timed events still running count, past ones don't. */
    fun nextEvent(events: List<DeviceCalendarEvent>, nowMillis: Long): DeviceCalendarEvent? =
        events.filter { it.endTime > nowMillis }.minByOrNull { it.startTime }

    private fun whenText(
        event: DeviceCalendarEvent,
        nowMillis: Long,
        is24Hour: Boolean,
        words: LiveTileWords,
        timeZone: TimeZone
    ): String {
        if (!event.isAllDay && event.startTime <= nowMillis) return words.onNow
        // All-day events are stored at midnight UTC, so read their day in UTC, then place it at
        // local noon so the local day name matches.
        val day = if (event.isAllDay) {
            val utc =
                java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = event.startTime }
            val localNoon = java.util.Calendar.getInstance(timeZone).apply {
                clear()
                set(
                    utc.get(java.util.Calendar.YEAR),
                    utc.get(java.util.Calendar.MONTH),
                    utc.get(java.util.Calendar.DAY_OF_MONTH),
                    12,
                    0
                )
            }.timeInMillis
            // A tile is about 73dp wide: the day alone ("tomorrow") fits, "tomorrow, all day" doesn't.
            return MalaysianEnglish.dayName(localNoon, nowMillis, timeZone)
        } else {
            MalaysianEnglish.dayName(event.startTime, nowMillis, timeZone)
        }
        val time = SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.UK)
            .apply { this.timeZone = timeZone }.format(Date(event.startTime)).lowercase(Locale.UK)
        return "$day $time"
    }
}
