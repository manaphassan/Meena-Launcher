package my.hana.meena.data.system

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Malaysian English wording for dates: day before month, whole words instead of "tod" or
 * "tom", and the hijri months and prayer names as JAKIM writes them.
 */
object MalaysianEnglish {

    /** JAKIM spellings (e-solat takwim), month 1 to 12. */
    private val hijriMonths = listOf(
        "muharam", "safar", "rabiulawal", "rabiulakhir", "jamadilawal", "jamadilakhir",
        "rejab", "syaaban", "ramadan", "syawal", "zulkaedah", "zulhijah"
    )

    /** Prayer names on the e-solat site, keyed by the e-solat api field name. */
    val prayerNames = linkedMapOf(
        "imsak" to "imsak",
        "fajr" to "subuh",
        "syuruk" to "syuruk",
        "dhuhr" to "zohor",
        "asr" to "asar",
        "maghrib" to "maghrib",
        "isha" to "isyak"
    )

    /** "3 jamadilakhir 1448h", or null for a month outside 1 to 12. */
    fun hijriDate(day: Int, month: Int, year: Int): String? {
        val name = hijriMonths.getOrNull(month - 1) ?: return null
        return "$day $name ${year}h"
    }

    /** "today", "tomorrow", then the full weekday ("sunday") for the rest of the week. */
    fun dayName(
        dayMillis: Long,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String = when (daysBetween(nowMillis, dayMillis, timeZone)) {
        0 -> "today"
        1 -> "tomorrow"
        else -> SimpleDateFormat("EEEE", Locale.UK).apply { this.timeZone = timeZone }
            .format(Date(dayMillis)).lowercase(Locale.UK)
    }

    /** A time of day in the person's chosen clock: "15:48" or "3:48 pm". */
    fun clockTime(
        millis: Long,
        is24Hour: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String =
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.UK)
            .apply { this.timeZone = timeZone }.format(Date(millis)).lowercase(Locale.UK)

    /**
     * When the next alarm rings: "today 6:30 am", "tomorrow 03:30", "saturday 07:00", and the
     * date ("21 november 07:00") once it is a week or more away.
     */
    fun alarmTime(
        triggerMillis: Long,
        nowMillis: Long,
        is24Hour: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val days = daysBetween(nowMillis, triggerMillis, timeZone)
        val time = clockTime(triggerMillis, is24Hour, timeZone)
        val day = if (days >= 7) {
            SimpleDateFormat("d MMMM", Locale.UK).apply { this.timeZone = timeZone }
                .format(Date(triggerMillis)).lowercase(Locale.UK)
        } else {
            dayName(triggerMillis, nowMillis, timeZone)
        }
        return "$day $time"
    }

    fun daysBetween(fromMillis: Long, toMillis: Long, timeZone: TimeZone): Int {
        fun startOfDay(millis: Long): Long = Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        // Round so a 23 or 25 hour day around a clock change still counts as one day.
        return Math.round((startOfDay(toMillis) - startOfDay(fromMillis)) / 86_400_000.0).toInt()
    }
}
