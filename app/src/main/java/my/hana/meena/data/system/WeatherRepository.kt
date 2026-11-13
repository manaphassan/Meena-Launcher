package my.hana.meena.data.system

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.hana.meena.ui.widgets.WidgetState
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CurrentWeather(
    val temperature: Float,
    val apparentTemperature: Float,
    val humidity: Int,
    val weatherCode: Int,
    val condition: String,
    val windSpeed: Float,
    val isDay: Boolean,
    val locationName: String
)

data class DailyForecast(
    val date: String,
    val dayLabel: String,
    val weatherCode: Int,
    val condition: String,
    val maxTemp: Float,
    val minTemp: Float,
    val precipitationProbability: Int
)

data class WeatherData(
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val fetchedAt: Long,
    /** The next few forecast hours, for the rain line. Empty when the forecast had none. */
    val rainHours: List<RainHour> = emptyList()
)

/**
 * Weather repository driven by Open-Meteo's free, keyless forecast API.
 * Adheres to Meena UI principles: no fabricated data, exact state machine,
 * and 30-minute target freshness cache.
 */
object WeatherRepository {
    private const val TAG = "WeatherRepository"
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes per plan §3.4

    private val _state = MutableStateFlow<WidgetState<WeatherData>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<WeatherData>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun weatherCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "clear sky"
            1 -> "mainly clear"
            2 -> "partly cloudy"
            3 -> "overcast"
            45, 48 -> "foggy"
            51, 53, 55 -> "drizzle"
            56, 57 -> "freezing drizzle"
            61 -> "light rain"
            63 -> "rain"
            65 -> "heavy rain"
            66, 67 -> "freezing rain"
            71 -> "light snow"
            73 -> "snow"
            75 -> "heavy snow"
            77 -> "snow grains"
            80, 81, 82 -> "rain showers"
            85, 86 -> "snow showers"
            95 -> "thunderstorm"
            96, 99 -> "thunderstorm with hail"
            else -> "cloudy"
        }
    }

    fun refresh(context: Context, force: Boolean = false) {
        if (!DeviceLocationHelper.hasLocationPermission(context)) {
            _state.value = WidgetState.NeedsSetup
            return
        }

        val currentState = _state.value
        val now = System.currentTimeMillis()

        if (!force && currentState is WidgetState.Ready) {
            if (now - currentState.updatedAt < CACHE_DURATION_MS) {
                // Cache is fresh
                return
            }
        }

        if (activeJob?.isActive == true) return

        val location = DeviceLocationHelper.getCurrentLocation(context)
        val lat = location.latitude
        val lng = location.longitude
        val locationName = location.locationName

        // If we already had ready data, keep it as stale while loading
        if (currentState is WidgetState.Ready) {
            _state.value = WidgetState.Stale(currentState.data, currentState.updatedAt)
        } else if (currentState !is WidgetState.Stale) {
            _state.value = WidgetState.Loading
        }

        activeJob = scope.launch {
            try {
                val weatherData = fetchOpenMeteo(lat, lng, locationName)
                if (weatherData != null) {
                    Log.i(TAG, "Fetched weather successfully: ${weatherData.current.temperature}°C, ${weatherData.current.condition} for $locationName")
                    _state.value = WidgetState.Ready(weatherData, System.currentTimeMillis())
                } else {
                    handleFailure("couldn't parse weather data")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Weather fetch failed", e)
                handleFailure("couldn't reach weather service", e)
            }
        }
    }

    private fun handleFailure(message: String, throwable: Throwable? = null) {
        val current = _state.value
        if (current is WidgetState.Stale) {
            // Keep stale if network failed
            Log.d(TAG, "Retaining stale weather data on network failure")
        } else {
            _state.value = WidgetState.Error(message, throwable)
        }
    }

    private suspend fun fetchOpenMeteo(lat: Double, lng: Double, locationName: String): WeatherData? {
        return withContext(Dispatchers.IO) {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&hourly=precipitation_probability,precipitation&forecast_hours=5&timezone=auto",
                lat,
                lng
            )

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MeenaLauncher/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body?.string() ?: return@withContext null
            val root = JSONObject(bodyString)

            val currentObj = root.getJSONObject("current")
            val currentTemp = currentObj.getDouble("temperature_2m").toFloat()
            val currentApparent = currentObj.optDouble("apparent_temperature", currentTemp.toDouble()).toFloat()
            val currentHumidity = currentObj.optInt("relative_humidity_2m", 0)
            val currentCode = currentObj.getInt("weather_code")
            val currentWind = currentObj.optDouble("wind_speed_10m", 0.0).toFloat()
            val isDay = currentObj.optInt("is_day", 1) == 1

            val currentWeather = CurrentWeather(
                temperature = currentTemp,
                apparentTemperature = currentApparent,
                humidity = currentHumidity,
                weatherCode = currentCode,
                condition = weatherCodeToCondition(currentCode),
                windSpeed = currentWind,
                isDay = isDay,
                locationName = locationName
            )

            val dailyObj = root.optJSONObject("daily")
            val dailyList = mutableListOf<DailyForecast>()

            if (dailyObj != null) {
                val times = dailyObj.optJSONArray("time")
                val codes = dailyObj.optJSONArray("weather_code")
                val maxTemps = dailyObj.optJSONArray("temperature_2m_max")
                val minTemps = dailyObj.optJSONArray("temperature_2m_min")
                val rainProbs = dailyObj.optJSONArray("precipitation_probability_max")

                val count = times?.length() ?: 0
                val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                for (i in 0 until count) {
                    val dateStr = times?.optString(i) ?: ""
                    val code = codes?.optInt(i, 0) ?: 0
                    val maxT = maxTemps?.optDouble(i, 0.0)?.toFloat() ?: 0f
                    val minT = minTemps?.optDouble(i, 0.0)?.toFloat() ?: 0f
                    val rainProb = rainProbs?.optInt(i, 0) ?: 0

                    val label = try {
                        val parsed = dateParser.parse(dateStr)
                        if (parsed != null) MalaysianEnglish.dayName(parsed.time, System.currentTimeMillis()) else dateStr
                    } catch (_: Exception) {
                        dateStr
                    }

                    dailyList.add(
                        DailyForecast(
                            date = dateStr,
                            dayLabel = label,
                            weatherCode = code,
                            condition = weatherCodeToCondition(code),
                            maxTemp = maxT,
                            minTemp = minT,
                            precipitationProbability = rainProb
                        )
                    )
                }
            }

            // Hourly times are local ISO times; utc_offset_seconds turns them back into instants.
            val rainHours = mutableListOf<RainHour>()
            val hourlyObj = root.optJSONObject("hourly")
            if (hourlyObj != null) {
                val offsetMs = root.optInt("utc_offset_seconds", 0) * 1000L
                val hourParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val hourTimes = hourlyObj.optJSONArray("time")
                val probs = hourlyObj.optJSONArray("precipitation_probability")
                val amounts = hourlyObj.optJSONArray("precipitation")
                for (i in 0 until (hourTimes?.length() ?: 0)) {
                    val localMs =
                        runCatching { hourParser.parse(hourTimes!!.optString(i))?.time }.getOrNull() ?: continue
                    if (probs == null || probs.isNull(i)) continue
                    rainHours += RainHour(
                        endMillis = localMs - offsetMs,
                        probability = probs.optInt(i, 0),
                        millimetres = amounts?.optDouble(i, 0.0)?.toFloat() ?: 0f
                    )
                }
            }

            WeatherData(
                current = currentWeather,
                daily = dailyList,
                latitude = lat,
                longitude = lng,
                locationName = locationName,
                fetchedAt = System.currentTimeMillis(),
                rainHours = rainHours
            )
        }
    }
}
