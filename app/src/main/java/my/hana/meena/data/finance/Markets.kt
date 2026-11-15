package my.hana.meena.data.finance

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

/** One daily price in ringgit. [date] is the source's own "yyyy-MM-dd". */
data class PricePoint(val date: String, val price: Double)

/** The latest price of one asset, its change since the previous day, and a thinned year of history. */
data class MarketQuote(
    /** Frankfurter code: XAU (gold), BTC, ETH, XAG (silver). */
    val code: String,
    /** Ringgit for one troy ounce (gold, silver) or one coin. */
    val price: Double,
    val date: String,
    /** Percent change from the previous published price; null with only one price. */
    val dayChangePercent: Double?,
    /** Oldest first, always ending with the latest price. */
    val history: List<PricePoint>
)

data class MarketsData(val quotes: List<MarketQuote>)

/** One entry in Frankfurter's list of everything it prices. [type] is "currency", "crypto" or "metal". */
data class Ticker(val code: String, val name: String, val type: String)

/** Pure helpers behind [MarketsRepository], kept apart so they can be unit tested. */
object Markets {
    /** What the markets widget follows until the person picks their own. */
    val DEFAULT_TICKERS = listOf("XAU", "BTC", "ETH", "XAG")
    /** What the rates table shows until the person picks their own. */
    val DEFAULT_RATES = listOf("USD", "SGD", "EUR", "GBP", "JPY", "CNY", "AUD", "THB", "IDR")
    const val MAX_MARKET_TICKERS = 8
    const val MAX_RATE_CURRENCIES = 16

    /** Kept for the chart colours: one colour per default ticker, in this order. */
    val ASSETS = DEFAULT_TICKERS

    /** About a point a week keeps the chart smooth and the cache small. */
    const val CHART_POINTS = 60

    private val json = Json { ignoreUnknownKeys = true }

    const val CATALOGUE_URL = "https://api.frankfurter.dev/v2/currencies"

    fun url(fromDate: String, codes: List<String> = DEFAULT_TICKERS): String =
        "https://api.frankfurter.dev/v2/rates?from=$fromDate&base=myr&quotes=" +
            codes.joinToString(",") { it.lowercase(Locale.ROOT) }

    /** Frankfurter's list ([{"iso_code","name","type"}]) without ringgit itself, sorted by code. */
    fun parseCatalogue(body: String): List<Ticker> {
        val rows = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull() ?: return emptyList()
        return rows.mapNotNull { row ->
            val obj = row as? JsonObject ?: return@mapNotNull null
            val code = obj["iso_code"]?.jsonPrimitive?.contentOrNull?.uppercase(Locale.ROOT) ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: code
            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "currency"
            if (code == "MYR") null else Ticker(code, name, type)
        }.distinctBy { it.code }.sortedBy { it.code }
    }

    /** "gold" from "Gold (Troy Ounce)", "solana" from "Solana": the plain name, lowercase. */
    fun displayName(ticker: Ticker): String =
        ticker.name.substringBefore(" (").trim().ifEmpty { ticker.code }.lowercase(Locale.ROOT)

    /** Adds [code] once, at the end, up to [max]; null when the list is full or it's already there. */
    fun addTicker(list: List<String>, code: String, max: Int): List<String>? {
        val clean = code.uppercase(Locale.ROOT)
        if (clean in list || list.size >= max) return null
        return list + clean
    }

    /**
     * How many units to quote so the ringgit price is at least 1: 1 usd, 100 jpy, 10,000 idr.
     * Keeps every row readable at two decimals.
     */
    fun unitFor(price: Double): Int {
        var unit = 1
        while (price * unit < 1.0 && unit < 1_000_000) unit *= 10
        return unit
    }

    /** Ringgit price and change since the previous published day, for each of [codes] that came back. */
    fun rateItems(series: Map<String, List<PricePoint>>, codes: List<String>): List<ExchangeRateItem> =
        codes.mapNotNull { code ->
            val points = series[code.uppercase(Locale.ROOT)].orEmpty()
            val last = points.lastOrNull() ?: return@mapNotNull null
            val previous = points.getOrNull(points.size - 2)
            ExchangeRateItem(
                currency = code.uppercase(Locale.ROOT),
                rateToBase = last.price,
                baseToCurrency = 1.0 / last.price,
                changePercent = previous?.let { (last.price / it.price - 1.0) * 100.0 }
            )
        }

    fun oneYearAgo(nowMillis: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.YEAR, -1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    /**
     * Reads Frankfurter's v2 list ([{"date","base","quote","rate"}], base MYR) into ringgit prices
     * per asset, oldest first. A rate is how much of the asset one ringgit buys, so the price is 1 / rate.
     */
    fun parse(body: String): Map<String, List<PricePoint>> {
        val rows = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull() ?: return emptyMap()
        val series = mutableMapOf<String, MutableList<PricePoint>>()
        for (row in rows) {
            val obj = row as? JsonObject ?: continue
            val quote = obj["quote"]?.jsonPrimitive?.contentOrNull?.uppercase(Locale.ROOT) ?: continue
            val date = obj["date"]?.jsonPrimitive?.contentOrNull ?: continue
            val rate = obj["rate"]?.jsonPrimitive?.doubleOrNull ?: continue
            if (rate <= 0.0 || rate.isNaN()) continue
            series.getOrPut(quote) { mutableListOf() }.add(PricePoint(date, 1.0 / rate))
        }
        return series.mapValues { (_, points) -> points.sortedBy { it.date }.distinctBy { it.date } }
    }

    fun toQuotes(
        series: Map<String, List<PricePoint>>,
        codes: List<String> = DEFAULT_TICKERS,
        maxPoints: Int = CHART_POINTS
    ): List<MarketQuote> =
        codes.mapNotNull { code ->
            val points = series[code].orEmpty()
            val last = points.lastOrNull() ?: return@mapNotNull null
            val previous = points.getOrNull(points.size - 2)
            MarketQuote(
                code = code,
                price = last.price,
                date = last.date,
                dayChangePercent = previous?.let { (last.price / it.price - 1.0) * 100.0 },
                history = downsample(points, maxPoints)
            )
        }

    /** Evenly thins [points] to at most [maxPoints], always keeping the first and the last. */
    fun downsample(points: List<PricePoint>, maxPoints: Int): List<PricePoint> {
        if (points.size <= maxPoints || maxPoints < 2) return points
        val step = ceil((points.size - 1).toDouble() / (maxPoints - 1)).toInt()
        val picked = points.indices.filter { it % step == 0 }.map { points[it] }.toMutableList()
        if (picked.last() != points.last()) picked.add(points.last())
        return picked
    }

    /** Percent change of each point from the first one, for drawing several assets on one scale. */
    fun relativeChange(history: List<PricePoint>): List<Double> {
        val first = history.firstOrNull()?.price ?: return emptyList()
        if (first <= 0.0) return emptyList()
        return history.map { (it.price / first - 1.0) * 100.0 }
    }

    /** Days since 1970-01-01 for a "yyyy-MM-dd" date (java.time needs API 26), or null. */
    fun dayNumber(date: String): Long? {
        val parts = date.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        val d = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return null
        // Howard Hinnant's days_from_civil.
        val yy = if (m <= 2) y - 1 else y
        val era = (if (yy >= 0) yy else yy - 399) / 400
        val yoe = yy - era * 400
        val mp = (m + 9) % 12
        val doy = (153 * mp + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }

    /** "16,018" for big prices, "207.48" for small ones, three significant figures below 1 ("0.0000523"). */
    fun formatPrice(price: Double): String = when {
        price >= 1000 -> String.format(Locale.US, "%,.0f", price)
        price >= 1 || price <= 0.0 -> String.format(Locale.US, "%,.2f", price)
        else -> java.math.BigDecimal(price).round(java.math.MathContext(3)).stripTrailingZeros().toPlainString()
    }

    /** "nov 2025" from "2025-11-12"; empty if the date can't be read. */
    fun monthLabel(date: String): String {
        val parts = date.split("-")
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return ""
        val year = parts.getOrNull(0) ?: return ""
        val name = MONTHS.getOrNull(month - 1) ?: return ""
        return "$name $year"
    }

    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    fun encode(quotes: List<MarketQuote>): String = buildJsonArray {
        quotes.forEach { quote ->
            add(buildJsonObject {
                put("code", quote.code)
                put("price", quote.price)
                put("date", quote.date)
                put("change", quote.dayChangePercent?.let { JsonPrimitive(it) } ?: JsonNull)
                put("history", buildJsonArray {
                    quote.history.forEach { point ->
                        add(buildJsonArray {
                            add(JsonPrimitive(point.date))
                            add(JsonPrimitive(point.price))
                        })
                    }
                })
            })
        }
    }.toString()

    fun decode(text: String): List<MarketQuote> = runCatching {
        json.parseToJsonElement(text).jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val history = (obj["history"] as? JsonArray).orEmpty().mapNotNull { pair ->
                val values = pair as? JsonArray ?: return@mapNotNull null
                val date = values.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val price = values.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                PricePoint(date, price)
            }
            MarketQuote(
                code = obj["code"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                price = obj["price"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                date = obj["date"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                dayChangePercent = (obj["change"] as? JsonPrimitive)?.doubleOrNull,
                history = history
            )
        }
    }.getOrDefault(emptyList())
}
