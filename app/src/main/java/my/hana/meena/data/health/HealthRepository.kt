package my.hana.meena.data.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConnectedDeviceInfo(
    val name: String = "",
    val connectionType: String = "",
    val isConnected: Boolean = false,
    val batteryPercent: Int = 0,
    val bodyTempCelsius: Double = 0.0,
    val lastSyncTime: String = ""
)

data class FitTelemetry(
    val stepsToday: Int = 0,
    val stepGoal: Int = 0,
    val activeCaloriesKcal: Int = 0,
    val activeMinutes: Int = 0,
    val distanceKm: Double = 0.0,
    val heartRateBpm: Int = 0,
    val restingHeartRateBpm: Int = 0,
    val maxHeartRateBpm: Int = 0,
    val sleepHoursFormatted: String = "",
    val deepSleepFormatted: String = "",
    val sleepScore: Int = 0
)

data class AirQualityIndexInfo(
    val aqiValue: Int = 0,
    val aqiStatus: String = "",
    val aqiColorHex: String = "#000000",
    val stationName: String = "",
    val pm25: Double = 0.0,
    val pm10: Double = 0.0,
    val humidityPercent: Int = 0,
    val uvIndex: Int = 0,
    val uvCategory: String = "",
    val advisory: String = ""
)

data class HealthData(
    val deviceInfo: ConnectedDeviceInfo = ConnectedDeviceInfo(),
    val fitTelemetry: FitTelemetry = FitTelemetry(),
    val airIndex: AirQualityIndexInfo = AirQualityIndexInfo()
)

object HealthRepository : WidgetSpec<HealthData> {

    override val id = "widget-health"
    override val title = "health"
    override val source = "Health Connect & Local Sensors"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 30

    private val _healthState = MutableStateFlow<WidgetState<HealthData>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<HealthData>> = _healthState.asStateFlow()

    override fun fetchInitialState(): WidgetState<HealthData> {
        return _healthState.value
    }

    override suspend fun refresh(): WidgetState<HealthData> {
        // Will implement actual fetching from Health Connect here. 
        // For now, keeps returning NeedsSetup to avoid hardcoded fake data in live.
        return _healthState.value
    }

    fun refreshTelemetry() {
        // No-op until Health Connect is wired up
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


