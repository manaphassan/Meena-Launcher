package my.hana.meena.data.finance

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import my.hana.meena.data.system.KeystoreSecretsManager
import my.hana.meena.data.system.SecureFields
import my.hana.meena.data.system.SecureRead
import my.hana.meena.data.system.SharedPrefsStore
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Google Finance's page for a sheet symbol. Sheets write "KLSE:FFB" but the page address is
 * "FFB:KLSE" (checked on google.com/finance, 16 Nov 2026).
 */
fun googleQuoteUrl(symbol: String): String {
    val clean = symbol.trim().uppercase()
    val page = if (':' in clean) "${clean.substringAfter(':')}:${clean.substringBefore(':')}" else clean
    return "https://www.google.com/finance/quote/$page"
}

/**
 * Data model for a portfolio holding.
 */
data class FinanceHolding(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,             // e.g. "BURSA", "AXIATA", "HLAL"
    val name: String,               // e.g. "Bursa Malaysia Bhd"
    val shares: Double,             // e.g. 500.0
    val averageCost: Double,        // e.g. 7.50
    val currentPrice: Double,       // e.g. 8.90
    val currency: String = "MYR",
    val groupName: String = "mplus" // e.g. "mplus", "luno", "bank"
) {
    /** False when no average cost was given; then there is no gain or loss to show. */
    val hasCost: Boolean get() = averageCost > 0.0
    /** False until a price is known (typed in, or loaded from the sheet). */
    val hasPrice: Boolean get() = currentPrice > 0.0
    val totalCost: Double get() = shares * averageCost
    val currentValue: Double get() = shares * currentPrice
    val profitLoss: Double get() = if (hasCost && hasPrice) currentValue - totalCost else 0.0
    val profitLossPercent: Double get() = if (hasCost && hasPrice) (profitLoss / totalCost) * 100.0 else 0.0
}

/** Value of the holdings that have a price, and the gain or loss of those that also have a cost. */
data class PortfolioTotals(val value: Double, val change: Double?, val changePercent: Double?, val anyPriced: Boolean)

fun portfolioTotals(holdings: List<FinanceHolding>): PortfolioTotals {
    val priced = holdings.filter { it.hasPrice }
    val costed = priced.filter { it.hasCost }
    val cost = costed.sumOf { it.totalCost }
    val change = costed.sumOf { it.profitLoss }
    return PortfolioTotals(
        value = priced.sumOf { it.currentValue },
        change = if (costed.isEmpty()) null else change,
        changePercent = if (cost > 0.0) change / cost * 100.0 else null,
        anyPriced = priced.isNotEmpty()
    )
}

/**
 * Watchlist ticker item.
 */
data class FinanceTicker(
    val symbol: String,             // e.g. "BURSA", "KLCI", "AXIATA"
    val tickerDisplay: String,      // "BURSA", "KLCI", "AXIATA"
    val name: String,
    val exchange: String = "KLSE",
    val currency: String = "MYR",
    val price: Double = 0.0,
    /** Null when the sheet cell is blank or an error such as #N/A, so no made-up 0 is shown. */
    val change: Double? = null,
    val changePercent: Double? = null,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0,
    val sparkline: List<Double> = emptyList(),
    val quoteUrl: String = ""
)

/**
 * Summary account model for portfolio groups.
 */
data class PortfolioAccount(
    val name: String,
    val value: Double,
    val change: Double,
    val changePercent: Double,
    val currency: String = "MYR"
)

/**
 * OHLC Candle model for chart rendering.
 */
data class FinanceCandle(
    val timeLabel: String,
    val dateLabel: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long = 0L,
    val timestamp: Long = 0L
)

/**
 * Complete finance state representation.
 */
data class FinanceData(
    val holdings: List<FinanceHolding> = emptyList(),
    val portfolios: List<PortfolioAccount> = emptyList(),
    val watchlist: List<FinanceTicker> = emptyList(),
    val sheetConnected: Boolean = false,
    val sheetUrl: String = "",
    /** When the sheet last loaded successfully; null if it hasn't loaded since the app started. */
    val sheetSyncedAt: Long? = null,
    /** Coin holdings priced from Frankfurter; null when there are none. */
    val coins: CoinPricing? = null
)

/** "KLSE:FFB", "ffb" and "FFB.KL" all mean FFB, so a holding matches its sheet row however it was typed. */
fun plainTicker(symbol: String): String = symbol.trim().uppercase()
    .removePrefix("KLSE:").removeSuffix(".KL").removePrefix("NASDAQ:").removePrefix("NYSE:")

/** Sheet rows the person doesn't own; owned ones already show as holdings, so they aren't listed twice. */
fun FinanceData.unheldWatchlist(): List<FinanceTicker> {
    val held = holdings.map { plainTicker(it.symbol) }.toSet()
    return watchlist.filter { plainTicker(it.symbol) !in held }
}

/**
 * Compliance-ready Finance Repository.
 * Primary live quote source: Google Sheets bridge (user's sheet with GOOGLEFINANCE formulas).
 * Local portfolio holdings management with zero hardcoded sample data (Hard Rule 1).
 * Replaces trademarked GoogleFinanceRepository and removes unauthorized Yahoo scraping.
 */
object FinanceRepository : WidgetSpec<FinanceData> {
    private const val TAG = "FinanceRepository"
    private const val PREFS_NAME = "meena_finance_data"
    // Plain keys are only read to move data from older builds; new data goes to the enc_ keys.
    private const val KEY_HOLDINGS_JSON = "user_holdings_json"
    private const val KEY_SHEET_URL = "user_sheet_url"
    private const val KEY_HOLDINGS_ENC = "enc_user_holdings_json"
    private const val KEY_SHEET_URL_ENC = "enc_user_sheet_url"
    private const val UNLOCK_FAILED_MESSAGE = "couldn't unlock your portfolio"

    override val id = "widget-finance-charts"
    override val title = "portfolio & watchlist"
    override val source = "Google Sheets Bridge & Local Holdings"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 15

    private val _state = MutableStateFlow<WidgetState<FinanceData>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<FinanceData>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val holdingsList = mutableListOf<FinanceHolding>()
    private val watchlistItems = mutableListOf<FinanceTicker>()
    private var sheetUrlConfigured: String = ""
    private var sheetSyncedAt: Long? = null
    private var coinPricing: CoinPricing? = null
    private var appContext: Context? = null
    /** One price load at a time, so the sheet and the coin prices never write over each other. */
    private val refreshMutex = Mutex()

    /**
     * True when saved holdings exist but can't be decrypted. Saving is blocked so the
     * saved data is never overwritten; only [resetPortfolioStorage] clears it.
     */
    var isStorageLocked: Boolean = false
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadSavedData(context)
        if (isStorageLocked) return
        evaluateState()
        refreshPrices(context)
    }

    /** Deletes saved holdings and the sheet link. Only call after the user says yes. */
    fun resetPortfolioStorage(context: Context) {
        val store = SharedPrefsStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
        listOf(KEY_HOLDINGS_ENC, KEY_HOLDINGS_JSON, KEY_SHEET_URL_ENC, KEY_SHEET_URL).forEach { store.remove(it) }
        holdingsList.clear()
        watchlistItems.clear()
        sheetUrlConfigured = ""
        sheetSyncedAt = null
        coinPricing = null
        isStorageLocked = false
        evaluateState()
    }

    override fun fetchInitialState(): WidgetState<FinanceData> = _state.value

    override suspend fun refresh(): WidgetState<FinanceData> {
        if (isStorageLocked) return _state.value
        refreshAll(appContext)
        return _state.value
    }

    fun getTotalPortfolioValue(): Double {
        return holdingsList.sumOf { it.currentValue }
    }

    fun getTotalPortfolioChange(): Double {
        return holdingsList.sumOf { it.profitLoss }
    }

    fun getTotalPortfolioChangePercent(): Double = portfolioTotals(holdingsList).changePercent ?: 0.0

    fun addHolding(context: Context, holding: FinanceHolding) {
        if (isStorageLocked) return
        holdingsList.removeAll { it.id == holding.id }
        holdingsList.add(holding)
        saveData(context)
        evaluateState()
        // A new coin gets its price straight away.
        val app = context.applicationContext
        scope.launch { refreshMutex.withLock { refreshCoinsInternal(app) } }
    }

    fun removeHolding(context: Context, holdingId: String) {
        if (isStorageLocked) return
        holdingsList.removeAll { it.id == holdingId }
        saveData(context)
        evaluateState()
    }

    fun setSheetUrl(context: Context, url: String) {
        if (isStorageLocked) return
        sheetUrlConfigured = url.trim()
        sheetSyncedAt = null
        saveData(context)
        if (sheetUrlConfigured.isBlank()) evaluateState()
        refreshPrices(context)
    }

    /** Loads the sheet (if linked), then coin prices from Frankfurter. */
    fun refreshPrices(context: Context) {
        if (isStorageLocked) return
        val app = context.applicationContext
        scope.launch { refreshAll(app) }
    }

    private suspend fun refreshAll(context: Context?) = refreshMutex.withLock {
        if (sheetUrlConfigured.isNotBlank()) refreshSheetInternal(sheetUrlConfigured, context)
        refreshCoinsInternal(context)
    }

    /**
     * Prices coin holdings (XRP, BTC, BCH; XBT for Luno's bitcoin) in ringgit from Frankfurter's
     * latest daily rates. A failed load keeps whatever price each holding had and says so;
     * it never fills in a price.
     */
    private suspend fun refreshCoinsInternal(context: Context?) {
        val ctx = context ?: appContext ?: return
        if (isStorageLocked || holdingsList.isEmpty()) {
            coinPricing = null
            publish()
            return
        }
        val catalogue = TickerCatalogueRepository.tickers(ctx)
        if (catalogue.isEmpty()) {
            // Without Frankfurter's list there's no telling which holdings are coins.
            coinPricing = coinPricing?.copy(failed = true)
            publish()
            return
        }
        val sheetSymbols = watchlistItems.map { plainTicker(it.symbol) }.toSet()
        val coins = CoinPrices.coinHoldings(holdingsList.toList(), CoinPrices.priceableCodes(catalogue), sheetSymbols)
        if (coins.isEmpty()) {
            coinPricing = null
            publish()
            return
        }
        val before = coinPricing
        coinPricing = before?.copy(holdingIds = coins.keys, failed = false) ?: CoinPricing(coins.keys)
        publish()

        coinPricing = try {
            val request = Request.Builder().url(CoinPrices.latestUrl(coins.values.toSet())).build()
            httpClient.newCall(request).execute().use { response ->
                val body = if (response.isSuccessful) response.body?.string().orEmpty() else ""
                val latest = CoinPrices.latest(Markets.parse(body))
                val result = CoinPrices.summarise(coins, latest)
                if (result.failed) {
                    Log.w(TAG, "No coin prices came back (http ${response.code})")
                    (before ?: result).copy(holdingIds = coins.keys, failed = true)
                } else {
                    for (i in holdingsList.indices) {
                        holdingsList[i] = CoinPrices.priced(holdingsList[i], coins, latest)
                    }
                    saveData(ctx)
                    result
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load coin prices: ${e.javaClass.simpleName}")
            (before ?: CoinPricing(coins.keys)).copy(holdingIds = coins.keys, failed = true)
        }
        publish()
    }

    /** Shows the latest holdings without hiding a sheet problem: a saved copy stays marked as saved. */
    private fun publish() {
        when (val current = _state.value) {
            is WidgetState.Stale -> _state.value = WidgetState.Stale(buildData(), current.updatedAt)
            is WidgetState.Error -> Unit
            else -> evaluateState()
        }
    }

    private suspend fun refreshSheetInternal(url: String, context: Context? = null) {
        val current = _state.value
        if (current is WidgetState.Ready) {
            _state.value = WidgetState.Stale(current.data, current.updatedAt)
        } else if (current !is WidgetState.Stale) {
            _state.value = WidgetState.Loading
        }

        try {
            // Normalize Google Sheets URL to export as CSV if it's a docs.google.com link
            val csvUrl = normalizeSheetUrl(url)
            val request = Request.Builder().url(csvUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                handleFailure("couldn't load sheet (http ${response.code})")
                return
            }

            val csvContent = response.body?.string() ?: ""
            parseCsvSheet(csvContent)
            sheetSyncedAt = System.currentTimeMillis()

            context?.let { saveData(it) }
            evaluateState()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load sheet data", e)
            handleFailure("couldn't reach google sheet. tap to retry.")
        }
    }

    private fun normalizeSheetUrl(inputUrl: String): String {
        val url = inputUrl.trim()
        return if (url.contains("docs.google.com/spreadsheets/d/")) {
            val sheetId = url.substringAfter("/d/").substringBefore("/")
            "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv"
        } else {
            url
        }
    }

    /**
     * Parses standard CSV line respecting quotes and escaped quotes.
     */
    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Sanitizes numerical string removing currency symbols, percentage signs, and commas.
     */
    fun parseNumeric(value: String?): Double = parseNumericOrNull(value) ?: 0.0

    /** Like [parseNumeric], but null for a blank cell or a sheet error such as #N/A. */
    fun parseNumericOrNull(value: String?): Double? {
        if (value.isNullOrBlank()) return null
        val cleaned = value
            .replace("RM", "", ignoreCase = true)
            .replace("$", "")
            .replace("%", "")
            .replace(",", "")
            .replace("+", "")
            .trim()
        return cleaned.toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
    }

    /**
     * Parses standard CSV exported from Google Sheets.
     * Expected columns or rows: symbol, price, change, changePercent, name
     */
    fun parseCsvSheet(csvText: String) {
        if (csvText.isBlank()) return
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        val newWatchlist = mutableListOf<FinanceTicker>()
        // Determine header indices if first row has headers
        val header = lines.first().lowercase()
        val hasHeaders = header.contains("symbol") || header.contains("ticker") || header.contains("price")

        val dataLines = if (hasHeaders) lines.drop(1) else lines

        for (line in dataLines) {
            val cols = parseCsvLine(line)
            if (cols.isEmpty() || cols[0].isBlank()) continue

            val rawSymbol = cols[0]
            val symbol = rawSymbol.uppercase()
            val price = parseNumeric(cols.getOrNull(1))
            val change = parseNumericOrNull(cols.getOrNull(2))
            val changePercent = parseNumericOrNull(cols.getOrNull(3))
            val name = cols.getOrNull(4)?.ifBlank { null } ?: symbol

            val cleanTicker = symbol
                .removePrefix("KLSE:")
                .removeSuffix(".KL")
                .removePrefix("NASDAQ:")
                .removePrefix("NYSE:")

            newWatchlist.add(
                FinanceTicker(
                    symbol = symbol,
                    tickerDisplay = cleanTicker,
                    name = name,
                    price = price,
                    change = change,
                    changePercent = changePercent,
                    currency = "MYR",
                    quoteUrl = googleQuoteUrl(symbol)
                )
            )

            // Also update any matching local holdings with current live price from sheet
            for (i in holdingsList.indices) {
                val h = holdingsList[i]
                if (h.symbol.equals(symbol, ignoreCase = true) ||
                    h.symbol.equals(cleanTicker, ignoreCase = true) ||
                    h.symbol.equals(rawSymbol, ignoreCase = true)
                ) {
                    if (price > 0.0) {
                        holdingsList[i] = h.copy(currentPrice = price)
                    }
                }
            }
        }

        if (newWatchlist.isNotEmpty()) {
            watchlistItems.clear()
            watchlistItems.addAll(newWatchlist)
        }
    }

    fun openQuote(context: Context, ticker: FinanceTicker) {
        openQuoteUrl(context, ticker.quoteUrl)
    }

    fun openQuoteUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun evaluateState() {
        if (holdingsList.isEmpty() && watchlistItems.isEmpty() && sheetUrlConfigured.isBlank()) {
            _state.value = WidgetState.NeedsSetup
        } else {
            _state.value = WidgetState.Ready(data = buildData(), updatedAt = System.currentTimeMillis())
        }
    }

    private fun buildData(): FinanceData {
        val groups = holdingsList.groupBy { it.groupName }
        val accounts = groups.map { (groupName, list) ->
            val totals = portfolioTotals(list)
            PortfolioAccount(
                name = groupName,
                value = totals.value,
                change = totals.change ?: 0.0,
                changePercent = totals.changePercent ?: 0.0,
                currency = "MYR"
            )
        }
        val coins = coinPricing?.let { c ->
            c.copy(holdingIds = c.holdingIds.filterTo(mutableSetOf()) { id -> holdingsList.any { it.id == id } })
        }?.takeIf { it.holdingIds.isNotEmpty() }
        return FinanceData(
            holdings = holdingsList.toList(),
            portfolios = accounts,
            watchlist = watchlistItems.toList(),
            sheetConnected = sheetUrlConfigured.isNotBlank(),
            sheetUrl = sheetUrlConfigured,
            sheetSyncedAt = sheetSyncedAt,
            coins = coins
        )
    }

    private fun handleFailure(message: String) {
        val current = _state.value
        if (current is WidgetState.Stale) {
            Log.d(TAG, "Retaining stale finance data on error")
        } else {
            _state.value = WidgetState.Error(message)
        }
    }

    private fun loadSavedData(context: Context) {
        isStorageLocked = false
        val store = SharedPrefsStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
        val cipher = KeystoreSecretsManager.fieldCipher

        val sheet = SecureFields.read(store, cipher, KEY_SHEET_URL_ENC, KEY_SHEET_URL)
        val holdings = SecureFields.read(store, cipher, KEY_HOLDINGS_ENC, KEY_HOLDINGS_JSON)
        if (sheet !is SecureRead.Value || holdings !is SecureRead.Value) {
            lockStorage("saved portfolio can't be decrypted")
            return
        }

        try {
            val loaded = mutableListOf<FinanceHolding>()
            val holdingsStr = holdings.text
            if (!holdingsStr.isNullOrBlank()) {
                val arr = JSONArray(holdingsStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    loaded.add(
                        FinanceHolding(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            symbol = obj.optString("symbol"),
                            name = obj.optString("name"),
                            shares = obj.optDouble("shares", 0.0),
                            averageCost = obj.optDouble("averageCost", 0.0),
                            currentPrice = obj.optDouble("currentPrice", 0.0),
                            currency = obj.optString("currency", "MYR"),
                            groupName = obj.optString("groupName", "portfolio")
                        )
                    )
                }
            }
            holdingsList.clear()
            holdingsList.addAll(loaded)
            sheetUrlConfigured = sheet.text.orEmpty()
        } catch (e: Exception) {
            // JSONException messages quote the input, so log the type only (hard rule 6).
            lockStorage("saved portfolio can't be parsed: ${e.javaClass.simpleName}")
        }
    }

    private fun lockStorage(reason: String) {
        Log.w(TAG, reason)
        isStorageLocked = true
        holdingsList.clear()
        watchlistItems.clear()
        sheetUrlConfigured = ""
        _state.value = WidgetState.Error(UNLOCK_FAILED_MESSAGE)
    }

    private fun saveData(context: Context) {
        if (isStorageLocked) return
        try {
            val arr = JSONArray()
            for (h in holdingsList) {
                val obj = JSONObject().apply {
                    put("id", h.id)
                    put("symbol", h.symbol)
                    put("name", h.name)
                    put("shares", h.shares)
                    put("averageCost", h.averageCost)
                    put("currentPrice", h.currentPrice)
                    put("currency", h.currency)
                    put("groupName", h.groupName)
                }
                arr.put(obj)
            }

            val store = SharedPrefsStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
            val cipher = KeystoreSecretsManager.fieldCipher
            val savedHoldings = SecureFields.write(store, cipher, KEY_HOLDINGS_ENC, KEY_HOLDINGS_JSON, arr.toString())
            val savedSheet = SecureFields.write(store, cipher, KEY_SHEET_URL_ENC, KEY_SHEET_URL, sheetUrlConfigured)
            if (!savedHoldings || !savedSheet) Log.w(TAG, "Couldn't save finance data")
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't save finance data: ${e.javaClass.simpleName}")
        }
    }
}
