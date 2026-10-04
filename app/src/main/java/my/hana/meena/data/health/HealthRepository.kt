package my.hana.meena.data.health

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.DeviceLocationHelper
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

data class CompanionAppInfo(
    val packageName: String,
    val displayName: String
)

data class ConnectedDeviceInfo(
    val name: String = "",
    val connectionType: String = "",
    val isConnected: Boolean = false,
    val batteryPercent: Int = 0,
    val bodyTempCelsius: Double = 0.0,
    val lastSyncTime: String = ""
)

data class HealthMetrics(
    val stepsToday: Int = 0,
    val stepGoal: Int = 10000,
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

typealias FitTelemetry = HealthMetrics

data class AirQualityIndexInfo(
    val aqiValue: Int = 0,
    val aqiStatus: String = "",
    val aqiColorHex: String = "#767676",
    val stationName: String = "",
    val pm25: Double = 0.0,
    val pm10: Double = 0.0,
    val humidityPercent: Int = 0,
    val uvIndex: Int = 0,
    val uvCategory: String = "",
    val advisory: String = ""
)

data class HealthData(
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val hasPermissions: Boolean = false,
    val deviceInfo: ConnectedDeviceInfo = ConnectedDeviceInfo(),
    val fitTelemetry: FitTelemetry = FitTelemetry(),
    val airIndex: AirQualityIndexInfo = AirQualityIndexInfo()
)

object HealthRepository : WidgetSpec<HealthData> {

    private const val TAG = "HealthRepository"

    override val id = "widget-health"
    override val title = "health"
    override val source = "Health Connect & Open-Meteo"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 30

    val READ_PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    private val _healthState = MutableStateFlow<WidgetState<HealthData>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<HealthData>> = _healthState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun fetchInitialState(): WidgetState<HealthData> {
        return _healthState.value
    }

    override suspend fun refresh(): WidgetState<HealthData> {
        return _healthState.value
    }

    fun refreshTelemetry(context: Context, userSettings: MeenaUserSettings? = null) {
        if (activeJob?.isActive == true) return

        val currentState = _healthState.value
        if (currentState is WidgetState.Ready) {
            _healthState.value = WidgetState.Stale(currentState.data, currentState.updatedAt)
        } else if (currentState !is WidgetState.Stale) {
            _healthState.value = WidgetState.Loading
        }

        activeJob = scope.launch {
            try {
                val data = loadHealthData(context, userSettings)
                _healthState.value = WidgetState.Ready(data, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.w(TAG, "Failed refreshing health data", e)
                val current = _healthState.value
                if (current !is WidgetState.Stale) {
                    _healthState.value = WidgetState.Error("couldn't read health data", e)
                }
            }
        }
    }

    private suspend fun loadHealthData(context: Context, userSettings: MeenaUserSettings?): HealthData = withContext(Dispatchers.IO) {
        val availability = getHealthConnectAvailability(context)
        var hasPermissions = false
        var telemetry = FitTelemetry(
            stepGoal = userSettings?.stepGoal ?: 10000
        )

        val companionApp = resolveCompanionApp(context, userSettings)
        val deviceInfo = ConnectedDeviceInfo(
            name = companionApp.displayName,
            connectionType = "companion",
            isConnected = companionApp.packageName.isNotBlank()
        )

        if (availability == HealthConnectAvailability.AVAILABLE) {
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val grantedPermissions = client.permissionController.getGrantedPermissions()
                hasPermissions = grantedPermissions.containsAll(READ_PERMISSIONS) ||
                        grantedPermissions.contains(HealthPermission.getReadPermission(StepsRecord::class))

                if (hasPermissions) {
                    telemetry = queryHealthConnectTelemetry(client, userSettings)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error querying Health Connect", e)
            }
        }

        // Fetch live Air Quality from Open-Meteo Air Quality API
        val airQuality = fetchOpenMeteoAirQuality(context)

        HealthData(
            availability = availability,
            hasPermissions = hasPermissions,
            deviceInfo = deviceInfo,
            fitTelemetry = telemetry,
            airIndex = airQuality
        )
    }

    fun getHealthConnectAvailability(context: Context): HealthConnectAvailability {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return HealthConnectAvailability.NOT_SUPPORTED
        }
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
                else -> {
                    // Check if Health Connect APK is installed
                    if (isPackageInstalled(context, "com.google.android.apps.healthdata")) {
                        HealthConnectAvailability.UPDATE_REQUIRED
                    } else {
                        HealthConnectAvailability.NOT_INSTALLED
                    }
                }
            }
        } catch (_: Exception) {
            HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    private suspend fun queryHealthConnectTelemetry(
        client: HealthConnectClient,
        userSettings: MeenaUserSettings?
    ): FitTelemetry {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        var stepsToday = 0
        var distanceKm = 0.0
        var activeCalories = 0

        try {
            val aggregateResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )

            stepsToday = (aggregateResponse[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
            val distMeters = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            distanceKm = distMeters / 1000.0
            activeCalories = aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt() ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Aggregate query failed", e)
        }

        // Query Sleep session in the last 24h
        var sleepFormatted = ""
        try {
            val yesterday = now.minus(24, ChronoUnit.HOURS)
            val sleepRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(yesterday, now)
                )
            ).records

            val latestSession = sleepRecords.maxByOrNull { it.endTime }
            if (latestSession != null) {
                val duration = Duration.between(latestSession.startTime, latestSession.endTime)
                val hours = duration.toHours()
                val minutes = duration.toMinutes() % 60
                sleepFormatted = "${hours}h ${minutes}m"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sleep query failed", e)
        }

        return FitTelemetry(
            stepsToday = stepsToday,
            stepGoal = userSettings?.stepGoal ?: 10000,
            activeCaloriesKcal = activeCalories,
            distanceKm = distanceKm,
            sleepHoursFormatted = sleepFormatted
        )
    }

    private fun resolveCompanionApp(context: Context, userSettings: MeenaUserSettings?): CompanionAppInfo {
        val customPkg = userSettings?.healthCompanionPackage
        val customName = userSettings?.healthCompanionName
        if (!customPkg.isNullOrBlank() && isPackageInstalled(context, customPkg)) {
            return CompanionAppInfo(
                packageName = customPkg,
                displayName = customName.takeUnless { it.isNullOrBlank() }
                    ?: getAppLabel(context, customPkg)
            )
        }

        // Autodetect from installed fitness candidates
        val candidates = getInstalledFitnessApps(context)
        return candidates.firstOrNull() ?: CompanionAppInfo("", "health connect")
    }

    fun getInstalledFitnessApps(context: Context): List<CompanionAppInfo> {
        val knownPackages = listOf(
            "com.xiaomi.wearable" to "mi fitness",
            "com.sec.android.app.shealth" to "samsung health",
            "com.google.android.apps.fitness" to "google fit",
            "com.google.android.apps.healthdata" to "health connect",
            "com.huami.watch.hmwatchmanager" to "zepp life",
            "com.fitbit.FitbitMobile" to "fitbit",
            "com.garmin.android.apps.connectmobile" to "garmin connect",
            "com.strava" to "strava",
            "com.huawei.health" to "huawei health"
        )

        val result = mutableListOf<CompanionAppInfo>()
        for ((pkg, label) in knownPackages) {
            if (isPackageInstalled(context, pkg)) {
                val name = getAppLabel(context, pkg).ifBlank { label }
                result.add(CompanionAppInfo(packageName = pkg, displayName = name.lowercase()))
            }
        }
        return result
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun fetchOpenMeteoAirQuality(context: Context): AirQualityIndexInfo = withContext(Dispatchers.IO) {
        val location = DeviceLocationHelper.getCurrentLocation(context)
        val lat = location.latitude
        val lng = location.longitude
        val locationName = location.locationName.ifBlank { "local area" }

        if (!DeviceLocationHelper.hasLocationPermission(context)) {
            return@withContext AirQualityIndexInfo(
                stationName = "coarse location needed",
                aqiStatus = "needs location"
            )
        }

        try {
            val url = String.format(
                Locale.US,
                "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%.4f&longitude=%.4f&current=us_aqi,pm10,pm2_5&timezone=auto",
                lat,
                lng
            )

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MeenaLauncher/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext AirQualityIndexInfo(stationName = locationName, aqiStatus = "offline")
            }

            val body = response.body?.string() ?: return@withContext AirQualityIndexInfo(stationName = locationName)
            val json = JSONObject(body)
            val current = json.optJSONObject("current") ?: return@withContext AirQualityIndexInfo(stationName = locationName)

            val aqi = current.optInt("us_aqi", 0)
            val pm25 = current.optDouble("pm2_5", 0.0)
            val pm10 = current.optDouble("pm10", 0.0)

            val (status, colorHex, advisory) = when (aqi) {
                in 0..50 -> Triple("good", "#30D158", "air quality is satisfactory.")
                in 51..100 -> Triple("moderate", "#FFD60A", "air quality is acceptable.")
                in 101..150 -> Triple("sensitive groups", "#FF9F0A", "sensitive groups should reduce prolonged outdoor exertion.")
                in 151..200 -> Triple("unhealthy", "#FF453A", "everyone may experience health effects.")
                in 201..300 -> Triple("very unhealthy", "#BF5AF2", "health alert: risk of health effects for all.")
                else -> Triple("hazardous", "#8E8E93", "health warning: emergency conditions.")
            }

            AirQualityIndexInfo(
                aqiValue = aqi,
                aqiStatus = status,
                aqiColorHex = colorHex,
                stationName = locationName,
                pm25 = pm25,
                pm10 = pm10,
                advisory = advisory
            )
        } catch (e: Exception) {
            Log.w(TAG, "Air quality fetch failed", e)
            AirQualityIndexInfo(
                stationName = locationName,
                aqiStatus = "offline"
            )
        }
    }

    /**
     * Launch connected device companion / fitness app (Mi Fitness, Health Connect, Samsung Health).
     */
    fun openFitnessApp(context: Context, preferredPackage: String? = null) {
        if (!preferredPackage.isNullOrBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(preferredPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return
            }
        }

        val candidates = listOf(
            "com.xiaomi.wearable",
            "com.sec.android.app.shealth",
            "com.google.android.apps.fitness",
            "com.google.android.apps.healthdata",
            "com.huami.watch.hmwatchmanager",
            "com.fitbit.FitbitMobile",
            "com.garmin.android.apps.connectmobile"
        )

        for (pkg in candidates) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return
            }
        }

        // Fallback: Open Health Connect settings or Play Store
        openHealthConnectSettings(context)
    }

    /**
     * Open Health Connect settings on device, or redirect to Play Store if not installed.
     */
    fun openHealthConnectSettings(context: Context) {
        try {
            val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openHealthConnectPlayStore(context)
        }
    }

    /**
     * Open Google Play Store listing for Health Connect.
     */
    fun openHealthConnectPlayStore(context: Context) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Launch web air quality station for detailed pollution map.
     */
    fun openAirQualityDetails(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/en/docs/air-quality-api")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
