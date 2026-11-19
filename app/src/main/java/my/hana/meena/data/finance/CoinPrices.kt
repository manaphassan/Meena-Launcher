package my.hana.meena.data.finance

import java.util.Locale

/** How the coin (and metal) holdings got their prices this session. */
data class CoinPricing(
    /** Holdings priced from Frankfurter, by holding id. */
    val holdingIds: Set<String>,
    /** Frankfurter's date for the oldest coin price shown ("2026-11-18"); null until a load works. */
    val date: String? = null,
    /** The last load got nothing back; any coin price shown is from before. */
    val failed: Boolean = false,
    /** Codes the last load had no price for; those holdings keep the price they had. */
    val missing: List<String> = emptyList()
)

/**
 * Coin holdings (XRP, BTC, BCH) priced in ringgit from Frankfurter's daily blended rates
 * (https://frankfurter.dev, keyless; its FAQ allows commercial use and caching). Chosen on
 * 19 Nov 2026 over the sheet, whose crypto prices were far off, and over the Luno API, whose
 * terms (§19) forbid redistributing its market data without written consent.
 */
object CoinPrices {
    /** Coins and metals only. Currencies are left out so a stock ticker never gets a currency's price. */
    private val TYPES = setOf("crypto", "metal")

    /** Luno calls bitcoin XBT; Frankfurter calls it BTC. */
    private val ALIASES = mapOf("XBT" to "BTC")

    /** Latest prices, base ringgit, e.g. https://api.frankfurter.dev/v2/rates?base=myr&quotes=xrp,btc */
    fun latestUrl(codes: Collection<String>): String =
        "https://api.frankfurter.dev/v2/rates?base=myr&quotes=" +
            codes.sorted().joinToString(",") { it.lowercase(Locale.ROOT) }

    fun priceableCodes(catalogue: List<Ticker>): Set<String> =
        catalogue.filter { it.type in TYPES }.map { it.code.uppercase(Locale.ROOT) }.toSet()

    /** The Frankfurter code for a holding's symbol, or null if Frankfurter doesn't price it as a coin or metal. */
    fun codeFor(symbol: String, priceable: Set<String>): String? {
        val clean = symbol.trim().uppercase(Locale.ROOT)
        val code = ALIASES[clean] ?: clean
        return code.takeIf { it in priceable }
    }

    /**
     * Holding id to Frankfurter code for every coin holding. A holding that also has a row in the
     * person's sheet is left to the sheet, so a stock that shares a coin's ticker keeps its own price.
     */
    fun coinHoldings(
        holdings: List<FinanceHolding>,
        priceable: Set<String>,
        sheetSymbols: Set<String>
    ): Map<String, String> =
        holdings.mapNotNull { h ->
            if (plainTicker(h.symbol) in sheetSymbols) return@mapNotNull null
            codeFor(h.symbol, priceable)?.let { h.id to it }
        }.toMap()

    /** The newest price per code from a parsed Frankfurter reply. */
    fun latest(series: Map<String, List<PricePoint>>): Map<String, PricePoint> =
        series.mapNotNull { (code, points) -> points.lastOrNull()?.let { code to it } }.toMap()

    /** [holding] with its coin price filled in. Without a fresh price it is left exactly as it was. */
    fun priced(
        holding: FinanceHolding,
        coins: Map<String, String>,
        latest: Map<String, PricePoint>
    ): FinanceHolding {
        val price = coins[holding.id]?.let { latest[it]?.price }
        return if (price != null && price > 0.0) holding.copy(currentPrice = price) else holding
    }

    /** What the money page says about the load: the oldest date shown and the codes that got no price. */
    fun summarise(coins: Map<String, String>, latest: Map<String, PricePoint>): CoinPricing {
        val codes = coins.values.toSet()
        val got = codes.filter { it in latest }
        return CoinPricing(
            holdingIds = coins.keys,
            date = got.mapNotNull { latest[it]?.date }.minOrNull(),
            failed = got.isEmpty(),
            missing = (codes - got.toSet()).sorted()
        )
    }
}
