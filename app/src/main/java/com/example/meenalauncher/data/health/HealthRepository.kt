package com.example.meenalauncher.data.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConnectedDeviceInfo(
    val name: String = "Xiaomi Smart Band 9 E34F",
    val connectionType: String = "BT",
    val isConnected: Boolean = true,
    val batteryPercent: Int = 63,
    val bodyTempCelsius: Double = 34.0,
    val lastSyncTime: String = "12:09 PM"
)

data class FitTelemetry(
    val stepsToday: Int = 6842,
    val stepGoal: Int = 10000,
    val activeCaloriesKcal: Int = 485,
    val activeMinutes: Int = 42,
    val distanceKm: Double = 5.12,
    val heartRateBpm: Int = 72,
    val restingHeartRateBpm: Int = 64,
    val maxHeartRateBpm: Int = 132,
    val sleepHoursFormatted: String = "7h 24m",
    val deepSleepFormatted: String = "2h 10m",
    val sleepScore: Int = 86
)

data class AirQualityIndexInfo(
    val aqiValue: Int = 52,
    val aqiStatus: String = "MODERATE",
    val aqiColorHex: String = "#FFB800",
    val stationName: String = "Kuala Lumpur • Cheras AQM Station",
    val pm25: Double = 13.8,
    val pm10: Double = 28.4,
    val humidityPercent: Int = 72,
    val uvIndex: Int = 8,
    val uvCategory: String = "VERY HIGH",
    val advisory: String = "Air quality is acceptable for most people. Sensitive groups should avoid prolonged outdoor exertion."
)

object HealthRepository {

    private val _deviceInfo = MutableStateFlow(ConnectedDeviceInfo())
    val deviceInfo: StateFlow<ConnectedDeviceInfo> = _deviceInfo.asStateFlow()

    private val _fitTelemetry = MutableStateFlow(FitTelemetry())
    val fitTelemetry: StateFlow<FitTelemetry> = _fitTelemetry.asStateFlow()

    private val _airIndex = MutableStateFlow(AirQualityIndexInfo())
    val airIndex: StateFlow<AirQualityIndexInfo> = _airIndex.asStateFlow()

    fun refreshTelemetry() {
        val nowStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        _deviceInfo.value = _deviceInfo.value.copy(lastSyncTime = nowStr)
    }

    /**
     * Launch connected device companion / fitness app (Mi Fitness, Health Connect, Google Fit).
     */
    fun openFitnessApp(context: Context) {
        val candidatePackages = listOf(
            "com.xiaomi.wearable",       // Mi Fitness
            "com.google.android.apps.fitness", // Google Fit
            "com.google.android.apps.healthdata", // Health Connect
            "com.sec.android.app.shealth", // Samsung Health
            "com.huami.watch.hmwatchmanager" // Zepp Life
        )

        for (pkg in candidatePackages) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return
            }
        }

        // Fallback: Open Bluetooth settings to manage connected device
        try {
            val btIntent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(btIntent)
        } catch (_: Exception) {}
    }

    /**
     * Launch web air quality station for detailed pollution map.
     */
    fun openAirQualityDetails(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.aqi.in/dashboard/malaysia/kuala-lumpur")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
