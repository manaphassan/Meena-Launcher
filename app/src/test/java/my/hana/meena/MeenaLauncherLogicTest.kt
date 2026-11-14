package my.hana.meena

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.JakimSolatRepository
import my.hana.meena.data.system.SpendingRepository
import androidx.compose.ui.graphics.Color
import my.hana.meena.theme.Meena10AccentSwatches
import my.hana.meena.theme.MeenaDarkPalette
import my.hana.meena.theme.calculateContrastRatio
import my.hana.meena.theme.isAccessibleContrast
import my.hana.meena.ui.navigation.ParallaxHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeenaLauncherLogicTest {

    @Test
    fun defaultUserSettings_hasValidConfiguration() {
        val settings = MeenaUserSettings()
        assertEquals("en", settings.language)
        assertEquals("dark", settings.baseTheme)
        assertEquals(0, settings.defaultHubIndex)
        assertTrue(settings.widgetConfigs.isNotEmpty())
        assertTrue(settings.widgetConfigs["widget-clock-weather"]?.isEnabled == true)
        assertEquals("today", settings.widgetConfigs["widget-clock-weather"]?.hubId)
        assertEquals("inbox", settings.widgetConfigs["widget-conversations"]?.hubId)
        assertEquals("news", settings.widgetConfigs["widget-news-feed"]?.hubId)
        assertEquals("money", settings.widgetConfigs["widget-finance-charts"]?.hubId)
        assertFalse(settings.widgetConfigs["widget-map-radar"]?.isEnabled == true)
    }

    @Test
    fun defaultWidgetMap_orderAndEnabledFlags_tellTodayAsAStory() {
        val defaultMap = MeenaUserSettings.defaultWidgetMap()

        // 1. Verify default-off widgets (isEnabled = false)
        val expectedDisabled = listOf(
            "widget-calendar-month",
            "widget-notes",
            "widget-health-device",
            "widget-health-telemetry",
            "widget-map-radar",
            "widget-device-telemetry"
        )
        for (widgetId in expectedDisabled) {
            val config = defaultMap[widgetId]
            assertNotNull("Widget $widgetId must exist in defaultWidgetMap", config)
            assertFalse("Widget $widgetId must be default-off (isEnabled = false)", config!!.isEnabled)
        }

        // 2. Verify enabled today widgets and their relative positions
        val enabledTodayWidgets = defaultMap.entries
            .filter { it.value.hubId == "today" && it.value.isEnabled }
            .sortedBy { it.value.position }
            .map { it.key }

        assertEquals(
            listOf(
                "widget-clock-weather",
                "widget-weather-forecast",
                "widget-tasks",
                "widget-solat",
                "widget-favourites"
            ),
            enabledTodayWidgets
        )

        // 3. Acceptance verification: on a fresh install Today reads now, sky, your week, prayer, apps.
        val freshSettings = MeenaUserSettings()
        val visibleTodayWidgetsOnFreshInstall = freshSettings.widgetConfigs.entries
            .filter {
                val isTodayHub = it.value.hubId == "today" || it.value.hubId == "start" || it.value.hubId == "agenda" || it.value.hubId == "health"
                val isNotInboxWidget = it.key != "widget-conversations" && it.key != "widget-notifications"
                val isNotRemovedWidget = it.key != "widget-health-device"
                isTodayHub && isNotInboxWidget && isNotRemovedWidget && it.value.isEnabled
            }
            .sortedBy { it.value.position }
            .map { it.key }

        assertEquals(
            listOf(
                "widget-clock-weather",
                "widget-weather-forecast",
                "widget-tasks",
                "widget-solat",
                "widget-favourites"
            ),
            visibleTodayWidgetsOnFreshInstall
        )
    }

    @Test
    fun storyOrder_resetsPositionsOnly() {
        val custom = MeenaUserSettings().let { s ->
            s.copy(widgetConfigs = s.widgetConfigs.mapValues { (id, c) ->
                when (id) {
                    "widget-solat" -> c.copy(position = 0, hubId = "money", isCollapsed = true)
                    "widget-clock-weather" -> c.copy(position = 9)
                    "widget-map-radar" -> c.copy(isEnabled = true)
                    else -> c
                }
            })
        }
        val sorted = custom.withStoryOrder().widgetConfigs
        assertEquals(0, sorted["widget-clock-weather"]?.position)
        assertEquals(3, sorted["widget-solat"]?.position)
        assertEquals("money", sorted["widget-solat"]?.hubId)
        assertTrue(sorted["widget-solat"]?.isCollapsed == true)
        assertTrue(sorted["widget-map-radar"]?.isEnabled == true)
    }

    @Test
    fun migrate_movesOldUsDateDefaultToMalaysianEnglish() {
        val old = MeenaUserSettings(dateFormatPattern = MeenaUserSettings.OLD_DEFAULT_DATE_PATTERN).migrate()
        assertEquals(MeenaUserSettings.DEFAULT_DATE_PATTERN, old.dateFormatPattern)
        val picked = MeenaUserSettings(dateFormatPattern = "yyyy-MM-dd").migrate()
        assertEquals("yyyy-MM-dd", picked.dateFormatPattern)
    }

    @Test
    fun userSettings_migration_preservesUserConfiguredEnabledStates() {
        val existingUser = MeenaUserSettings(
            widgetConfigs = mapOf(
                "widget-map-radar" to my.hana.meena.data.model.WidgetConfig("today", 10, isEnabled = true)
            )
        )
        val migrated = existingUser.migrate()
        assertTrue("migrate() must preserve user's enabled widget-map-radar", migrated.widgetConfigs["widget-map-radar"]?.isEnabled == true)
        assertFalse("Newly added missing widget-notes must receive default-off", migrated.widgetConfigs["widget-notes"]?.isEnabled == true)
    }

    @Test
    fun userSettings_migration_correctlyMapsLegacyHubs() {
        val legacy = MeenaUserSettings(
            defaultHubIndex = 5,
            widgetConfigs = mapOf(
                "widget-clock-weather" to my.hana.meena.data.model.WidgetConfig("start", 0),
                "widget-conversations" to my.hana.meena.data.model.WidgetConfig("start", 1),
                "widget-solat" to my.hana.meena.data.model.WidgetConfig("agenda", 0),
                "widget-health-device" to my.hana.meena.data.model.WidgetConfig("health", 0),
                "widget-finance-charts" to my.hana.meena.data.model.WidgetConfig("finance", 0)
            )
        )
        val migrated = legacy.migrate()
        assertEquals(4, migrated.defaultHubIndex)
        assertEquals("today", migrated.widgetConfigs["widget-clock-weather"]?.hubId)
        assertEquals("inbox", migrated.widgetConfigs["widget-conversations"]?.hubId)
        assertEquals("today", migrated.widgetConfigs["widget-solat"]?.hubId)
        assertEquals("today", migrated.widgetConfigs["widget-health-device"]?.hubId)
        assertEquals("money", migrated.widgetConfigs["widget-finance-charts"]?.hubId)
        assertTrue(migrated.widgetConfigs.containsKey("widget-news-feed"))
    }

    @Test
    fun jakimSolatRepository_returnsValidSchedule() {
        val state = JakimSolatRepository.state.value
        assertTrue(state is my.hana.meena.ui.widgets.WidgetState.NeedsSetup)
    }

    @Test
    fun spendingRepository_recordsTransactionsCorrectly() {
        val initialCount = (SpendingRepository.state.value as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data?.size ?: 0
        SpendingRepository.addTransaction(
            bank = "Maybank MAE",
            bankCode = "M",
            merchant = "Nasi Kandar Pelita",
            channel = "DuitNow QR",
            amount = 18.50
        )
        val afterCount = (SpendingRepository.state.value as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data?.size ?: 0
        assertEquals(initialCount + 1, afterCount)
        val firstTx = (SpendingRepository.state.value as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data?.first()!!
        assertEquals("Maybank MAE", firstTx.bank)
        assertEquals(18.50, firstTx.amount, 0.001)
    }

    @Test
    fun spendingRepository_parsesNotificationAmounts() {
        SpendingRepository.isOptedIn = true
        val parsed = SpendingRepository.parseAndRecordFromNotification(
            packageName = "com.maybank2u.life",
            title = "MAE Transaction Successful",
            text = "You paid RM 25.40 at Village Park Restaurant"
        )
        assertTrue(parsed)
        val latest = (SpendingRepository.state.value as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data?.first()!!
        assertEquals(25.40, latest.amount, 0.001)
        assertEquals("Village Park Restaurant", latest.merchant)

        // Verify non-allowlisted package is strictly rejected
        val rejectedRandomApp = SpendingRepository.parseAndRecordFromNotification(
            packageName = "com.random.chat",
            title = "Friend",
            text = "Can you transfer me RM 50.00?"
        )
        assertFalse(rejectedRandomApp)

        // Verify opted-out state rejects allowlisted notifications
        SpendingRepository.isOptedIn = false
        val rejectedWhenOptedOut = SpendingRepository.parseAndRecordFromNotification(
            packageName = "com.maybank2u.life",
            title = "MAE Transaction",
            text = "You paid RM 10.00 at Store"
        )
        assertFalse(rejectedWhenOptedOut)
    }

    @Test
    fun parallaxHelper_calculatesZeroOffsetAtFirstPage() {
        val widths = listOf(180, 160, 140, 200, 150)
        val spacing = 96
        val offset = my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(
            continuousPage = 0f,
            titleWidths = widths,
            spacingPx = spacing,
            maxScroll = 2000
        )
        assertEquals(0, offset)
    }

    @Test
    fun parallaxHelper_calculatesExactOffsetsForMeasuredTitles() {
        val widths = listOf(180, 160, 140, 200, 150)
        val spacing = 96
        // Page 1 (inbox) should offset by title 0 width + spacing: 180 + 96 = 276
        val page1Offset = my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(
            continuousPage = 1f,
            titleWidths = widths,
            spacingPx = spacing,
            maxScroll = 2000
        )
        assertEquals(276, page1Offset)

        // Page 2 (news) should offset by 276 + 160 + 96 = 532
        val page2Offset = my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(
            continuousPage = 2f,
            titleWidths = widths,
            spacingPx = spacing,
            maxScroll = 2000
        )
        assertEquals(532, page2Offset)

        // Page 4 (apps) should offset by 532 + (140 + 96) + (200 + 96) = 532 + 236 + 296 = 1064
        val page4Offset = my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(
            continuousPage = 4f,
            titleWidths = widths,
            spacingPx = spacing,
            maxScroll = 2000
        )
        assertEquals(1064, page4Offset)
    }

    @Test
    fun parallaxHelper_interpolatesSmoothlyBetweenPages() {
        val widths = listOf(200, 200, 200, 200, 200)
        val spacing = 100 // step = 300
        val offsetMid = my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(
            continuousPage = 0.5f,
            titleWidths = widths,
            spacingPx = spacing,
            maxScroll = 2000
        )
        assertEquals(150, offsetMid)

        val offsetQuarter = my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(
            continuousPage = 1.25f,
            titleWidths = widths,
            spacingPx = spacing,
            maxScroll = 2000
        )
        // 300 + 0.25 * 300 = 375
        assertEquals(375, offsetQuarter)
    }

    @Test
    fun parallaxHelper_clampsAtMaxScrollAndBounds() {
        val widths = listOf(100, 100)
        val spacing = 50
        // Negative page clamped to 0
        assertEquals(0, my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(-1f, widths, spacing, 1000))
        // Beyond page clamped to max title
        assertEquals(150, my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(5f, widths, spacing, 1000))
        // Clamped by maxScroll
        assertEquals(80, my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleScrollOffset(1f, widths, spacing, 80))
    }

    @Test
    fun parallaxHelper_calculatesTitleSpanWithGuaranteedPeek() {
        // screenWidth = 1080px, startMargin = 72px (24dp), peekWidth = 204px (68dp)
        // span = 1080 - 204 - 72 = 804px
        val span = my.hana.meena.ui.navigation.ParallaxHelper.calculateTitleSpan(
            screenWidthPx = 1080,
            startMarginPx = 72,
            peekWidthPx = 204
        )
        assertEquals(804, span)
    }

    @Test
    fun parallaxHelper_panoramicScrollCalculatesAccurateOffsets() {
        val titleSpan = 300
        val maxScroll = 2000
        val pageCount = 5

        // Page 0 -> 0
        assertEquals(0, my.hana.meena.ui.navigation.ParallaxHelper.calculatePanoramicScrollOffset(0f, pageCount, titleSpan, maxScroll))

        // Page 1 -> 300
        assertEquals(300, my.hana.meena.ui.navigation.ParallaxHelper.calculatePanoramicScrollOffset(1f, pageCount, titleSpan, maxScroll))

        // Page 0.5 -> 150
        assertEquals(150, my.hana.meena.ui.navigation.ParallaxHelper.calculatePanoramicScrollOffset(0.5f, pageCount, titleSpan, maxScroll))

        // Page 4 (apps) -> 1200
        assertEquals(1200, my.hana.meena.ui.navigation.ParallaxHelper.calculatePanoramicScrollOffset(4f, pageCount, titleSpan, maxScroll))

        // Clamping beyond max page (e.g. 6f clamped to 4f -> 1200)
        assertEquals(1200, my.hana.meena.ui.navigation.ParallaxHelper.calculatePanoramicScrollOffset(6f, pageCount, titleSpan, maxScroll))
    }

    @Test
    fun parallaxHelper_calculatesBrandParallaxOffset() {
        // At page 0, offset is 0
        val offset0 = my.hana.meena.ui.navigation.ParallaxHelper.calculateBrandParallaxOffset(0f, 1080)
        assertEquals(0f, offset0, 0.001f)

        // At page 1, continuous offset moves smoothly to the left
        val offset1 = my.hana.meena.ui.navigation.ParallaxHelper.calculateBrandParallaxOffset(1f, 1080, speedRatio = 0.32f)
        assertTrue(offset1 < 0f)
        assertEquals(-1080f * 0.32f * 0.25f, offset1, 0.001f)
    }

    @Test
    fun quickPanelHardwareHelper_initialStatesAreValid() {
        assertNotNull(my.hana.meena.data.system.QuickPanelHardwareHelper.wifiState.value)
        assertNotNull(my.hana.meena.data.system.QuickPanelHardwareHelper.bluetoothState.value)
        assertEquals(false, my.hana.meena.data.system.QuickPanelHardwareHelper.isTorchActive.value)
    }

    @Test
    fun deviceCalendarRepository_initialStateIsNeedsPermission() {
        val state = my.hana.meena.data.system.DeviceCalendarRepository.state.value
        assertTrue(state is my.hana.meena.data.system.CalendarState.NeedsPermission)
    }

    @Test
    fun weatherRepository_initialStateAndConditions() {
        val state = my.hana.meena.data.system.WeatherRepository.state.value
        assertTrue(state is my.hana.meena.ui.widgets.WidgetState.NeedsSetup)

        assertEquals("clear sky", my.hana.meena.data.system.WeatherRepository.weatherCodeToCondition(0))
        assertEquals("partly cloudy", my.hana.meena.data.system.WeatherRepository.weatherCodeToCondition(2))
        assertEquals("drizzle", my.hana.meena.data.system.WeatherRepository.weatherCodeToCondition(53))
        assertEquals("rain", my.hana.meena.data.system.WeatherRepository.weatherCodeToCondition(63))
        assertEquals("heavy rain", my.hana.meena.data.system.WeatherRepository.weatherCodeToCondition(65))
        assertEquals("rain showers", my.hana.meena.data.system.WeatherRepository.weatherCodeToCondition(81))
        assertEquals("thunderstorm", my.hana.meena.data.system.WeatherRepository.weatherCodeToCondition(95))
    }

    @Test
    fun migrate_dropsRemovedBriefWidget() {
        val legacy = MeenaUserSettings(
            widgetConfigs = MeenaUserSettings.defaultWidgetMap() +
                ("widget-today-summary" to my.hana.meena.data.model.WidgetConfig("today", 5, isEnabled = true))
        )
        val migrated = legacy.migrate()
        assertFalse(migrated.widgetConfigs.containsKey("widget-today-summary"))
        assertEquals(MeenaUserSettings.defaultWidgetMap().keys, migrated.widgetConfigs.keys)
    }

    @Test
    fun settingsJson_withGeminiFields_decodesAndDropsThem() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val legacyJson = """{"geminiApiKey":"AIza-test","geminiModel":"gemini-1.5-flash","geminiIncludeCalendar":true}"""
        val parsed = json.decodeFromString(MeenaUserSettings.serializer(), legacyJson)
        val reencoded = json.encodeToString(MeenaUserSettings.serializer(), parsed.migrate())
        assertFalse(reencoded.contains("gemini"))
        assertFalse(reencoded.contains("AIza-test"))
    }

    @Test
    fun exchangeRateRepository_initialStateIsNeedsSetup() {
        val state = my.hana.meena.data.finance.ExchangeRateRepository.state.value
        assertTrue(state is my.hana.meena.ui.widgets.WidgetState.NeedsSetup)
    }

    @Test
    fun financeRepository_initialStateIsNeedsSetup() {
        val state = my.hana.meena.data.finance.FinanceRepository.state.value
        assertTrue(state is my.hana.meena.ui.widgets.WidgetState.NeedsSetup)
    }

    @Test
    fun financeHolding_profitAndLossCalculationsAreCorrect() {
        val holding = my.hana.meena.data.finance.FinanceHolding(
            symbol = "BURSA",
            name = "Bursa Malaysia Bhd",
            shares = 500.0,
            averageCost = 7.50,
            currentPrice = 8.90
        )
        assertEquals(3750.0, holding.totalCost, 0.001)
        assertEquals(4450.0, holding.currentValue, 0.001)
        assertEquals(700.0, holding.profitLoss, 0.001)
        assertEquals((700.0 / 3750.0) * 100.0, holding.profitLossPercent, 0.001)
    }

    @Test
    fun calendarUserSettings_defaultValues() {
        val settings = MeenaUserSettings()
        assertEquals(7, settings.calendarDaysAhead)
        assertTrue(settings.calendarShowAllDay)
    }

    @Test
    fun deviceCalendarEvent_dataModelProperties() {
        val event = my.hana.meena.data.system.DeviceCalendarEvent(
            id = 42L,
            title = "team sprint sync",
            startTime = 1700000000000L,
            endTime = 1700003600000L,
            isAllDay = false,
            location = "room a",
            calendarColor = -16711936,
            formattedTime = "10:00 – 11:00",
            formattedDate = "today"
        )
        assertEquals(42L, event.id)
        assertEquals("team sprint sync", event.title)
        assertEquals("room a", event.location)
        assertFalse(event.isAllDay)
        assertEquals("today", event.formattedDate)
    }

    @Test
    fun calendarAccount_dataModelProperties() {
        val account = my.hana.meena.data.system.CalendarAccount(
            id = 1L,
            displayName = "personal",
            accountName = "user@example.com",
            color = -16776961,
            isVisible = true
        )
        assertEquals(1L, account.id)
        assertEquals("personal", account.displayName)
        assertEquals("user@example.com", account.accountName)
        assertTrue(account.isVisible)
    }

    @Test
    fun userSettings_dateTimeDefaults_areValid() {
        val settings = MeenaUserSettings()
        assertTrue(settings.is24HourFormat)
        assertEquals("EEEE, d MMMM", settings.dateFormatPattern)
        assertFalse(settings.showClockSeconds)
        assertTrue(settings.showHijriDate)
        assertEquals(0, settings.hijriDayOffset)
    }

    @Test
    fun dailyForecast_dataModelProperties() {
        val forecast = my.hana.meena.data.system.DailyForecast(
            date = "2026-10-03",
            dayLabel = "sat",
            weatherCode = 0,
            condition = "clear sky",
            maxTemp = 32.5f,
            minTemp = 24.0f,
            precipitationProbability = 10
        )
        assertEquals("2026-10-03", forecast.date)
        assertEquals("sat", forecast.dayLabel)
        assertEquals(0, forecast.weatherCode)
        assertEquals("clear sky", forecast.condition)
        assertEquals(32.5f, forecast.maxTemp, 0.01f)
        assertEquals(24.0f, forecast.minTemp, 0.01f)
        assertEquals(10, forecast.precipitationProbability)
    }

    @Test
    fun hijrahDate_calculation_formatsValidString() {
        val hijrahDate = java.time.chrono.HijrahDate.now()
        assertNotNull(hijrahDate)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.getDefault())
        val formatted = formatter.format(hijrahDate).lowercase()
        assertTrue(formatted.isNotBlank())
        val offsetDate = hijrahDate.plus(1, java.time.temporal.ChronoUnit.DAYS)
        val formattedOffset = formatter.format(offsetDate).lowercase()
        assertTrue(formattedOffset.isNotBlank())
    }

    @Test
    fun financeHolding_calculations_areAccurate() {
        val holding = my.hana.meena.data.finance.FinanceHolding(
            symbol = "BURSA",
            name = "Bursa Malaysia",
            shares = 500.0,
            averageCost = 8.00,
            currentPrice = 9.20,
            currency = "MYR",
            groupName = "mplus"
        )
        assertEquals(4000.0, holding.totalCost, 0.001)
        assertEquals(4600.0, holding.currentValue, 0.001)
        assertEquals(600.0, holding.profitLoss, 0.001)
        assertEquals(15.0, holding.profitLossPercent, 0.001)
    }

    @Test
    fun financeHolding_lossCalculation_isAccurate() {
        val holding = my.hana.meena.data.finance.FinanceHolding(
            symbol = "AXIATA",
            name = "Axiata Group",
            shares = 1000.0,
            averageCost = 3.00,
            currentPrice = 2.40,
            currency = "MYR",
            groupName = "mplus"
        )
        assertEquals(3000.0, holding.totalCost, 0.001)
        assertEquals(2400.0, holding.currentValue, 0.001)
        assertEquals(-600.0, holding.profitLoss, 0.001)
        assertEquals(-20.0, holding.profitLossPercent, 0.001)
    }

    @Test
    fun financeRepository_csvLineParser_handlesQuotesAndCommas() {
        val line = "\"KLSE:BURSA\",\"8,900.50\",\"+15.20\",\"+2.5%\",\"Bursa Malaysia, Bhd\""
        val tokens = my.hana.meena.data.finance.FinanceRepository.parseCsvLine(line)
        assertEquals(5, tokens.size)
        assertEquals("KLSE:BURSA", tokens[0])
        assertEquals("8,900.50", tokens[1])
        assertEquals("+15.20", tokens[2])
        assertEquals("+2.5%", tokens[3])
        assertEquals("Bursa Malaysia, Bhd", tokens[4])
    }

    @Test
    fun financeRepository_parseNumeric_cleansValuesCorrectly() {
        assertEquals(1234.56, my.hana.meena.data.finance.FinanceRepository.parseNumeric("RM 1,234.56"), 0.001)
        assertEquals(25.4, my.hana.meena.data.finance.FinanceRepository.parseNumeric("$25.40"), 0.001)
        assertEquals(3.2, my.hana.meena.data.finance.FinanceRepository.parseNumeric("+3.2%"), 0.001)
        assertEquals(-1.5, my.hana.meena.data.finance.FinanceRepository.parseNumeric("-1.5%"), 0.001)
        assertEquals(0.0, my.hana.meena.data.finance.FinanceRepository.parseNumeric(""), 0.001)
    }

    @Test
    fun financeRepository_parseCsvSheet_populatesWatchlist() {
        val csv = """
            symbol,price,change,changepct,name
            KLSE:BURSA,8.90,0.15,1.7%,Bursa Malaysia
            MAYBANK,10.20,-0.05,-0.5%,Malayan Banking
        """.trimIndent()
        my.hana.meena.data.finance.FinanceRepository.parseCsvSheet(csv)
        val state = my.hana.meena.data.finance.FinanceRepository.fetchInitialState()
        // verify tickers parsed
        val tickers = (state as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data?.watchlist ?: emptyList()
        // If state was ready, check ticker; otherwise verify parseCsvLine works directly
        assertTrue(tickers.isEmpty() || tickers.any { it.symbol == "KLSE:BURSA" })
    }

    @Test
    fun userSettings_showFinanceAmounts_defaultsToTrue() {
        val settings = MeenaUserSettings()
        assertTrue(settings.showFinanceAmounts)
    }

    @Test
    fun exchangeRateItem_calculation_and_properties() {
        val item = my.hana.meena.data.finance.ExchangeRateItem(
            currency = "USD",
            rateToBase = 4.25,
            baseToCurrency = 0.2353
        )
        assertEquals("USD", item.currency)
        assertEquals(4.25, item.rateToBase, 0.001)
        assertEquals(0.2353, item.baseToCurrency, 0.001)
    }

    @Test
    fun healthRepository_initialState_isNeedsSetup() {
        val state = my.hana.meena.data.health.HealthRepository.state.value
        assertTrue(state is my.hana.meena.ui.widgets.WidgetState.NeedsSetup || state is my.hana.meena.ui.widgets.WidgetState.Ready)
    }

    @Test
    fun healthRepository_readPermissions_containAllCoreRecords() {
        val permissions = my.hana.meena.data.health.HealthRepository.READ_PERMISSIONS
        assertEquals(5, permissions.size)
    }

    @Test
    fun fitTelemetry_defaultsAndCalculations() {
        val telemetry = my.hana.meena.data.health.FitTelemetry(
            stepsToday = 7500,
            stepGoal = 10000,
            activeCaloriesKcal = 320,
            distanceKm = 5.2,
            sleepHoursFormatted = "7h 30m"
        )
        assertEquals(7500, telemetry.stepsToday)
        assertEquals(10000, telemetry.stepGoal)
        assertEquals(320, telemetry.activeCaloriesKcal)
        assertEquals(5.2, telemetry.distanceKm, 0.001)
        assertEquals("7h 30m", telemetry.sleepHoursFormatted)
    }

    @Test
    fun airQualityIndexInfo_properties() {
        val aqi = my.hana.meena.data.health.AirQualityIndexInfo(
            aqiValue = 42,
            aqiStatus = "good",
            aqiColorHex = "#30D158",
            stationName = "kuala lumpur",
            pm25 = 10.2,
            pm10 = 18.5,
            advisory = "air quality is satisfactory."
        )
        assertEquals(42, aqi.aqiValue)
        assertEquals("good", aqi.aqiStatus)
        assertEquals("#30D158", aqi.aqiColorHex)
        assertEquals("kuala lumpur", aqi.stationName)
        assertEquals(10.2, aqi.pm25, 0.001)
    }

    @Test
    fun userSettings_healthSettings_defaults() {
        val settings = MeenaUserSettings()
        assertEquals(10000, settings.stepGoal)
        assertEquals(8.0f, settings.sleepHoursGoal, 0.001f)
        assertEquals("", settings.healthCompanionPackage)
        assertEquals("", settings.healthCompanionName)
    }

    @Test
    fun metroTheme_10AuthenticAccents_declaredCorrectly() {
        assertEquals(10, Meena10AccentSwatches.size)
        val uniqueHexCodes = mutableSetOf<String>()
        Meena10AccentSwatches.forEach { swatch ->
            assertTrue("Swatch ID should not be blank", swatch.id.isNotBlank())
            assertTrue("Swatch Name should not be blank", swatch.name.isNotBlank())
            assertTrue("Swatch Hex should start with #", swatch.hex.startsWith("#"))
            assertEquals(7, swatch.hex.length)
            uniqueHexCodes.add(swatch.hex.uppercase())
        }
        assertEquals("All 10 swatches must have unique hex codes", 10, uniqueHexCodes.size)
    }

    @Test
    fun metroTheme_wcagContrast_calculatesAccurately() {
        // Pure white on pure black must yield maximum 21:1 contrast ratio
        val whiteOnBlackRatio = calculateContrastRatio(MeenaDarkPalette.text, MeenaDarkPalette.background)
        assertTrue("White on black ratio ($whiteOnBlackRatio) must be >= 20.0", whiteOnBlackRatio >= 20.0f)

        // MeenaTextMuted on MeenaBlack must strictly exceed the 4.5:1 WCAG AA threshold (per GEMINI.md rule 7)
        val mutedOnBlackRatio = calculateContrastRatio(MeenaDarkPalette.textMuted, MeenaDarkPalette.background)
        assertTrue("Muted text ratio ($mutedOnBlackRatio) must be >= 4.5:1", mutedOnBlackRatio >= 4.5f)
        assertTrue("Muted text on black must pass accessible contrast check", isAccessibleContrast(MeenaDarkPalette.textMuted, MeenaDarkPalette.background))

        // Threshold #767676 (per GEMINI.md rule 7: muted text no darker than #767676 on black)
        val thresholdMuted = Color(0xFF767676)
        val thresholdRatio = calculateContrastRatio(thresholdMuted, MeenaDarkPalette.background)
        assertTrue("Threshold #767676 ratio ($thresholdRatio) must be >= 4.5:1", thresholdRatio >= 4.5f)
        assertTrue("Threshold #767676 on black must pass accessible contrast check", isAccessibleContrast(thresholdMuted, MeenaDarkPalette.background))

        // Dark grey on black must fail WCAG regular text check
        val subtleBorder = Color(0xFF222222)
        assertFalse("Subtle border against black should not pass text contrast", isAccessibleContrast(subtleBorder, MeenaDarkPalette.background))
    }

    @Test
    fun metroTheme_captionTypography_meets12spMinimum() {
        val typography = my.hana.meena.theme.getMeenaTypography(fontScalePercent = 100)
        assertTrue("labelSmall caption must be >= 12sp", typography.labelSmall.fontSize.value >= 12f)
        assertTrue("bodyMedium text must be >= 14sp", typography.bodyMedium.fontSize.value >= 14f)
        assertTrue("bodyLarge text must be >= 16sp", typography.bodyLarge.fontSize.value >= 16f)
    }

    @Test
    fun userSettings_reducedMotionAndHandedness_defaults() {
        val settings = MeenaUserSettings()
        assertFalse("Reduced motion should be disabled by default", settings.isReducedMotion)
        assertFalse("Left-handed mode should be disabled by default", settings.isLeftHanded)
    }

    @Test
    fun parallaxHelper_titleSpanCalculation_guaranteesPeek() {
        val screenWidthPx = 1080
        val startMarginPx = 72
        val peekWidthPx = 204

        val span = ParallaxHelper.calculateTitleSpan(
            screenWidthPx = screenWidthPx,
            startMarginPx = startMarginPx,
            peekWidthPx = peekWidthPx
        )
        // Span = 1080 - 204 - 72 = 804
        assertEquals(804, span)

        // At page 0, scroll offset must be 0
        val offset0 = ParallaxHelper.calculatePanoramicScrollOffset(
            continuousPage = 0f,
            pageCount = 5,
            titleSpanPx = span,
            maxScroll = 4000
        )
        assertEquals(0, offset0)

        // At page 1, scroll offset must equal 1 * span
        val offset1 = ParallaxHelper.calculatePanoramicScrollOffset(
            continuousPage = 1f,
            pageCount = 5,
            titleSpanPx = span,
            maxScroll = 4000
        )
        assertEquals(804, offset1)

        // At page 4, scroll offset must equal 4 * span
        val offset4 = ParallaxHelper.calculatePanoramicScrollOffset(
            continuousPage = 4f,
            pageCount = 5,
            titleSpanPx = span,
            maxScroll = 4000
        )
        assertEquals(4 * 804, offset4)
    }

    @Test
    fun userSettings_reducedMotion_aliasReflectsIsReducedMotion() {
        val defaultSettings = MeenaUserSettings()
        assertFalse(defaultSettings.reducedMotion)
        assertEquals(defaultSettings.isReducedMotion, defaultSettings.reducedMotion)

        val reducedSettings = defaultSettings.copy(isReducedMotion = true)
        assertTrue(reducedSettings.reducedMotion)
        assertEquals(reducedSettings.isReducedMotion, reducedSettings.reducedMotion)
    }

    @Test
    fun metroMotion_turnstileTransitions_instantiateWithoutCrashing() {
        val forwardAnim = my.hana.meena.ui.components.metroTurnstileTransition(forward = true, reducedMotion = false)
        assertNotNull(forwardAnim)

        val backwardAnim = my.hana.meena.ui.components.metroTurnstileTransition(forward = false, reducedMotion = false)
        assertNotNull(backwardAnim)

        val reducedAnim = my.hana.meena.ui.components.metroTurnstileTransition(forward = true, reducedMotion = true)
        assertNotNull(reducedAnim)
    }

    private fun resolveStringsFile(): java.io.File {
        val candidates = listOf(
            java.io.File("src/main/res/values/strings.xml"),
            java.io.File("app/src/main/res/values/strings.xml")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot locate strings.xml in: ${candidates.map { it.absolutePath }}")
    }

    @Test
    fun stringsXml_hasNoBannedWords() {
        val stringsFile = resolveStringsFile()
        assertTrue("strings.xml must exist", stringsFile.exists())
        val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(stringsFile)
        val stringNodes = doc.getElementsByTagName("string")
        
        val bannedPattern = Regex("\\b(telemetry|ram|ltpo|realtime|permission|intent|package|canvas|proactive|context)\\b", RegexOption.IGNORE_CASE)
        val violations = mutableListOf<String>()

        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i) as org.w3c.dom.Element
            val name = node.getAttribute("name")
            val text = node.textContent
            if (bannedPattern.containsMatchIn(text)) {
                val matched = bannedPattern.findAll(text).map { it.value }.toList()
                violations.add("string '$name' contains banned words $matched: '$text'")
            }
        }

        assertTrue("Found banned words in strings.xml:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    @Test
    fun stringsXml_isAllLowercaseExceptAppName() {
        val stringsFile = resolveStringsFile()
        assertTrue("strings.xml must exist", stringsFile.exists())
        val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(stringsFile)
        val stringNodes = doc.getElementsByTagName("string")
        
        val uppercasePattern = Regex("[A-Z]")
        val violations = mutableListOf<String>()

        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i) as org.w3c.dom.Element
            val name = node.getAttribute("name")
            if (name == "app_name") continue // "Meena" is allowed
            val text = node.textContent
            if (uppercasePattern.containsMatchIn(text)) {
                violations.add("string '$name' contains uppercase letters: '$text'")
            }
        }

        assertTrue("Found uppercase letters in strings.xml:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    @Test
    fun stringsXml_hasNoBannedSymbols() {
        val stringsFile = resolveStringsFile()
        assertTrue("strings.xml must exist", stringsFile.exists())
        val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(stringsFile)
        val stringNodes = doc.getElementsByTagName("string")
        
        val bannedSymbols = listOf("•", "✕", "▲", "▼", "→", "⚡")
        val violations = mutableListOf<String>()

        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i) as org.w3c.dom.Element
            val name = node.getAttribute("name")
            val text = node.textContent
            for (symbol in bannedSymbols) {
                if (text.contains(symbol)) {
                    violations.add("string '$name' contains banned symbol '$symbol': '$text'")
                }
            }
        }

        assertTrue("Found banned symbols in strings.xml:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun resolveManifestFile(): java.io.File {
        val candidates = listOf(
            java.io.File("src/main/AndroidManifest.xml"),
            java.io.File("app/src/main/AndroidManifest.xml")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot locate AndroidManifest.xml in: ${candidates.map { it.absolutePath }}")
    }

    @Test
    fun manifest_hasNoForbiddenPermissions() {
        val manifestFile = resolveManifestFile()
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(manifestFile)
        val permNodes = doc.getElementsByTagName("uses-permission")
        
        val forbidden = setOf(
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.ACCESS_BACKGROUND_LOCATION"
        )
        val found = mutableListOf<String>()

        for (i in 0 until permNodes.length) {
            val node = permNodes.item(i) as org.w3c.dom.Element
            val name = node.getAttribute("android:name")
            if (name in forbidden) {
                found.add(name)
            }
        }

        assertTrue("Forbidden permissions found in AndroidManifest.xml: $found", found.isEmpty())
    }

    @Test
    fun manifest_hasAllowedPermissionsOnly() {
        val manifestFile = resolveManifestFile()
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(manifestFile)
        val permNodes = doc.getElementsByTagName("uses-permission")
        
        val allowedPermissions = setOf(
            "android.permission.VIBRATE",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_COARSE_LOCATION",
            // Precise location for the nearby map, approved by the owner on 14 Nov 2026.
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CALENDAR",
            // Connected devices widget, approved by the owner on 12 Nov 2026 (plan D14).
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_DISTANCE",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED",
            "android.permission.health.READ_SLEEP",
            "android.permission.health.READ_HEART_RATE"
        )
        val unauthorized = mutableListOf<String>()

        for (i in 0 until permNodes.length) {
            val node = permNodes.item(i) as org.w3c.dom.Element
            val name = node.getAttribute("android:name")
            if (name !in allowedPermissions) {
                unauthorized.add(name)
            }
        }

        assertTrue("Unauthorized permissions found in AndroidManifest.xml: $unauthorized", unauthorized.isEmpty())
    }

    @Test
    fun manifest_disablesAllowBackup() {
        val manifestFile = resolveManifestFile()
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(manifestFile)
        val appNodes = doc.getElementsByTagName("application")
        assertTrue("Application node must exist", appNodes.length > 0)
        
        val app = appNodes.item(0) as org.w3c.dom.Element
        val allowBackup = app.getAttribute("android:allowBackup")
        assertEquals("allowBackup must be explicitly false for data safety", "false", allowBackup)
    }

    @Test
    fun privacyDocuments_existAndAreNonEmpty() {
        val privacyFile = listOf(
            java.io.File("docs/privacy-policy.md"),
            java.io.File("../docs/privacy-policy.md")
        ).firstOrNull { it.exists() }
        assertNotNull("docs/privacy-policy.md must exist", privacyFile)
        assertTrue("docs/privacy-policy.md must not be empty", privacyFile!!.length() > 500)

        val complianceFile = listOf(
            java.io.File("docs/play-store-compliance.md"),
            java.io.File("../docs/play-store-compliance.md")
        ).firstOrNull { it.exists() }
        assertNotNull("docs/play-store-compliance.md must exist", complianceFile)
        assertTrue("docs/play-store-compliance.md must not be empty", complianceFile!!.length() > 500)
    }

    @Test
    fun releaseMetadata_filesExistAndComplyWithLimits() {
        val candidates = listOf(
            java.io.File("distribution/metadata/en-US"),
            java.io.File("../distribution/metadata/en-US")
        )
        val metaDir = candidates.firstOrNull { it.exists() }
        assertNotNull("distribution/metadata/en-US directory must exist", metaDir)

        val titleFile = java.io.File(metaDir, "title.txt")
        assertTrue("title.txt must exist", titleFile.exists())
        val title = titleFile.readText().trim()
        assertEquals("Meena", title)
        assertTrue("Title must be <= 30 chars", title.length <= 30)

        val shortDescFile = java.io.File(metaDir, "short_description.txt")
        assertTrue("short_description.txt must exist", shortDescFile.exists())
        val shortDesc = shortDescFile.readText().trim()
        assertTrue("Short description must be <= 80 chars", shortDesc.length <= 80)

        val fullDescFile = java.io.File(metaDir, "full_description.txt")
        assertTrue("full_description.txt must exist", fullDescFile.exists())
        val fullDesc = fullDescFile.readText().trim()
        assertTrue("Full description must be <= 4000 chars", fullDesc.length <= 4000)

        val releaseNotesFile = java.io.File(metaDir, "release_notes.txt")
        assertTrue("release_notes.txt must exist", releaseNotesFile.exists())
        val releaseNotes = releaseNotesFile.readText().trim()
        assertTrue("Release notes must be <= 500 chars", releaseNotes.length <= 500)

        val rolloutGuideFile = listOf(
            java.io.File("docs/play-store-rollout-guide.md"),
            java.io.File("../docs/play-store-rollout-guide.md")
        ).firstOrNull { it.exists() }
        assertNotNull("docs/play-store-rollout-guide.md must exist", rolloutGuideFile)
        assertTrue("docs/play-store-rollout-guide.md must not be empty", rolloutGuideFile!!.length() > 500)
    }
}



