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
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Kijang Emas buy and sell prices from Bank Negara Malaysia's open API, published once each
 * business day. Asks for this month's prices (one call), and last month's only when this month
 * has a single day so far, so there is always a price to compare with.
 */
object KijangEmasRepository : WidgetSpec<KijangEmasData> {
    private const val TAG = "KijangEmasRepository"
    private const val PREFS_NAME = "meena_kijang_emas_cache"
    private const val KEY_CACHED_JSON = "cached_days_json"
    private const val KEY_CACHED_TIME = "cached_days_timestamp"
    private const val MIN_REFRESH_MS = 3 * 60 * 60 * 1000L

    override val id = "widget-kijang-emas"
    override val title = "kijang emas"
    override val source = "Bank Negara Malaysia Open API"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 180

    private val _state = MutableStateFlow<WidgetState<KijangEmasData>>(WidgetState.Loading)
    val state: StateFlow<WidgetState<KijangEmasData>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var initialized = false

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        if (!initialized) {
            initialized = true
            loadCachedData(appContext)
        }
        refresh(appContext, force = false)
    }

    override fun fetchInitialState(): WidgetState<KijangEmasData> = _state.value

    override suspend fun refresh(): WidgetState<KijangEmasData> {
        refreshInternal(null, force = true)
        return _state.value
    }

    fun refresh(context: Context, force: Boolean = true) {
        val appContext = context.applicationContext
        scope.launch { refreshInternal(appContext, force) }
    }

    private suspend fun refreshInternal(context: Context?, force: Boolean) = mutex.withLock {
        val current = _state.value
        val now = System.currentTimeMillis()
        if (!force && current is WidgetState.Ready && now - current.updatedAt < MIN_REFRESH_MS) return@withLock

        if (current is WidgetState.Ready) {
            _state.value = WidgetState.Stale(current.data, current.updatedAt)
        } else if (current !is WidgetState.Stale) {
            _state.value = WidgetState.Loading
        }

        try {
            // Bank Negara dates its prices in Malaysian time.
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur")).apply { timeInMillis = now }
            var days = fetchMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
            if (days.size < 2) {
                calendar.add(Calendar.MONTH, -1)
                days = fetchMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1) + days
            }
            val latestTwo = days.sortedBy { it.date }.takeLast(2)
            val summary = KijangEmas.summarise(latestTwo)
            if (summary == null) {
                handleFailure("no kijang emas prices came back. tap to try again.")
                return@withLock
            }
            _state.value = WidgetState.Ready(summary, now)
            context?.let { saveCachedData(it, KijangEmas.encode(latestTwo), now) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load kijang emas prices", e)
            handleFailure("couldn't reach bank negara. tap to try again.")
        }
    }

    private fun fetchMonth(year: Int, month: Int): List<KijangEmasDay> {
        val request = Request.Builder()
            .url(KijangEmas.monthUrl(year, month))
            .header("Accept", KijangEmas.ACCEPT)
            .build()
        httpClient.newCall(request).execute().use { response ->
            // An empty month answers 404 "no records found", which just means no prices yet.
            if (response.code == 404) return emptyList()
            if (!response.isSuccessful) throw IllegalStateException("http ${response.code}")
            return KijangEmas.parseDays(response.body?.string().orEmpty())
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
        val summary = KijangEmas.summarise(KijangEmas.decode(cached)) ?: return
        val fresh = System.currentTimeMillis() - timestamp < MIN_REFRESH_MS
        _state.value = if (fresh) WidgetState.Ready(summary, timestamp) else WidgetState.Stale(summary, timestamp)
    }

    private fun saveCachedData(context: Context, json: String, timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_CACHED_JSON, json)
            putLong(KEY_CACHED_TIME, timestamp)
        }
    }
}
