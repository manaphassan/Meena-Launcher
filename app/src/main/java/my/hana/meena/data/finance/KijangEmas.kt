package my.hana.meena.data.finance

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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

/** One coin size on one day. Bank Negara's "selling" is what you pay; "buying" is what you get back. */
data class KijangEmasPrice(val size: String, val youPay: Double, val youGet: Double)

/** Prices for every coin size on one effective date ("yyyy-MM-dd"). */
data class KijangEmasDay(val date: String, val prices: List<KijangEmasPrice>)

/** The latest prices, and how the 1 oz price moved since the day before. */
data class KijangEmasData(
    val date: String,
    val prices: List<KijangEmasPrice>,
    val previousDate: String?,
    /** 1 oz "you pay" change since [previousDate], in percent; null without an earlier day. */
    val changePercent: Double?
)

/**
 * Pure helpers for Bank Negara Malaysia's Kijang Emas prices (the bullion coin, not spot gold).
 * Open API, no key: https://api.bnm.gov.my/public; the Accept header below is required.
 */
object KijangEmas {
    const val BASE = "https://api.bnm.gov.my/public/kijang-emas"
    const val ACCEPT = "application/vnd.BNM.API.v1+json"
    val SIZES = listOf("one_oz", "half_oz", "quarter_oz")

    private val json = Json { ignoreUnknownKeys = true }

    fun monthUrl(year: Int, month: Int): String = "$BASE/year/$year/month/$month"

    /** Reads {"data": [...]} (a month) or {"data": {...}} (one day); oldest first. */
    fun parseDays(body: String): List<KijangEmasDay> {
        val data = runCatching { json.parseToJsonElement(body).jsonObject["data"] }.getOrNull() ?: return emptyList()
        val rows = when (data) {
            is JsonArray -> data.toList()
            is JsonObject -> listOf(data)
            else -> emptyList()
        }
        return rows.mapNotNull { row ->
            val obj = row as? JsonObject ?: return@mapNotNull null
            val date = obj["effective_date"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val prices = SIZES.mapNotNull { size ->
                val coin = obj[size] as? JsonObject ?: return@mapNotNull null
                val pay = coin["selling"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val get = coin["buying"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                if (pay <= 0.0 || get <= 0.0) null else KijangEmasPrice(size, pay, get)
            }
            if (prices.isEmpty()) null else KijangEmasDay(date, prices)
        }.sortedBy { it.date }.distinctBy { it.date }
    }

    fun summarise(days: List<KijangEmasDay>): KijangEmasData? {
        val sorted = days.sortedBy { it.date }
        val latest = sorted.lastOrNull() ?: return null
        val previous = sorted.getOrNull(sorted.size - 2)
        val now = latest.prices.firstOrNull { it.size == "one_oz" }?.youPay
        val before = previous?.prices?.firstOrNull { it.size == "one_oz" }?.youPay
        return KijangEmasData(
            date = latest.date,
            prices = latest.prices,
            previousDate = previous?.date,
            changePercent = if (now != null && before != null && before > 0.0) (now / before - 1.0) * 100.0 else null
        )
    }

    /** "12 nov" from "2026-11-12"; empty if the date can't be read. */
    fun dayLabel(date: String): String {
        val parts = date.split("-")
        val day = parts.getOrNull(2)?.toIntOrNull() ?: return ""
        val month = Markets.monthLabel(date).substringBefore(" ")
        return if (month.isEmpty()) "" else "$day $month"
    }

    fun encode(days: List<KijangEmasDay>): String = buildJsonArray {
        days.forEach { day ->
            add(buildJsonObject {
                put("date", day.date)
                put("prices", buildJsonArray {
                    day.prices.forEach { price ->
                        add(buildJsonArray {
                            add(JsonPrimitive(price.size))
                            add(JsonPrimitive(price.youPay))
                            add(JsonPrimitive(price.youGet))
                        })
                    }
                })
            })
        }
    }.toString()

    fun decode(text: String): List<KijangEmasDay> = runCatching {
        json.parseToJsonElement(text).jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val date = obj["date"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val prices = (obj["prices"] as? JsonArray).orEmpty().mapNotNull { item ->
                val values = item as? JsonArray ?: return@mapNotNull null
                val size = values.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val pay = values.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val get = values.getOrNull(2)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                KijangEmasPrice(size, pay, get)
            }
            KijangEmasDay(date, prices)
        }
    }.getOrDefault(emptyList())
}
