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
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.ui.widgets.WidgetState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class BriefData(
    val summaryText: String,
    val modelUsed: String,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Manages the Gemini AI daily brief synthesis.
 * Uses only user-permitted sources (calendar, weather, prayer, spending).
 * Reads the API key strictly from encrypted Keystore storage.
 * Follows Meena UI copy: 2-3 casual lowercase sentences, zero jargon, zero emojis.
 */
object GeminiBriefRepository {
    private const val TAG = "GeminiBriefRepository"
    private const val CACHE_DURATION_MS = 2 * 60 * 60 * 1000L // 2 hours fresh

    private val _state = MutableStateFlow<WidgetState<BriefData>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<BriefData>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun testApiKeyAndFetchModels(apiKey: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("api key is empty"))
                val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (response.code == 400 || response.code == 403) {
                    return@withContext Result.failure(IllegalArgumentException("invalid api key"))
                }
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("http error ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: return@withContext Result.failure(IllegalStateException("empty response"))
                val json = JSONObject(bodyStr)
                val modelsArray = json.optJSONArray("models") ?: JSONArray()
                val supported = mutableListOf<String>()

                for (i in 0 until modelsArray.length()) {
                    val m = modelsArray.getJSONObject(i)
                    val name = m.getString("name").removePrefix("models/")
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    var canGenerate = false
                    if (methods != null) {
                        for (j in 0 until methods.length()) {
                            if (methods.getString(j) == "generateContent") {
                                canGenerate = true
                                break
                            }
                        }
                    }
                    if (canGenerate && (name.contains("flash") || name.contains("pro"))) {
                        supported.add(name)
                    }
                }

                if (supported.isEmpty()) {
                    supported.addAll(listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro"))
                }

                Result.success(supported)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun refresh(context: Context, settings: MeenaUserSettings, force: Boolean = false) {
        val apiKey = KeystoreSecretsManager.getGeminiApiKey(context)
        if (apiKey.isBlank()) {
            _state.value = WidgetState.NeedsSetup
            return
        }

        val currentState = _state.value
        val now = System.currentTimeMillis()

        if (!force && currentState is WidgetState.Ready) {
            if (now - currentState.updatedAt < CACHE_DURATION_MS) {
                return
            }
        }

        if (activeJob?.isActive == true) return

        if (currentState is WidgetState.Ready) {
            _state.value = WidgetState.Stale(currentState.data, currentState.updatedAt)
        } else if (currentState !is WidgetState.Stale) {
            _state.value = WidgetState.Loading
        }

        activeJob = scope.launch {
            try {
                val modelName = settings.geminiModel.ifBlank { "gemini-1.5-flash" }
                val contextData = buildContextString(settings)

                val prompt = "You are Meena, a calm, personal executive assistant launcher. " +
                        "Summarize the user's day in exactly 2 to 3 short sentences. " +
                        "Write in 100% lowercase text only. " +
                        "No ALL CAPS, no emojis, no bullet points, no markdown formatting, no symbols, and no jargon. " +
                        "Casual, warm, and helpful tone.\n\n" +
                        "Data for today:\n$contextData"

                val briefText = generateContent(apiKey, modelName, prompt)
                if (briefText != null) {
                    _state.value = WidgetState.Ready(
                        BriefData(summaryText = briefText.lowercase().trim(), modelUsed = modelName),
                        System.currentTimeMillis()
                    )
                } else {
                    handleFailure("couldn't create your brief")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Brief generation failed", e)
                handleFailure("couldn't reach brief service", e)
            }
        }
    }

    private fun handleFailure(message: String, throwable: Throwable? = null) {
        val current = _state.value
        if (current is WidgetState.Stale) {
            Log.d(TAG, "Retaining stale brief data on failure")
        } else {
            _state.value = WidgetState.Error(message, throwable)
        }
    }

    private fun buildContextString(settings: MeenaUserSettings): String {
        val sb = StringBuilder()

        if (settings.geminiIncludeCalendar) {
            val calEvents = (DeviceCalendarRepository.state.value as? CalendarState.Ready)?.events
                ?.filter { it.formattedDate == "today" }
                ?: emptyList()
            if (calEvents.isNotEmpty()) {
                sb.append("calendar: ").append(calEvents.joinToString("; ") { "${it.formattedTime} ${it.title}" }).append("\n")
            } else {
                sb.append("calendar: no scheduled events today\n")
            }
        }

        if (settings.geminiIncludeWeather) {
            val weather = (WeatherRepository.state.value as? WidgetState.Ready)?.data
                ?: (WeatherRepository.state.value as? WidgetState.Stale)?.data
            if (weather != null) {
                sb.append("weather: ${weather.current.temperature.toInt()}°c, ${weather.current.condition}, ${weather.locationName}\n")
            }
        }

        if (settings.geminiIncludeSolat) {
            val solat = (JakimSolatRepository.state.value as? WidgetState.Ready)?.data
                ?: (JakimSolatRepository.state.value as? WidgetState.Stale)?.data
            if (solat != null && solat.nextPrayerName.isNotBlank()) {
                sb.append("prayer: upcoming is ${solat.nextPrayerName} at ${solat.nextPrayerTime}\n")
            }
        }

        if (settings.geminiIncludeFinance) {
            val txs = (SpendingRepository.state.value as? WidgetState.Ready)?.data ?: emptyList()
            if (txs.isNotEmpty()) {
                val total = txs.sumOf { it.amount }
                sb.append("spending: ${txs.size} payments today totaling rm ${String.format("%.2f", total)}\n")
            }
        }

        return sb.toString().ifBlank { "no items scheduled today" }
    }

    private suspend fun generateContent(apiKey: String, model: String, prompt: String): String? {
        return withContext(Dispatchers.IO) {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey.trim()}"

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 120)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.code == 429) {
                throw IllegalStateException("quota reached. try again in a bit.")
            }
            if (!response.isSuccessful) return@withContext null

            val respString = response.body?.string() ?: return@withContext null
            val root = JSONObject(respString)
            val candidates = root.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null

            parts.getJSONObject(0).optString("text")
        }
    }
}
