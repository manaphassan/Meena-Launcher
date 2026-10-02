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
}

