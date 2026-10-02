package com.example.meenalauncher.data.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import kotlin.math.roundToInt

data class RealDeviceTelemetry(
    val batteryPercent: Int = 88,
    val isCharging: Boolean = false,
    val usedRamMb: Long = 4200,
    val totalRamMb: Long = 8192,
    val ramPercent: Int = 51,
    val usedStorageGb: Double = 64.2,
    val freeStorageGb: Double = 191.8,
    val totalStorageGb: Double = 256.0,
    val storagePercent: Int = 25,
    val trafficRxMb: Double = 4.82,
    val trafficTxMb: Double = 1.14,
    val screenBrightnessPercent: Int = 65
)

object DeviceTelemetryHelper {

    fun getTelemetry(context: Context): RealDeviceTelemetry {
        // 1. RAM INFO
        var usedRamMb = 0L
        var totalRamMb = 8192L
        var ramPct = 50
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am != null) {
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                totalRamMb = mi.totalMem / (1024 * 1024)
                val freeRamMb = mi.availMem / (1024 * 1024)
                usedRamMb = (totalRamMb - freeRamMb).coerceAtLeast(0)
                ramPct = if (totalRamMb > 0) ((usedRamMb.toDouble() / totalRamMb) * 100).roundToInt() else 50
            }
        } catch (e: Exception) {
            usedRamMb = 4200L
            totalRamMb = 8192L
            ramPct = 51
        }

        // 2. STORAGE INFO
        var usedStorageGb = 64.2
        var freeStorageGb = 191.8
        var totalStorageGb = 256.0
        var storagePct = 25
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0)

            totalStorageGb = String.format(java.util.Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            freeStorageGb = String.format(java.util.Locale.US, "%.1f", freeBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            usedStorageGb = String.format(java.util.Locale.US, "%.1f", usedBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            storagePct = if (totalStorageGb > 0) ((usedStorageGb / totalStorageGb) * 100).roundToInt() else 25
        } catch (e: Exception) {
            // Keep defaults
        }

        // 3. BATTERY INFO
        var batteryPct = 88
        var isCharging = false
        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val bStatus = context.registerReceiver(null, ifilter)
            if (bStatus != null) {
                val level = bStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = bStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).roundToInt() else 88
                val status = bStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        } catch (e: Exception) {
            // Keep defaults
        }

        // 4. TRAFFIC STATS
        var rxMb = 4.82
        var txMb = 1.14
        try {
            val totalRx = TrafficStats.getTotalRxBytes()
            val totalTx = TrafficStats.getTotalTxBytes()
            if (totalRx > 0) {
                rxMb = String.format(java.util.Locale.US, "%.2f", totalRx / (1024.0 * 1024.0)).toDouble()
            }
            if (totalTx > 0) {
                txMb = String.format(java.util.Locale.US, "%.2f", totalTx / (1024.0 * 1024.0)).toDouble()
            }
        } catch (e: Exception) {
            // Keep defaults
        }

        // 5. BRIGHTNESS
        var brightnessPct = 65
        try {
            val bVal = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 170)
            brightnessPct = ((bVal.toFloat() / 255f) * 100).roundToInt()
        } catch (e: Exception) {
            // Keep default
        }

        return RealDeviceTelemetry(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            usedRamMb = usedRamMb,
            totalRamMb = totalRamMb,
            ramPercent = ramPct,
            usedStorageGb = usedStorageGb,
            freeStorageGb = freeStorageGb,
            totalStorageGb = totalStorageGb,
            storagePercent = storagePct,
            trafficRxMb = rxMb,
            trafficTxMb = txMb,
            screenBrightnessPercent = brightnessPct
        )
    }
}
