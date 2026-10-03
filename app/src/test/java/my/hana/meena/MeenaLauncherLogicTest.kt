package my.hana.meena

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.JakimSolatRepository
import my.hana.meena.data.system.SpendingRepository
import org.junit.Assert.assertEquals
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
        val parsed = SpendingRepository.parseAndRecordFromNotification(
            packageName = "com.maybank2u.life",
            title = "MAE Transaction Successful",
            text = "You paid RM 25.40 at Village Park Restaurant"
        )
        assertTrue(parsed)
        val latest = (SpendingRepository.state.value as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data?.first()!!
        assertEquals(25.40, latest.amount, 0.001)
        assertEquals("Village Park Restaurant", latest.merchant)
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
}


