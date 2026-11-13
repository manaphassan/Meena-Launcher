package my.hana.meena.data.news

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Date labels for news, Malaysian English order (day month), always lowercase. */
object NewsTime {

    /** "12 november, 2:21 pm"; the year is added when it isn't this year. Empty when the date is unknown. */
    fun readerDate(timeMs: Long, nowMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        if (timeMs <= 0L) return ""
        val pattern = if (sameYear(timeMs, nowMs, zone)) "d MMMM, h:mm a" else "d MMMM yyyy, h:mm a"
        return format(pattern, timeMs, zone)
    }

    /** "12 nov" (or "12 nov 2025" in another year), for list rows older than a week. */
    fun shortDate(timeMs: Long, nowMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        if (timeMs <= 0L) return ""
        val pattern = if (sameYear(timeMs, nowMs, zone)) "d MMM" else "d MMM yyyy"
        return format(pattern, timeMs, zone)
    }

    private fun format(pattern: String, timeMs: Long, zone: TimeZone): String =
        SimpleDateFormat(pattern, Locale.ENGLISH).apply { timeZone = zone }
            .format(Date(timeMs))
            .lowercase(Locale.ENGLISH)

    private fun sameYear(a: Long, b: Long, zone: TimeZone): Boolean {
        val ca = Calendar.getInstance(zone).apply { timeInMillis = a }
        val cb = Calendar.getInstance(zone).apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
    }
}
