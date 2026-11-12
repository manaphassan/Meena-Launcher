package my.hana.meena.data.system

import java.util.Calendar
import java.util.TimeZone

data class StripDay(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    /** Calendar.DAY_OF_WEEK of this day. */
    val dayOfWeek: Int,
    val startMillis: Long,
    val isToday: Boolean,
    val isWeekend: Boolean,
    val eventCount: Int,
    /** False when the day is past how far ahead the calendar was read, so its count means nothing. */
    val hasEventInfo: Boolean = true
)

/** A calendar entry reduced to what the strip needs. */
data class StripEvent(val startMillis: Long, val isAllDay: Boolean)

/**
 * Today and the next six days, each with how many events start on it. All-day events are stored
 * by the Calendar Provider at midnight UTC, so they are placed by their UTC date; otherwise they
 * would land on the previous day west of Greenwich. Only the first [knownDays] days carry counts.
 */
object WeekStripBuilder {

    fun build(
        nowMillis: Long,
        events: List<StripEvent>,
        timeZone: TimeZone = TimeZone.getDefault(),
        knownDays: Int = 7
    ): List<StripDay> {
        val utc = TimeZone.getTimeZone("UTC")
        fun dayKey(millis: Long, zone: TimeZone): Triple<Int, Int, Int> {
            val c = Calendar.getInstance(zone).apply { timeInMillis = millis }
            return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        }
        val counts = events.groupingBy { dayKey(it.startMillis, if (it.isAllDay) utc else timeZone) }.eachCount()

        val day = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return List(7) { index ->
            if (index > 0) day.add(Calendar.DAY_OF_MONTH, 1)
            val key = Triple(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH))
            val dow = day.get(Calendar.DAY_OF_WEEK)
            val known = index < knownDays
            StripDay(
                year = key.first,
                month = key.second,
                dayOfMonth = key.third,
                dayOfWeek = dow,
                startMillis = day.timeInMillis,
                isToday = index == 0,
                isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY,
                eventCount = if (known) counts[key] ?: 0 else 0,
                hasEventInfo = known
            )
        }
    }
}
