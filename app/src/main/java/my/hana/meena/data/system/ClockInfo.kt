package my.hana.meena.data.system

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

    /** Malaysian English alarm time, see [MalaysianEnglish.alarmTime]. */
    fun formatAlarm(
        triggerMillis: Long,
        nowMillis: Long,
        is24Hour: Boolean,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String = MalaysianEnglish.alarmTime(triggerMillis, nowMillis, is24Hour, timeZone)
}
