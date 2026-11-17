package my.hana.meena

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.model.WidgetConfig
import my.hana.meena.ui.settings.SETTINGS_TAB_CONNECTIONS
import my.hana.meena.ui.settings.SETTINGS_TAB_PAGES
import my.hana.meena.ui.settings.SETTINGS_TAB_PERSONALIZE
import my.hana.meena.ui.settings.SETTINGS_TAB_SYSTEM
import my.hana.meena.ui.settings.SETTINGS_TAB_ABOUT
import my.hana.meena.ui.settings.SETTINGS_WIDGET_TITLES
import my.hana.meena.ui.settings.groupWidgetsByPage
import my.hana.meena.ui.settings.normalizeSettingsTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLayoutTest {

    @Test
    fun oldTabNames_mapToNewPivots() {
        assertEquals(SETTINGS_TAB_PAGES, normalizeSettingsTab("widgets"))
        assertEquals(SETTINGS_TAB_CONNECTIONS, normalizeSettingsTab("accounts"))
        assertEquals(SETTINGS_TAB_PERSONALIZE, normalizeSettingsTab("look & feel"))
        assertEquals(SETTINGS_TAB_ABOUT, normalizeSettingsTab("system"))
        assertEquals(SETTINGS_TAB_PERSONALIZE, normalizeSettingsTab("something else"))
    }

    @Test
    fun widgets_groupByPageInPageOrderAndPosition() {
        val groups = groupWidgetsByPage(
            mapOf(
                "widget-news-feed" to WidgetConfig("news", 0),
                "widget-solat" to WidgetConfig("today", 3),
                "widget-clock-weather" to WidgetConfig("today", 0),
                "widget-exchange-rates" to WidgetConfig("money", 1),
                "widget-unknown" to WidgetConfig("today", 1)
            )
        )
        assertEquals(listOf("today", "news", "money"), groups.map { it.first })
        assertEquals(listOf("widget-clock-weather", "widget-solat"), groups.first().second)
    }

    @Test
    fun everyDefaultWidgetExceptRemovedOnes_hasATitle() {
        val missing = MeenaUserSettings.defaultWidgetMap().keys - SETTINGS_WIDGET_TITLES.keys
        // widget-health-device was removed from today (phase 3) and is never shown.
        assertTrue("widgets without a settings row: $missing", missing == setOf("widget-health-device"))
    }
}
