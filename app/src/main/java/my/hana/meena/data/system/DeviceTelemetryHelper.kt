package my.hana.meena.data.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import kotlin.math.roundToInt

/**
 * What the phone status widget shows. Every reading is null until the phone actually reports it,
 * so a failed read shows as "unknown" instead of a made-up number.
 */
data class RealDeviceTelemetry(
    val batteryPercent: Int? = null,
    val isCharging: Boolean = false,
    val freeMemoryGb: Double? = null,
    val memoryUsedPercent: Int? = null,
    val freeStorageGb: Double? = null,
    val storageUsedPercent: Int? = null,
    val screenBrightnessPercent: Int? = null,
    /** True when Android says the battery is full while plugged in. */
    val isFull: Boolean = false,
    /** Normal or fast, from the charger's own limits; null when the phone doesn't say. */
    val chargeSpeed: ChargeSpeed? = null,
    /** Minutes to 100% while charging, from Android or else Meena's estimate. */
    val minutesToFull: Int? = null,
    /** Minutes until empty while on battery, from Android or else Meena's estimate. */
    val minutesToEmpty: Int? = null,
    /** True when the time came from Meena's own estimate rather than Android. */
    val isEstimate: Boolean = false
)

object DeviceTelemetryHelper {

    private const val BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0

    /** Battery level as a percentage, or null when the broadcast has no usable level. */
    internal fun batteryPercent(level: Int, scale: Int): Int? =
        if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt().coerceIn(0, 100) else null

    /** Share of [total] in use, or null when the numbers don't make sense. */
    internal fun usedPercent(total: Long, free: Long): Int? =
        if (total > 0 && free in 0..total) (((total - free).toDouble() / total) * 100).roundToInt() else null

    /** Bytes to gigabytes, one decimal place. */
    internal fun toGb(bytes: Long): Double = ((bytes / BYTES_PER_GB) * 10.0).roundToInt() / 10.0

    /** Android stores screen brightness as 0 to 255. */
    internal fun brightnessPercent(raw: Int): Int? =
        if (raw in 0..255) ((raw / 255f) * 100).roundToInt() else null

    fun getTelemetry(context: Context): RealDeviceTelemetry {
        var freeMemoryGb: Double? = null
        var memoryUsed: Int? = null
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return@runCatching
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            memoryUsed = usedPercent(mi.totalMem, mi.availMem)
            if (memoryUsed != null) freeMemoryGb = toGb(mi.availMem)
        }

        var freeStorageGb: Double? = null
        var storageUsed: Int? = null
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            storageUsed = usedPercent(stat.totalBytes, stat.availableBytes)
            if (storageUsed != null) freeStorageGb = toGb(stat.availableBytes)
        }

        var battery: Int? = null
        var isCharging = false
        var isFull = false
        var speed: ChargeSpeed? = null
        runCatching {
            val status =
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return@runCatching
            battery = batteryPercent(
                status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            )
            val state = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = state == BatteryManager.BATTERY_STATUS_CHARGING || state == BatteryManager.BATTERY_STATUS_FULL
            isFull = state == BatteryManager.BATTERY_STATUS_FULL
            // Hidden extras (BatteryManager.EXTRA_MAX_CHARGING_CURRENT / _VOLTAGE), read by their string
            // names; Android Settings uses the same two for "charging rapidly". Missing on some phones.
            if (isCharging) speed = BatteryRules.speed(status.getIntExtra("max_charging_current", -1), status.getIntExtra("max_charging_voltage", -1))
        }

        val now = System.currentTimeMillis()
        var estimate = false
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        // https://developer.android.com/reference/android/os/BatteryManager#computeChargeTimeRemaining()
        val toFull = if (!isCharging || isFull) null else {
            val fromAndroid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { manager?.computeChargeTimeRemaining() }.getOrNull()?.takeIf { it > 0 }?.let { (it / 60_000L).toInt() }
            } else null
            fromAndroid ?: BatteryRules.minutesTo(BatteryHistory.samples(context), now, charging = true, target = 100)?.also { estimate = true }
        }
        // https://developer.android.com/reference/android/os/PowerManager#getBatteryDischargePrediction()
        val toEmpty = if (isCharging) null else {
            val fromAndroid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    (context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.batteryDischargePrediction
                }.getOrNull()?.toMinutes()?.toInt()?.takeIf { it > 0 }
            } else null
            fromAndroid ?: BatteryRules.minutesTo(BatteryHistory.samples(context), now, charging = false, target = 0)?.also { estimate = true }
        }

        // Throws when the phone has no value, which leaves brightness unknown.
        val brightness = runCatching {
            brightnessPercent(Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS))
        }.getOrNull()

        return RealDeviceTelemetry(
            batteryPercent = battery,
            isCharging = isCharging,
            freeMemoryGb = freeMemoryGb,
            memoryUsedPercent = memoryUsed,
            freeStorageGb = freeStorageGb,
            storageUsedPercent = storageUsed,
            screenBrightnessPercent = brightness,
            isFull = isFull,
            chargeSpeed = speed,
            minutesToFull = toFull,
            minutesToEmpty = toEmpty,
            isEstimate = estimate
        )
    }
}
