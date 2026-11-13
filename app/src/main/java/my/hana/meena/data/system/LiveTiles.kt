package my.hana.meena.data.system

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One back face of a live tile: a big line and a small line. Only real data, never a sample. */
data class LiveFace(val primary: String, val secondary: String)

/** What the tiles can say about the next alarm: when it rings and which app set it. */
data class NextAlarm(val triggerMillis: Long, val creatorPackage: String?)

/** Words the faces use, resolved from strings.xml by the caller. */
data class LiveTileWords(
    val new: String,
    /** Takes the first name and how many more, e.g. "%1\$s and %2\$d more". */
    val andMore: (String, Int) -> String,
    val alarm: (String) -> String,
    val onNow: String
)

/**
 * Builds the back faces for a favourite tile from what Meena already reads on the phone:
 * notifications (count and, for chats, who they are from), the next calendar event for the
 * calendar app, and the next alarm for the app that set it. Android has no way for other apps
 * to send tile content to a launcher, so nothing else is shown.
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
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<LiveFace> {
        val faces = mutableListOf<LiveFace>()

        val mine = notifications.filter { it.packageName == packageName }.sortedByDescending { it.timestamp }
        if (mine.isNotEmpty()) {
            // Names only come from chats (sender or group); other apps' titles can be message text.
            val names = if (showNames) {
                mine.filter { it.isConversation }.map { it.title.trim() }.filter { it.isNotBlank() }.distinct()
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
                faces += LiveFace(
                    event.title.lowercase(Locale.UK),
                    whenText(event, nowMillis, is24Hour, words, timeZone)
                )
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
            faces += LiveFace(time, words.alarm(day))
        }

        return faces
    }

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
