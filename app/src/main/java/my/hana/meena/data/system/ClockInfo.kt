package my.hana.meena.data.system

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Pure helpers for the info lines under the clock (place, low and high, wind, next alarm). */
object ClockInfo {

    // Names DeviceLocationHelper uses before a real place is known; never show these as a place.
    private val placeholderNames = setOf("", "current location", "detecting location...", "local area")

    /** Returns the place to show, or null when only a placeholder or the built-in default is known. */
    fun placeName(name: String?, isRealFix: Boolean): String? {
        if (!isRealFix) return null
        val trimmed = name?.trim()?.lowercase() ?: return null
        return trimmed.takeUnless { it in placeholderNames }
    }

    /**
     * "fri 3:30 am" (or "fri 03:30" in 24h). Alarms a week or more away show the date instead
     * of the weekday, so "fri" never points to the wrong week.
     */
    fun formatAlarm(
        triggerMillis: Long,
        nowMillis: Long,
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val dayPattern = if (daysBetween(nowMillis, triggerMillis, timeZone) >= 7) "d MMM" else "EEE"
        val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
        val format = SimpleDateFormat("$dayPattern $timePattern", locale).apply { this.timeZone = timeZone }
        return format.format(Date(triggerMillis)).lowercase(locale)
    }

    private fun daysBetween(fromMillis: Long, toMillis: Long, timeZone: TimeZone): Int {
        fun startOfDay(millis: Long): Long = Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((startOfDay(toMillis) - startOfDay(fromMillis)) / 86_400_000L).toInt()
    }
}
