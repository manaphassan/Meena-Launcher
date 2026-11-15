package my.hana.meena

import my.hana.meena.data.finance.Markets
import my.hana.meena.data.finance.PricePoint
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.model.WidgetConfig
import my.hana.meena.theme.MeenaDarkPalette
import my.hana.meena.theme.MeenaLightPalette
import my.hana.meena.theme.MeenaPaperPalette
import my.hana.meena.theme.calculateContrastRatio
import my.hana.meena.theme.chartSeriesColours
import my.hana.meena.theme.pageColours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Markets on the money page and double tap to lock (15 November 2026). */
class MarketsAndLockTest {

    // A cut-down Frankfurter v2 reply (base MYR), out of order like a real multi-quote list.
    private val reply = """
        [{"date":"2026-11-14","base":"MYR","quote":"BTC","rate":0.0000035},
         {"date":"2026-11-13","base":"MYR","quote":"BTC","rate":0.0000040},
         {"date":"2026-11-13","base":"MYR","quote":"XAU","rate":0.0000625},
         {"date":"2026-11-14","base":"MYR","quote":"XAU","rate":0.0000500},
         {"date":"2026-11-14","base":"MYR","quote":"ZZZ","rate":0},
         {"date":"2026-11-14","base":"MYR","quote":"ETH"}]
    """.trimIndent()

    @Test
    fun parse_turnsRatesIntoRinggitPrices_oldestFirst() {
        val series = Markets.parse(reply)
        assertEquals(listOf("2026-11-13", "2026-11-14"), series.getValue("XAU").map { it.date })
        assertEquals(16000.0, series.getValue("XAU")[0].price, 0.01)
        assertEquals(20000.0, series.getValue("XAU")[1].price, 0.01)
        // A zero rate or a missing rate is skipped, never shown as a price.
        assertFalse(series.containsKey("ZZZ"))
        assertFalse(series.containsKey("ETH"))
    }

    @Test
    fun parse_badReply_givesNothing() {
        assertTrue(Markets.parse("not json").isEmpty())
        assertTrue(Markets.parse("{\"message\":\"rate limited\"}").isEmpty())
    }

    @Test
    fun quotes_followAssetOrder_withDayChange() {
        val quotes = Markets.toQuotes(Markets.parse(reply))
        assertEquals(listOf("XAU", "BTC"), quotes.map { it.code })
        val gold = quotes.first()
        assertEquals("2026-11-14", gold.date)
        assertEquals(25.0, gold.dayChangePercent!!, 0.001)
    }

    @Test
    fun quotes_onePriceOnly_hasNoChange() {
        val quotes = Markets.toQuotes(mapOf("XAU" to listOf(PricePoint("2026-11-14", 100.0))))
        assertNull(quotes.single().dayChangePercent)
    }

    @Test
    fun downsample_keepsFirstAndLast_andStaysUnderTheLimit() {
        val points = (0 until 365).map { PricePoint("d$it", it.toDouble()) }
        val thin = Markets.downsample(points, 60)
        assertTrue(thin.size <= 61)
        assertEquals(points.first(), thin.first())
        assertEquals(points.last(), thin.last())
        assertEquals(points.take(10), Markets.downsample(points.take(10), 60))
    }

    @Test
    fun relativeChange_startsAtZero() {
        val change = Markets.relativeChange(listOf(PricePoint("a", 200.0), PricePoint("b", 250.0), PricePoint("c", 150.0)))
        assertEquals(listOf(0.0, 25.0, -25.0), change)
    }

    @Test
    fun prices_andLabels_readNaturally() {
        assertEquals("16,018", Markets.formatPrice(16017.7))
        assertEquals("207.48", Markets.formatPrice(207.483))
        assertEquals("nov 2025", Markets.monthLabel("2025-11-12"))
        assertEquals("", Markets.monthLabel("soon"))
    }

    @Test
    fun dayNumber_matchesTheCalendar() {
        assertEquals(0L, Markets.dayNumber("1970-01-01"))
        assertEquals(19723L, Markets.dayNumber("2024-01-01"))
        assertEquals(366L, Markets.dayNumber("2024-12-31")!! - Markets.dayNumber("2023-12-31")!!)
        assertNull(Markets.dayNumber("2024-13-01"))
    }

    @Test
    fun cache_roundTrips() {
        val quotes = Markets.toQuotes(Markets.parse(reply))
        assertEquals(quotes, Markets.decode(Markets.encode(quotes)))
        assertTrue(Markets.decode("broken").isEmpty())
    }

    @Test
    fun url_asksForRinggitPricesOfEveryAsset() {
        val url = Markets.url("2025-11-15")
        assertTrue(url.startsWith("https://api.frankfurter.dev/v2/rates?"))
        assertTrue(url.contains("from=2025-11-15"))
        assertTrue(url.contains("base=myr"))
        assertTrue(url.contains("quotes=xau,btc,eth,xag"))
    }

    @Test
    fun chartColours_readOnEveryPage() {
        for (palette in listOf(MeenaDarkPalette, MeenaLightPalette, MeenaPaperPalette)) {
            val colours = chartSeriesColours(palette)
            assertTrue(colours.size >= Markets.MAX_MARKET_TICKERS)
            for (colour in colours) for (page in pageColours(palette)) {
                assertTrue("${palette.id} colour too faint", calculateContrastRatio(colour, page) >= 4.5f)
            }
        }
    }

    // Money page layout

    @Test
    fun migrate_putsMarketsFirstOnTheMoneyPage_once() {
        val old = MeenaUserSettings(
            layoutRevision = MeenaUserSettings.EMAIL_SECTION_REVISION,
            widgetConfigs = MeenaUserSettings.defaultWidgetMap() - "widget-markets" - "widget-kijang-emas" + mapOf(
                "widget-finance-charts" to WidgetConfig("money", 0),
                "widget-exchange-rates" to WidgetConfig("money", 1),
                "widget-spending-summary" to WidgetConfig("money", 2)
            )
        )
        val migrated = old.migrate()
        val money = migrated.widgetConfigs.filterValues { it.hubId == "money" }.entries
            .sortedBy { it.value.position }.map { it.key }
        assertEquals(listOf("widget-markets", "widget-kijang-emas", "widget-finance-charts", "widget-exchange-rates", "widget-spending-summary"), money)
        assertEquals(MeenaUserSettings.KIJANG_EMAS_REVISION, migrated.layoutRevision)

        // Afterwards the user's own order is kept.
        val moved = migrated.copy(widgetConfigs = migrated.widgetConfigs + ("widget-markets" to WidgetConfig("money", 9)))
        assertEquals(9, moved.migrate().widgetConfigs.getValue("widget-markets").position)
    }

    // Kijang Emas (Bank Negara Malaysia)

    // Trimmed from https://api.bnm.gov.my/public/kijang-emas/year/2026/month/11 (15 Nov 2026), out of order.
    private val bnmMonth = """
        {"data":[{"effective_date":"2026-11-12","one_oz":{"buying":16544,"selling":17241},"half_oz":{"buying":8272,"selling":8783},"quarter_oz":{"buying":4136,"selling":4473}},
                 {"effective_date":"2026-11-11","one_oz":{"buying":16707,"selling":17415},"half_oz":{"buying":8353,"selling":8872},"quarter_oz":{"buying":4177,"selling":4518}}],
         "meta":{"last_updated":"2026-11-12 02:00:47","total_result":2}}
    """.trimIndent()

    @Test
    fun kijangEmas_sellingIsWhatYouPay_andChangeIsFromTheDayBefore() {
        val days = my.hana.meena.data.finance.KijangEmas.parseDays(bnmMonth)
        assertEquals(listOf("2026-11-11", "2026-11-12"), days.map { it.date })
        val summary = my.hana.meena.data.finance.KijangEmas.summarise(days)!!
        assertEquals("2026-11-12", summary.date)
        val oneOz = summary.prices.first()
        assertEquals("one_oz", oneOz.size)
        assertEquals(17241.0, oneOz.youPay, 0.0)
        assertEquals(16544.0, oneOz.youGet, 0.0)
        assertEquals(listOf("one_oz", "half_oz", "quarter_oz"), summary.prices.map { it.size })
        assertEquals("2026-11-11", summary.previousDate)
        assertEquals((17241.0 / 17415.0 - 1) * 100, summary.changePercent!!, 1e-9)
    }

    @Test
    fun kijangEmas_oneDayReply_andBadReplies() {
        val one = """{"data":{"effective_date":"2026-11-12","one_oz":{"buying":16544,"selling":17241}},"meta":{}}"""
        val summary = my.hana.meena.data.finance.KijangEmas.summarise(my.hana.meena.data.finance.KijangEmas.parseDays(one))!!
        assertNull(summary.changePercent)
        assertNull(summary.previousDate)
        assertTrue(my.hana.meena.data.finance.KijangEmas.parseDays("""{"message":"No records found.","code":404}""").isEmpty())
        assertTrue(my.hana.meena.data.finance.KijangEmas.parseDays("<html>").isEmpty())
        assertNull(my.hana.meena.data.finance.KijangEmas.summarise(emptyList()))
    }

    @Test
    fun kijangEmas_cacheRoundTrips_andDatesReadNaturally() {
        val days = my.hana.meena.data.finance.KijangEmas.parseDays(bnmMonth)
        assertEquals(days, my.hana.meena.data.finance.KijangEmas.decode(my.hana.meena.data.finance.KijangEmas.encode(days)))
        assertEquals("12 nov", my.hana.meena.data.finance.KijangEmas.dayLabel("2026-11-12"))
        assertEquals("https://api.bnm.gov.my/public/kijang-emas/year/2026/month/11", my.hana.meena.data.finance.KijangEmas.monthUrl(2026, 11))
    }

    // Add ticker and the rates table

    @Test
    fun catalogue_readsFrankfurtersList_withoutRinggit() {
        val body = """[{"iso_code":"SOL","name":"Solana","type":"crypto"},
            {"iso_code":"MYR","name":"Malaysian Ringgit","type":"currency"},
            {"iso_code":"XPT","name":"Platinum","type":"metal"},
            {"iso_code":"XAU","name":"Gold (Troy Ounce)","type":"metal"},
            {"name":"no code"}]"""
        val list = Markets.parseCatalogue(body)
        assertEquals(listOf("SOL", "XAU", "XPT"), list.map { it.code })
        assertEquals("gold", Markets.displayName(list[1]))
        assertEquals("solana", Markets.displayName(list[0]))
        assertTrue(Markets.parseCatalogue("oops").isEmpty())
    }

    @Test
    fun addTicker_onceAndWithinTheLimit() {
        assertEquals(listOf("BTC", "SOL"), Markets.addTicker(listOf("BTC"), "sol", 8))
        assertNull(Markets.addTicker(listOf("BTC"), "BTC", 8))
        assertNull(Markets.addTicker(listOf("A", "B"), "C", 2))
    }

    @Test
    fun unitFor_keepsEveryRateAtLeastOneRinggit() {
        assertEquals(1, Markets.unitFor(4.09))
        assertEquals(1, Markets.unitFor(1.0))
        assertEquals(10, Markets.unitFor(0.12))
        assertEquals(100, Markets.unitFor(0.0255))
        assertEquals(10_000, Markets.unitFor(0.00026))
    }

    @Test
    fun rateItems_followTheChosenOrder_withChangeSinceTheDayBefore() {
        val series = Markets.parse(
            """[{"date":"2026-11-13","base":"MYR","quote":"USD","rate":0.25},
                {"date":"2026-11-14","base":"MYR","quote":"USD","rate":0.20},
                {"date":"2026-11-14","base":"MYR","quote":"JPY","rate":40}]"""
        )
        val items = Markets.rateItems(series, listOf("JPY", "USD", "XXX"))
        assertEquals(listOf("JPY", "USD"), items.map { it.currency })
        assertEquals(5.0, items[1].rateToBase, 1e-9)
        assertEquals(25.0, items[1].changePercent!!, 1e-9)
        assertNull(items[0].changePercent)
    }

    @Test
    fun tinyPrices_keepThreeFigures() {
        assertEquals("0.0000523", Markets.formatPrice(0.00005234))
        assertEquals("0.712", Markets.formatPrice(0.7123))
    }

    @Test
    fun tickers_defaultToTheBuiltInLists() {
        assertEquals(Markets.DEFAULT_TICKERS, MeenaUserSettings().marketTickers)
        assertEquals(Markets.DEFAULT_RATES, MeenaUserSettings().rateCurrencies)
    }

    // Double tap to lock

    @Test
    fun doubleTapToLock_isOffUntilTurnedOn() {
        assertFalse(MeenaUserSettings().doubleTapToLock)
        assertFalse(MeenaUserSettings().migrate().doubleTapToLock)
    }
}
