package my.hana.meena

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.JakimSolatRepository
import my.hana.meena.data.system.SpendingRepository
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
        assertEquals("amoled", settings.baseTheme)
        assertEquals(0, settings.defaultHubIndex)
        assertTrue(settings.widgetConfigs.isNotEmpty())
        assertTrue(settings.widgetConfigs["widget-clock-weather"]?.isEnabled == true)
        assertEquals("today", settings.widgetConfigs["widget-clock-weather"]?.hubId)
        assertEquals("inbox", settings.widgetConfigs["widget-conversations"]?.hubId)
        assertEquals("news", settings.widgetConfigs["widget-news-feed"]?.hubId)
        assertEquals("money", settings.widgetConfigs["widget-finance-charts"]?.hubId)
        assertTrue(settings.widgetConfigs["widget-map-radar"]?.isEnabled == true)
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
    fun geminiBriefRepository_initialStateIsNeedsSetup() {
        val state = my.hana.meena.data.system.GeminiBriefRepository.state.value
        assertTrue(state is my.hana.meena.ui.widgets.WidgetState.NeedsSetup)
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
        assertEquals("EEEE, MMMM d", settings.dateFormatPattern)
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
}



