package my.hana.meena.data.finance

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState

/**
 * Data model for a financial security / stock in Google Finance Watchlist.
 */
data class FinanceTicker(
    val symbol: String,             // Yahoo symbol: e.g. "^KLSE", "6888.KL", "1818.KL", "5306.KL", "HLAL", "5305.KL"
    val tickerDisplay: String,      // Display ticker: "KLCI", "AXIATA", "BURSA", "FFB", "HLAL", "SENHENG"
    val name: String,               // "FTSE Bursa Malaysia KLCI", "Axiata Group Bhd", "Bursa Malaysia Bhd", etc.
    val exchange: String,           // "KLSE", "NASDAQ"
    val currency: String,           // "MYR" or "USD"
    val price: Double,              // Current market price
    val change: Double,             // Net change in price
    val changePercent: Double,      // Change percentage: e.g. +1.85
    val dayHigh: Double,
    val dayLow: Double,
    val sparkline: List<Double>,    // Intraday sparkline points
    val googleFinanceUrl: String    // Web link to Google Finance page
)

/**
 * Data model for user's Google Finance portfolio accounts (e.g. Mplus+, Luno).
 */
data class PortfolioAccount(
    val name: String,               // "Mplus+", "Luno"
    val value: Double,              // 197.00, 178.51
    val change: Double,             // +1.00, +2.26
    val changePercent: Double,      // +0.51, +1.28
    val currency: String = "MYR"
)

/**
 * Full OHLC Candle model for the Candlestick Chart.
 */
data class FinanceCandle(
    val timeLabel: String,          // e.g. "09:00", "11:00", "Mon", "28 Sep"
    val dateLabel: String,          // e.g. "02 Oct 2026", "28 Sep 2026"
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long = 0L,
    val timestamp: Long = 0L
)

/**
 * Data wrapper for all finance state.
 */
data class FinanceData(
    val portfolios: List<PortfolioAccount> = emptyList(),
    val watchlist: List<FinanceTicker> = emptyList()
)

/**
 * Repository integrating live Google Finance and Yahoo market data.
 */
object GoogleFinanceRepository : WidgetSpec<FinanceData> {

    override val id = "widget-google-finance"
    override val title = "google finance"
    override val source = "Yahoo Finance APIs"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 15

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Baseline User Portfolios matching Google Finance screenshot
    private val defaultPortfolios = emptyList<PortfolioAccount>()

    // Baseline Watchlist matching user's exact Google Finance list:
    private val defaultWatchlist = emptyList<FinanceTicker>()

    private val _financeState = MutableStateFlow<WidgetState<FinanceData>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<FinanceData>> = _financeState.asStateFlow()

    private val _candlesCache = mutableMapOf<String, List<FinanceCandle>>()

    init {
        // Evaluate initial state
        updateState()
        // Trigger background live refresh
        refreshLiveData()
    }
    
    override fun fetchInitialState(): WidgetState<FinanceData> {
        return _financeState.value
    }
    
    override suspend fun refresh(): WidgetState<FinanceData> {
        refreshLiveData()
        return _financeState.value
    }

    private fun updateState() {
        val currentWatchlist = defaultWatchlist // Will be replaced by settings/db later
        val currentPortfolios = defaultPortfolios
        
        if (currentWatchlist.isEmpty() && currentPortfolios.isEmpty()) {
            _financeState.value = WidgetState.NeedsSetup
        } else {
            _financeState.value = WidgetState.Ready(
                data = FinanceData(portfolios = currentPortfolios, watchlist = currentWatchlist),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun getTotalPortfolioValue(): Double {
        val data = (_financeState.value as? WidgetState.Ready)?.data
        return data?.portfolios?.sumOf { it.value } ?: 0.0
    }

    fun getTicker(query: String): FinanceTicker? {
        val data = (_financeState.value as? WidgetState.Ready)?.data
        return data?.watchlist?.find {
            it.symbol.equals(query, ignoreCase = true) ||
            it.tickerDisplay.equals(query, ignoreCase = true)
        }
    }

    fun getTotalPortfolioChange(): Double {
        val data = (_financeState.value as? WidgetState.Ready)?.data
        return data?.portfolios?.sumOf { it.change } ?: 0.0
    }

    fun getTotalPortfolioChangePercent(): Double {
        val total = getTotalPortfolioValue()
        val change = getTotalPortfolioChange()
        return if (total > 0) (change / (total - change)) * 100.0 else 0.0
    }

    /**
     * Launch Google Finance in browser or Google app.
     */
    fun openGoogleFinance(context: Context, url: String = "https://www.google.com/finance") {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Get or fetch Candlestick data for a specific symbol & timeframe.
     */
    fun getCandles(symbol: String, timeframe: String): List<FinanceCandle> {
        val cacheKey = "$symbol-$timeframe"
        val cached = _candlesCache[cacheKey]
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }

        fetchCandlesOnline(symbol, timeframe)
        return emptyList()
    }

    /**
     * Fetch live quotes in the background from Yahoo/Google Finance query endpoints.
     */
    fun refreshLiveData() {
        repositoryScope.launch {
            val currentState = _financeState.value
            val currentData = (currentState as? WidgetState.Ready)?.data ?: FinanceData(defaultPortfolios, defaultWatchlist)
            
            if (currentData.watchlist.isEmpty()) {
                _financeState.value = WidgetState.NeedsSetup
                return@launch
            }
            
            try {
                val updatedList = currentData.watchlist.map { ticker ->
                    try {
                        val urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/${ticker.symbol}?interval=15m&range=1d"
                        val url = URL(urlStr)
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                            connectTimeout = 6000
                            readTimeout = 6000
                        }

                        if (conn.responseCode == 200) {
                            val reader = BufferedReader(InputStreamReader(conn.inputStream))
                            val response = reader.readText()
                            reader.close()

                            val root = JSONObject(response)
                            val chart = root.getJSONObject("chart")
                            val resultArr = chart.getJSONArray("result")
                            if (resultArr.length() > 0) {
                                val res = resultArr.getJSONObject(0)
                                val meta = res.getJSONObject("meta")
                                val price = meta.optDouble("regularMarketPrice", ticker.price)
                                val changePercent = meta.optDouble("regularMarketChangePercent", ticker.changePercent)
                                val dayHigh = meta.optDouble("regularMarketDayHigh", ticker.dayHigh)
                                val dayLow = meta.optDouble("regularMarketDayLow", ticker.dayLow)
                                val prevClose = meta.optDouble("chartPreviousClose", price)
                                val change = price - prevClose

                                // Extract latest closes for sparkline
                                val quotes = res.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
                                val closes = quotes.getJSONArray("close")
                                val sparkPoints = mutableListOf<Double>()
                                for (k in 0 until closes.length()) {
                                    if (!closes.isNull(k)) {
                                        sparkPoints.add(closes.getDouble(k))
                                    }
                                }
                                val finalSpark = if (sparkPoints.size >= 5) sparkPoints.takeLast(10) else ticker.sparkline

                                ticker.copy(
                                    price = price,
                                    change = change,
                                    changePercent = changePercent,
                                    dayHigh = dayHigh,
                                    dayLow = dayLow,
                                    sparkline = finalSpark
                                )
                            } else ticker
                        } else ticker
                    } catch (_: Exception) {
                        ticker
                    }
                }
                
                _financeState.value = WidgetState.Ready(
                    data = FinanceData(portfolios = currentData.portfolios, watchlist = updatedList),
                    updatedAt = System.currentTimeMillis()
                )
            } catch (_: Exception) {
                _financeState.value = WidgetState.Error("Failed to fetch live data")
            }
        }
    }

    private fun fetchCandlesOnline(symbol: String, timeframe: String) {
        repositoryScope.launch {
            try {
                val (interval, range) = when (timeframe) {
                    "1H" -> Pair("5m", "1d")
                    "1D" -> Pair("15m", "1d")
                    "1W" -> Pair("1d", "5d")
                    "1M" -> Pair("1d", "1mo")
                    else -> Pair("15m", "1d")
                }

                val urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=$interval&range=$range"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    connectTimeout = 6000
                    readTimeout = 6000
                }

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val root = JSONObject(response)
                    val resultArr = root.getJSONObject("chart").getJSONArray("result")
                    if (resultArr.length() > 0) {
                        val res = resultArr.getJSONObject(0)
                        val timestamps = res.getJSONArray("timestamp")
                        val quotes = res.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
                        val opens = quotes.getJSONArray("open")
                        val highs = quotes.getJSONArray("high")
                        val lows = quotes.getJSONArray("low")
                        val closes = quotes.getJSONArray("close")

                        val parsedCandles = mutableListOf<FinanceCandle>()
                        val timeFmt = when (timeframe) {
                            "1H", "1D" -> SimpleDateFormat("HH:mm", Locale.getDefault())
                            "1W" -> SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                            else -> SimpleDateFormat("dd MMM", Locale.getDefault())
                        }
                        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                        for (i in 0 until timestamps.length()) {
                            if (!opens.isNull(i) && !highs.isNull(i) && !lows.isNull(i) && !closes.isNull(i)) {
                                val tSec = timestamps.getLong(i)
                                val dateObj = Date(tSec * 1000)
                                parsedCandles.add(
                                    FinanceCandle(
                                        timeLabel = timeFmt.format(dateObj),
                                        dateLabel = dateFmt.format(dateObj),
                                        open = opens.getDouble(i),
                                        high = highs.getDouble(i),
                                        low = lows.getDouble(i),
                                        close = closes.getDouble(i),
                                        timestamp = tSec
                                    )
                                )
                            }
                        }

                        if (parsedCandles.isNotEmpty()) {
                            _candlesCache["$symbol-$timeframe"] = parsedCandles
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

}


