package my.hana.meena.data.system

import java.util.Calendar

/** The single soonest thing the user cares about: a calendar event or a prayer. */
data class NextUp(
    val title: String,
    val startMillis: Long,
    val isPrayer: Boolean
) {
    fun minutesUntil(nowMillis: Long): Long = ((startMillis - nowMillis).coerceAtLeast(0L) + 59_999L) / 60_000L
}

object NextUpCalculator {
    /**
     * Returns the sooner of the next timed calendar event and the next prayer today,
     * or null when neither source has data. Never returns a sample.
     * All-day events are ignored. Prayer slots flagged solar (sunrise) are ignored.
     */
    fun compute(
        nowMillis: Long,
        events: List<DeviceCalendarEvent>,
        slots: List<PrayerSlotInfo>
    ): NextUp? {
        val event = events
            .filter { !it.isAllDay && it.startTime > nowMillis && it.title.isNotBlank() }
            .minByOrNull { it.startTime }
            ?.let { NextUp(it.title.trim(), it.startTime, isPrayer = false) }

        val prayer = slots
            .filter { !it.isSolar && it.name.isNotBlank() }
            .map { slot ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, slot.hour24)
                    set(Calendar.MINUTE, slot.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                NextUp(slot.name.trim(), cal.timeInMillis, isPrayer = true)
            }
            .filter { it.startMillis > nowMillis }
            .minByOrNull { it.startMillis }

        return listOfNotNull(event, prayer).minByOrNull { it.startMillis }
    }

    /** "in 42m", "in 1h 05m". */
    fun formatRemaining(minutes: Long): String =
        if (minutes < 60) "in ${minutes}m" else "in ${minutes / 60}h ${"%02d".format(minutes % 60)}m"
}
