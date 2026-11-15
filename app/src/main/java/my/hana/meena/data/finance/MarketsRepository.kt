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
import java.util.concurrent.TimeUnit

/**
 * Daily prices in ringgit for the metals and coins the person follows, with a year of history.
 * Source: Frankfurter v2 blended rates (https://frankfurter.dev), keyless; its FAQ says blended
 * rates are free for commercial use and may be cached. These are reference prices published once
 * or a few times a day, not live quotes, and the widget says so.
 */
object MarketsRepository : WidgetSpec<MarketsData> {
    private const val TAG = "MarketsRepository"
    private const val PREFS_NAME = "meena_markets_cache"
    private const val KEY_CACHED_JSON = "cached_markets_json"
    private const val KEY_CACHED_TIME = "cached_markets_timestamp"
    /** Prices change a few times a day at most, so asking more often only wastes data. */
    private const val MIN_REFRESH_MS = 3 * 60 * 60 * 1000L

    override val id = "widget-markets"
    override val title = "markets"
    override val source = "Frankfurter (blended central bank reference rates)"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 180

    private val _state = MutableStateFlow<WidgetState<MarketsData>>(WidgetState.Loading)
    val state: StateFlow<WidgetState<MarketsData>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var initialized = false

    @Volatile
    private var codes: List<String> = Markets.DEFAULT_TICKERS

    fun initialize(context: Context, tickers: List<String> = Markets.DEFAULT_TICKERS) {
        val appContext = context.applicationContext
        val changed = tickers != codes
        codes = tickers
        if (!initialized) {
            initialized = true
            loadCachedData(appContext)
        }
        if (tickers.isEmpty()) return
        // A ticker the saved copy doesn't have yet needs a fresh load.
        val have = (_state.value as? WidgetState.Ready)?.data?.quotes?.map { it.code }.orEmpty() +
            (_state.value as? WidgetState.Stale)?.data?.quotes?.map { it.code }.orEmpty()
        refresh(appContext, force = changed && !have.containsAll(tickers))
    }

    override fun fetchInitialState(): WidgetState<MarketsData> = _state.value

    override suspend fun refresh(): WidgetState<MarketsData> {
        refreshInternal(null, force = true)
        return _state.value
    }

    fun refresh(context: Context, force: Boolean = true) {
        val appContext = context.applicationContext
        scope.launch { refreshInternal(appContext, force) }
    }

    private suspend fun refreshInternal(context: Context?, force: Boolean) = mutex.withLock {
        val current = _state.value
        val updatedAt = when (current) {
            is WidgetState.Ready -> current.updatedAt
            is WidgetState.Stale -> current.updatedAt
            else -> 0L
        }
        val now = System.currentTimeMillis()
        if (!force && current is WidgetState.Ready && now - updatedAt < MIN_REFRESH_MS) return@withLock

        if (current is WidgetState.Ready) {
            _state.value = WidgetState.Stale(current.data, current.updatedAt)
        } else if (current !is WidgetState.Stale) {
            _state.value = WidgetState.Loading
        }

        try {
            val wanted = codes
            if (wanted.isEmpty()) return@withLock
            val request = Request.Builder().url(Markets.url(Markets.oneYearAgo(now), wanted)).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    handleFailure("couldn't load prices (${response.code}). tap to try again.")
                    return@withLock
                }
                val body = response.body?.string().orEmpty()
                val quotes = Markets.toQuotes(Markets.parse(body), wanted)
                if (quotes.isEmpty()) {
                    handleFailure("no prices came back. tap to try again.")
                    return@withLock
                }
                _state.value = WidgetState.Ready(MarketsData(quotes), now)
                context?.let { saveCachedData(it, Markets.encode(quotes), now) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load market prices", e)
            handleFailure("no connection. tap to try again.")
        }
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
        val quotes = Markets.decode(cached)
        if (quotes.isNotEmpty()) {
            val fresh = System.currentTimeMillis() - timestamp < MIN_REFRESH_MS
            _state.value = if (fresh) WidgetState.Ready(MarketsData(quotes), timestamp)
            else WidgetState.Stale(MarketsData(quotes), timestamp)
        }
    }

    private fun saveCachedData(context: Context, json: String, timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_CACHED_JSON, json)
            putLong(KEY_CACHED_TIME, timestamp)
        }
    }
}
