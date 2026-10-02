package com.example.meenalauncher

import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.data.system.JakimSolatRepository
import com.example.meenalauncher.data.system.SpendingRepository
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
        assertTrue(settings.enabledWidgets.isNotEmpty())
        assertTrue(settings.enabledWidgets["widget-clock-weather"] == true)
        assertTrue(settings.enabledWidgets["widget-map-radar"] == true)
    }

    @Test
    fun jakimSolatRepository_returnsValidSchedule() {
        val schedule = JakimSolatRepository.getTodaySchedule("WLY01")
        assertEquals("WLY01", schedule.zone)
        assertTrue(schedule.slots.isNotEmpty())
        assertEquals(6, schedule.slots.size)
        assertNotNull(schedule.nextPrayerName)
        assertNotNull(schedule.nextPrayerTime)
    }

    @Test
    fun spendingRepository_recordsTransactionsCorrectly() {
        val initialCount = SpendingRepository.transactionsFlow.value.size
        SpendingRepository.addTransaction(
            bank = "Maybank MAE",
            bankCode = "M",
            merchant = "Nasi Kandar Pelita",
            channel = "DuitNow QR",
            amount = 18.50
        )
        val afterCount = SpendingRepository.transactionsFlow.value.size
        assertEquals(initialCount + 1, afterCount)
        val firstTx = SpendingRepository.transactionsFlow.value.first()
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
        val latest = SpendingRepository.transactionsFlow.value.first()
        assertEquals(25.40, latest.amount, 0.001)
        assertEquals("Village Park Restaurant", latest.merchant)
    }
}
