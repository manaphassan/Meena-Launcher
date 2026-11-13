package my.hana.meena.data.system

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * One forecast hour from Open-Meteo: [endMillis] closes the hour (precipitation and its
 * probability cover the hour before it, per https://open-meteo.com/en/docs).
 */
data class RainHour(val endMillis: Long, val probability: Int, val millimetres: Float)

/** What the next few hours look like; times are the hour boundaries from the forecast. */
sealed interface RainOutlook {
    data object Dry : RainOutlook
    data object AllWet : RainOutlook

    /** Rain likely now, easing at [untilMillis]. */
    data class WetUntil(val untilMillis: Long) : RainOutlook

    /** Dry now, rain likely from [fromMillis]. */
    data class WetFrom(val fromMillis: Long) : RainOutlook
}

object RainOutlookCalculator {
    /** Hours ahead the line speaks for. */
    const val HOURS = 3

    /** Open-Meteo's probability is for more than 0.1 mm in the hour; at 50% or more we say "likely". */
    const val LIKELY_PERCENT = 50

    /**
     * Reads the next [HOURS] forecast hours that haven't finished. Returns null when fewer than
     * two are left (old data), so the line hides instead of describing hours already gone.
     */
    fun compute(hours: List<RainHour>, nowMillis: Long): RainOutlook? {
        val ahead = hours.filter { it.endMillis > nowMillis }.sortedBy { it.endMillis }.take(HOURS)
        if (ahead.size < 2) return null
        val wet = ahead.map { it.probability >= LIKELY_PERCENT }
        return when {
            wet.none { it } -> RainOutlook.Dry
            wet.all { it } -> RainOutlook.AllWet
            wet.first() -> RainOutlook.WetUntil(ahead[wet.indexOfFirst { !it } - 1].endMillis)
            else -> RainOutlook.WetFrom(ahead[wet.indexOfFirst { it }].endMillis - HOUR_MS)
        }
    }

    private const val HOUR_MS = 3_600_000L
}

/** MET Malaysia's public radar page; opened in the browser, nothing is sent from Meena. */
object RainRadar {
    const val MET_MALAYSIA_RADAR_URL = "https://www.met.gov.my/pencerapan/radar/"

    fun open(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(MET_MALAYSIA_RADAR_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }
}
