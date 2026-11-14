package my.hana.meena.data.system

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Where the sun is: degrees clockwise from true north, and degrees above the horizon. */
data class SunPosition(val azimuth: Double, val elevation: Double)

/** Today's sunrise and sunset, each with the direction it happens in. Null when the sun doesn't cross. */
data class SunEvents(val riseMillis: Long?, val riseAzimuth: Double?, val setMillis: Long?, val setAzimuth: Double?)

/**
 * Sun position from the U.S. Naval Observatory's approximate solar coordinates (about 1
 * arcminute within two centuries of 2000), worked out on the phone; nothing is fetched. Sunrise
 * and sunset use the usual -0.833° (refraction plus the sun's radius). Greenwich sidereal time
 * uses the matching USNO approximation.
 * https://aa.usno.navy.mil/faq/sun_approx
 */
object SunPath {
    const val HORIZON = -0.833
    private const val DAY_MS = 86_400_000L
    private const val STEP_MS = 10 * 60_000L

    fun position(millis: Long, lat: Double, lng: Double): SunPosition {
        val d = millis / DAY_MS.toDouble() + 2440587.5 - 2451545.0
        val g = Math.toRadians(Qibla.normalize(357.529 + 0.98560028 * d))
        val q = Qibla.normalize(280.459 + 0.98564736 * d)
        val l = Math.toRadians(Qibla.normalize(q + 1.915 * sin(g) + 0.020 * sin(2 * g)))
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        val ra = Math.toDegrees(atan2(cos(e) * sin(l), cos(l)))
        val dec = asin(sin(e) * sin(l))
        val gmst = Qibla.normalize(15.0 * (18.697374558 + 24.06570982441908 * d))
        val h = Math.toRadians(Qibla.normalize(gmst + lng - ra))
        val phi = Math.toRadians(lat)
        val elevation = Math.toDegrees(asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(h)))
        // Measured from south going west, then turned to the usual "from north, clockwise".
        val fromSouth = Math.toDegrees(atan2(sin(h), cos(h) * sin(phi) - tan(dec) * cos(phi)))
        return SunPosition(Qibla.normalize(fromSouth + 180.0), elevation)
    }

    /** First sunrise and sunset in the 24 hours from [dayStartMillis] (local midnight). */
    fun events(dayStartMillis: Long, lat: Double, lng: Double): SunEvents {
        var rise: Long? = null
        var set: Long? = null
        var prevT = dayStartMillis
        var prevUp = position(prevT, lat, lng).elevation > HORIZON
        var t = dayStartMillis + STEP_MS
        while (t <= dayStartMillis + DAY_MS && (rise == null || set == null)) {
            val up = position(t, lat, lng).elevation > HORIZON
            if (up != prevUp) {
                val at = refine(prevT, t, lat, lng)
                if (up && rise == null) rise = at
                if (!up && set == null) set = at
            }
            prevT = t
            prevUp = up
            t += STEP_MS
        }
        return SunEvents(
            rise, rise?.let { position(it, lat, lng).azimuth },
            set, set?.let { position(it, lat, lng).azimuth }
        )
    }

    /** The sun every [stepMs] from sunrise to sunset, for drawing its path across the sky. */
    fun path(riseMillis: Long, setMillis: Long, lat: Double, lng: Double, stepMs: Long = 15 * 60_000L): List<SunPosition> {
        if (setMillis <= riseMillis) return emptyList()
        val times = generateSequence(riseMillis) { it + stepMs }.takeWhile { it < setMillis }.toList() + setMillis
        return times.map { position(it, lat, lng) }
    }

    /** Halves the window until it is under a second wide. */
    private fun refine(from: Long, to: Long, lat: Double, lng: Double): Long {
        var a = from
        var b = to
        val aUp = position(a, lat, lng).elevation > HORIZON
        while (b - a > 1_000L) {
            val mid = (a + b) / 2
            if ((position(mid, lat, lng).elevation > HORIZON) == aUp) a = mid else b = mid
        }
        return b
    }
}
