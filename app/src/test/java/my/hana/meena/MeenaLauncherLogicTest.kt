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
        assertTrue(settings.widgetConfigs["widget-map-radar"]?.isEnabled == true)
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

