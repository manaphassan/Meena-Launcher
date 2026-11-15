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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Everything "add ticker" can find: Frankfurter's list of currencies, metals and coins
 * (https://api.frankfurter.dev/v2/currencies, keyless). Loaded when the picker opens and kept
 * for a week, since the list rarely changes.
 */
object TickerCatalogueRepository {
    private const val TAG = "TickerCatalogue"
    private const val PREFS_NAME = "meena_ticker_catalogue"
    private const val KEY_JSON = "catalogue_json"
    private const val KEY_TIME = "catalogue_timestamp"
    private const val MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L

    sealed class State {
        object Loading : State()
        data class Ready(val tickers: List<Ticker>) : State()
        object Failed : State()
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun load(context: Context) {
        val appContext = context.applicationContext
        scope.launch { loadInternal(appContext) }
    }

    private suspend fun loadInternal(context: Context) = mutex.withLock {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_JSON, null)?.let { Markets.parseCatalogue(it) }.orEmpty()
        val age = System.currentTimeMillis() - prefs.getLong(KEY_TIME, 0L)
        if (saved.isNotEmpty()) {
            _state.value = State.Ready(saved)
            if (age < MAX_AGE_MS) return@withLock
        } else {
            _state.value = State.Loading
        }
        try {
            httpClient.newCall(Request.Builder().url(Markets.CATALOGUE_URL).build()).execute().use { response ->
                val body = if (response.isSuccessful) response.body?.string().orEmpty() else ""
                val fresh = Markets.parseCatalogue(body)
                if (fresh.isNotEmpty()) {
                    _state.value = State.Ready(fresh)
                    prefs.edit {
                        putString(KEY_JSON, body)
                        putLong(KEY_TIME, System.currentTimeMillis())
                    }
                } else if (saved.isEmpty()) {
                    _state.value = State.Failed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load the ticker list", e)
            if (saved.isEmpty()) _state.value = State.Failed
        }
    }
}
