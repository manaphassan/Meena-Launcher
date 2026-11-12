package my.hana.meena.data.system

import java.util.Calendar
import java.util.TimeZone

/** One week row of a month grid: ISO week number plus 7 day slots (null outside the month). */
data class MonthWeek(val weekNumber: Int, val days: List<Int?>)

data class MonthGrid(val year: Int, val month: Int, val weeks: List<MonthWeek>)

/**
 * Builds a Monday-first month grid with ISO-8601 week numbers (weeks start Monday; week 1 holds
 * the year's first Thursday), the way the AIO monthly calendar shows them.
 */
object MonthGridBuilder {

    fun build(year: Int, month: Int, timeZone: TimeZone = TimeZone.getDefault()): MonthGrid {
        val cal = isoCalendar(timeZone).apply {
            clear()
            set(year, month, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Monday = 0 ... Sunday = 6
        val leading = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

        val slots = List(leading) { null } + (1..daysInMonth).toList()
        val weeks = slots.chunked(7).map { week ->
            val firstDay = week.firstOrNull { it != null }!!
            val weekCal = isoCalendar(timeZone).apply {
                clear()
                set(year, month, firstDay)
            }
            MonthWeek(weekCal.get(Calendar.WEEK_OF_YEAR), week + List(7 - week.size) { null })
        }
        return MonthGrid(year, month, weeks)
    }

    /** Day-of-month numbers (in [year]/[month]) that have at least one event starting on them. */
    fun eventDays(startTimes: List<Long>, year: Int, month: Int, timeZone: TimeZone = TimeZone.getDefault()): Set<Int> {
        val cal = Calendar.getInstance(timeZone)
        return startTimes.mapNotNull { start ->
            cal.timeInMillis = start
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) cal.get(Calendar.DAY_OF_MONTH) else null
        }.toSet()
    }

    private fun isoCalendar(timeZone: TimeZone): Calendar = Calendar.getInstance(timeZone).apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
    }
}
