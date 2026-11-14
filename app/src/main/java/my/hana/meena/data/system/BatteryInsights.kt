package my.hana.meena.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat

/** How fast the phone is charging, as Android Settings words it. */
enum class ChargeSpeed { SLOW, NORMAL, FAST }

/** What the phone section should say about plugging in or out. */
enum class ChargeAdvice { PLUG_IN_NOW, PLUG_IN_SOON, NOT_YET, UNPLUG_NOW, FULL }

/** One battery reading: when, the level, and whether it was charging. */
data class BatterySample(val atMillis: Long, val level: Int, val charging: Boolean)

/** Battery rules, kept free of Android so they can be unit-tested. */
object BatteryRules {
    /** Advice levels. 20 and 80 are the usual battery-care marks [I]. */
    const val LOW = 20
    const val VERY_LOW = 10
    const val CARE_LIMIT = 80

    /** Android Settings' defaults: under 5 W is slow, over 7.5 W is fast (SettingsLib config.xml). */
    private const val SLOW_MICROWATTS = 5_000_000L
    private const val FAST_MICROWATTS = 7_500_000L
    private const val DEFAULT_MICROVOLTS = 5_000_000L

    /**
     * Speed from the charger's maximum current (µA) and voltage (µV), the way SettingsLib's
     * BatteryStatus does it; a missing voltage counts as 5 V. Null when the current is unknown.
     * https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SettingsLib/src/com/android/settingslib/fuelgauge/BatteryStatus.java
     */
    fun speed(maxMicroamps: Int, maxMicrovolts: Int): ChargeSpeed? {
        if (maxMicroamps <= 0) return null
        val volts = if (maxMicrovolts > 0) maxMicrovolts.toLong() else DEFAULT_MICROVOLTS
        val microwatts = Math.round(maxMicroamps * 0.001 * volts * 0.001)
        return when {
            microwatts <= 0 -> null
            microwatts < SLOW_MICROWATTS -> ChargeSpeed.SLOW
            microwatts > FAST_MICROWATTS -> ChargeSpeed.FAST
            else -> ChargeSpeed.NORMAL
        }
    }

    fun advice(level: Int, charging: Boolean, full: Boolean): ChargeAdvice = when {
        full || (charging && level >= 100) -> ChargeAdvice.FULL
        charging && level >= CARE_LIMIT -> ChargeAdvice.UNPLUG_NOW
        charging -> ChargeAdvice.NOT_YET
        level <= VERY_LOW -> ChargeAdvice.PLUG_IN_NOW
        level <= LOW -> ChargeAdvice.PLUG_IN_SOON
        else -> ChargeAdvice.NOT_YET
    }

    /** Readings this far back are used for the rate; older ones describe a different day. */
    const val WINDOW_MS = 3 * 60 * 60 * 1000L
    private const val MIN_SPAN_MS = 15 * 60 * 1000L
    private const val MIN_CHANGE = 2

    /**
     * Minutes until [target]% from how fast the level has been moving since the phone was last
     * plugged in or out (within [WINDOW_MS]). Null until there are at least 15 minutes and a 2%
     * change to go on, so a fresh reading never shows a wild guess.
     */
    fun minutesTo(samples: List<BatterySample>, nowMillis: Long, charging: Boolean, target: Int): Int? {
        val session = samples.sortedBy { it.atMillis }
            .takeLastWhile { it.charging == charging }
            .filter { nowMillis - it.atMillis <= WINDOW_MS }
        if (session.size < 2) return null
        val first = session.first()
        val last = session.last()
        val span = last.atMillis - first.atMillis
        val change = if (charging) last.level - first.level else first.level - last.level
        if (span < MIN_SPAN_MS || change < MIN_CHANGE) return null
        val left = if (charging) target - last.level else last.level - target
        if (left <= 0) return 0
        val msPerPercent = span.toDouble() / change
        val sinceLast = nowMillis - last.atMillis
        return ((left * msPerPercent - sinceLast).coerceAtLeast(0.0) / 60_000.0).toInt()
    }

    fun encode(samples: List<BatterySample>): String =
        samples.joinToString(";") { "${it.atMillis},${it.level},${if (it.charging) 1 else 0}" }

    fun decode(raw: String?): List<BatterySample> =
        raw.orEmpty().split(";").mapNotNull { part ->
            val bits = part.split(",")
            val at = bits.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val level = bits.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..100 } ?: return@mapNotNull null
            BatterySample(at, level, bits.getOrNull(2) == "1")
        }
}

/**
 * Keeps the last few hours of battery levels on the phone (never sent anywhere), so Meena can
 * estimate time left when Android doesn't. Android announces every level change while Meena runs.
 */
object BatteryHistory {
    private const val PREFS = "meena_battery_history"
    private const val KEY = "samples"
    private const val MAX_SAMPLES = 200

    @Volatile
    private var listening = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = record(context, intent)
    }

    /** System broadcasts arrive even when not exported (see the broadcasts guide). */
    fun startListening(context: Context) {
        if (listening) return
        val sticky = ContextCompat.registerReceiver(
            context.applicationContext, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        listening = true
        sticky?.let { record(context, it) }
    }

    fun samples(context: Context): List<BatterySample> =
        BatteryRules.decode(prefs(context).getString(KEY, null))

    private fun record(context: Context, intent: Intent) {
        val level = DeviceTelemetryHelper.batteryPercent(
            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        ) ?: return
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val now = System.currentTimeMillis()
        val current = samples(context)
        val last = current.lastOrNull()
        // The broadcast also fires for temperature and voltage; only a new level or plug state is kept.
        if (last != null && last.level == level && last.charging == charging) return
        val kept = (current + BatterySample(now, level, charging))
            .filter { now - it.atMillis <= BatteryRules.WINDOW_MS * 2 }
            .takeLast(MAX_SAMPLES)
        prefs(context).edit().putString(KEY, BatteryRules.encode(kept)).apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
