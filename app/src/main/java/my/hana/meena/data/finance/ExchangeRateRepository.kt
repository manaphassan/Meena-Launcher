package my.hana.meena.data.finance

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ExchangeRateItem(
    val currency: String,
    val rateToBase: Double,      // e.g. 1 USD = 4.08 MYR
    val baseToCurrency: Double,  // e.g. 1 MYR = 0.2448 USD
    /** Change of [rateToBase] since the previous published day, in percent; null with one day only. */
    val changePercent: Double? = null
)

data class ExchangeRatesData(
    val baseCurrency: String,
    val dateString: String,
    val rates: List<ExchangeRateItem>
)

/**
 * Ringgit exchange rates for the currencies the person picked, from Frankfurter v2 blended rates
 * (https://frankfurter.dev, keyless, free for commercial use). Asks for the last ten days, so each
 * rate has a previous day to compare with even after a weekend or holiday.
 * Follows Meena UI Hard Rule 1: no fake rates, cache-backed offline resilience.
 */
object ExchangeRateRepository : WidgetSpec<ExchangeRatesData> {
    private const val TAG = "ExchangeRateRepository"
    private const val PREFS_NAME = "meena_exchange_rates_cache"
    // v2 replies are kept under new keys; the old v1 copy is simply ignored.
    private const val KEY_CACHED_JSON = "cached_rates_v2_json"
    private const val KEY_CACHED_TIME = "cached_rates_v2_timestamp"
    private const val MIN_REFRESH_MS = 60 * 60 * 1000L
    private const val DAYS_BACK = 10

    override val id = "widget-exchange-rates"
    override val title = "exchange rates"
    override val source = "Frankfurter (blended central bank reference rates)"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 60

    private val _state = MutableStateFlow<WidgetState<ExchangeRatesData>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<ExchangeRatesData>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var codes: List<String> = Markets.DEFAULT_RATES

    /** Shows the saved copy for [currencies], then refreshes if it is old or the list changed. */
    fun initialize(context: Context, currencies: List<String>) {
        val appContext = context.applicationContext
        val changed = currencies != codes
        codes = currencies
        if (currencies.isEmpty()) {
            _state.value = WidgetState.Empty
            return
        }
        if (_state.value !is WidgetState.Ready || changed) loadCachedData(appContext)
        refresh(appContext, force = changed)
    }

    override fun fetchInitialState(): WidgetState<ExchangeRatesData> = _state.value

    override suspend fun refresh(): WidgetState<ExchangeRatesData> {
        refreshInternal(null, force = true)
        return _state.value
    }

    fun refresh(context: Context, force: Boolean = true) {
        val appContext = context.applicationContext
        scope.launch { refreshInternal(appContext, force) }
    }

    private suspend fun refreshInternal(context: Context?, force: Boolean) = mutex.withLock {
        val wanted = codes
        if (wanted.isEmpty()) return@withLock
        val current = _state.value
        val now = System.currentTimeMillis()
        if (!force && current is WidgetState.Ready && now - current.updatedAt < MIN_REFRESH_MS) return@withLock

        if (current is WidgetState.Ready) {
            _state.value = WidgetState.Stale(current.data, current.updatedAt)
        } else if (current !is WidgetState.Stale) {
            _state.value = WidgetState.Loading
        }

        try {
            val from = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -DAYS_BACK)
            }
            val url = Markets.url(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(from.time), wanted)
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    handleFailure("couldn't load exchange rates (${response.code}). tap to try again.")
                    return@withLock
                }
                val body = response.body?.string().orEmpty()
                val data = toData(body, wanted)
                if (data == null) {
                    handleFailure("no exchange rates came back. tap to try again.")
                    return@withLock
                }
                _state.value = WidgetState.Ready(data, now)
                context?.let { saveCachedData(it, body, now) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch exchange rates", e)
            handleFailure("no connection. tap to try again.")
        }
    }

    private fun toData(body: String, wanted: List<String>): ExchangeRatesData? {
        val series = Markets.parse(body)
        val items = Markets.rateItems(series, wanted)
        if (items.isEmpty()) return null
        val date = series.values.mapNotNull { it.lastOrNull()?.date }.maxOrNull().orEmpty()
        return ExchangeRatesData(baseCurrency = "MYR", dateString = date, rates = items)
    }

    private fun handleFailure(message: String) {
        if (_state.value !is WidgetState.Stale) {
            _state.value = WidgetState.Error(message)
        }
    }

    private fun loadCachedData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_CACHED_JSON, null) ?: return
        val timestamp = prefs.getLong(KEY_CACHED_TIME, 0L)
        val data = toData(cached, codes) ?: return
        _state.value = WidgetState.Stale(data, timestamp)
    }

    private fun saveCachedData(context: Context, body: String, timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_CACHED_JSON, body)
            putLong(KEY_CACHED_TIME, timestamp)
        }
    }
}
