package my.hana.meena.data.finance

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ExchangeRateItem(
    val currency: String,
    val rateToBase: Double,      // e.g. 1 USD = 4.08 MYR
    val baseToCurrency: Double   // e.g. 1 MYR = 0.2448 USD
)

data class ExchangeRatesData(
    val baseCurrency: String,
    val dateString: String,
    val rates: List<ExchangeRateItem>
)

/**
 * Live Foreign Exchange Rates Repository backed by Frankfurter (European Central Bank reference rates).
 * Free tier, no API key required, reliable daily reference rates.
 * Follows Meena UI Hard Rule 1: No fake rates, cache-backed offline resilience.
 */
object ExchangeRateRepository : WidgetSpec<ExchangeRatesData> {
    private const val TAG = "ExchangeRateRepository"
    private const val PREFS_NAME = "meena_exchange_rates_cache"
    private const val KEY_CACHED_JSON = "cached_rates_json"
    private const val KEY_CACHED_TIME = "cached_rates_timestamp"

    override val id = "widget-exchange-rates"
    override val title = "exchange rates"
    override val source = "European Central Bank via Frankfurter"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 60

    private val _state = MutableStateFlow<WidgetState<ExchangeRatesData>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<ExchangeRatesData>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Default target currencies of interest to display
    private val defaultTargetCurrencies = listOf(
        "USD", "SGD", "EUR", "GBP", "JPY", "CNY", "AUD", "THB", "IDR"
    )

    fun initialize(context: Context, baseCurrency: String = "MYR") {
        // Load cache if available
        loadCachedData(context, baseCurrency)
        // Refresh live data
        refresh(context, baseCurrency)
    }

    override fun fetchInitialState(): WidgetState<ExchangeRatesData> = _state.value

    override suspend fun refresh(): WidgetState<ExchangeRatesData> {
        refreshInternal("MYR")
        return _state.value
    }

    fun refresh(context: Context, baseCurrency: String = "MYR", force: Boolean = false) {
        scope.launch {
            refreshInternal(baseCurrency, context)
        }
    }

    private suspend fun refreshInternal(baseCurrency: String, context: Context? = null) {
        val current = _state.value
        if (current is WidgetState.Ready) {
            _state.value = WidgetState.Stale(current.data, current.updatedAt)
        } else if (current !is WidgetState.Stale) {
            _state.value = WidgetState.Loading
        }

        try {
            val url = "https://api.frankfurter.dev/v1/latest?base=${baseCurrency.uppercase()}"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                handleFailure("couldn't fetch exchange rates (${response.code})")
                return
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                handleFailure("empty response from exchange rate service")
                return
            }

            val parsedData = parseRatesJson(body, baseCurrency.uppercase())
            if (parsedData != null) {
                val now = System.currentTimeMillis()
                _state.value = WidgetState.Ready(parsedData, now)

                context?.let { ctx ->
                    saveCachedData(ctx, body, now)
                }
            } else {
                handleFailure("unable to parse exchange rate data")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch live exchange rates", e)
            handleFailure("network error. tap to retry.")
        }
    }

    private fun handleFailure(message: String) {
        val current = _state.value
        if (current is WidgetState.Stale) {
            // Keep showing stale data with freshness warning
            Log.d(TAG, "Retaining stale exchange rates on failure")
        } else {
            _state.value = WidgetState.Error(message)
        }
    }

    private fun parseRatesJson(jsonString: String, baseCurrency: String): ExchangeRatesData? {
        return try {
            val root = JSONObject(jsonString)
            val dateStr = root.optString("date", "")
            val ratesObj = root.optJSONObject("rates") ?: return null

            val items = mutableListOf<ExchangeRateItem>()
            for (target in defaultTargetCurrencies) {
                if (target.equals(baseCurrency, ignoreCase = true)) continue
                if (ratesObj.has(target)) {
                    val rate = ratesObj.getDouble(target)
                    if (rate > 0.0) {
                        // rate is 1 baseCurrency = X targetCurrency
                        // reciprocal is 1 targetCurrency = (1 / rate) baseCurrency
                        val reciprocal = 1.0 / rate
                        items.add(
                            ExchangeRateItem(
                                currency = target,
                                rateToBase = reciprocal,
                                baseToCurrency = rate
                            )
                        )
                    }
                }
            }

            ExchangeRatesData(
                baseCurrency = baseCurrency,
                dateString = dateStr,
                rates = items
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing rates json", e)
            null
        }
    }

    private fun loadCachedData(context: Context, baseCurrency: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_CACHED_JSON, null) ?: return
        val timestamp = prefs.getLong(KEY_CACHED_TIME, 0L)

        val parsed = parseRatesJson(cachedJson, baseCurrency)
        if (parsed != null) {
            _state.value = WidgetState.Stale(parsed, timestamp)
        }
    }

    private fun saveCachedData(context: Context, jsonString: String, timestamp: Long) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_CACHED_JSON, jsonString)
                .putLong(KEY_CACHED_TIME, timestamp)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save exchange rates cache", e)
        }
    }
}
