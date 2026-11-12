package my.hana.meena.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WidgetConfig(
    val hubId: String,
    val position: Int,
    val isCollapsed: Boolean = false,
    val maxLinesOrItems: Int = 5,
    val refreshMinutes: Int = 15,
    val isEnabled: Boolean = true,
    val customSettings: Map<String, String> = emptyMap()
)

@Serializable
data class MeenaUserSettings(
    val language: String = "en",
    val isStatusBarHidden: Boolean = false,
    val isLeftHanded: Boolean = false,
    val isReducedMotion: Boolean = false,
    val defaultHubIndex: Int = 0,
    val isHapticEnabled: Boolean = true,
    val baseTheme: String = "dark", // "dark", "light"
    val accentHex: String = "#00A4EF", // Meena Cyan default
    
    val fontScalePercent: Int = 100,
    
    val wallpaperUri: String? = null,
    val wallpaperDimPercent: Int = 40,
    
    val isMonochromeFavourites: Boolean = true,
    val pinnedAppIds: List<String> = emptyList(),
    val solatZone: String = "",
    val newsFeeds: List<String> = emptyList(),
    val widgetConfigs: Map<String, WidgetConfig> = defaultWidgetMap(),
    // Calendar settings
    val calendarDaysAhead: Int = 7,
    val calendarShowAllDay: Boolean = true,
    // Date & Time settings
    val is24HourFormat: Boolean = true,
    val dateFormatPattern: String = "EEEE, MMMM d",
    val showClockSeconds: Boolean = false,
    val showHijriDate: Boolean = true,
    val hijriDayOffset: Int = 0,
    // Finance privacy
    val showFinanceAmounts: Boolean = true,
    // Health Connect & Companion settings
    val stepGoal: Int = 10000,
    val sleepHoursGoal: Float = 8.0f,
    val healthCompanionPackage: String = "",
    val healthCompanionName: String = "",
    // First-run permission onboarding
    val hasCompletedFirstRunSetup: Boolean = false
) {
    val reducedMotion: Boolean get() = isReducedMotion

    fun migrate(): MeenaUserSettings {
        val updatedConfigs = widgetConfigs.toMutableMap()
        var changed = false

        // Ensure all default widgets exist
        defaultWidgetMap().forEach { (key, defaultConfig) ->
            if (!updatedConfigs.containsKey(key)) {
                updatedConfigs[key] = defaultConfig
                changed = true
            }
        }

        // Drop widgets that no longer exist
        if (updatedConfigs.keys.retainAll(defaultWidgetMap().keys)) {
            changed = true
        }

        // Migrate legacy hubIds
        updatedConfigs.forEach { (key, config) ->
            val targetHubId = when {
                key == "widget-conversations" || key == "widget-notifications" -> {
                    if (config.hubId == "start" || config.hubId == "today") "inbox" else config.hubId
                }
                config.hubId == "start" || config.hubId == "agenda" || config.hubId == "health" -> "today"
                config.hubId == "finance" -> "money"
                else -> config.hubId
            }
            if (targetHubId != config.hubId) {
                updatedConfigs[key] = config.copy(hubId = targetHubId)
                changed = true
            }
        }

        val clampedDefaultHubIndex = defaultHubIndex.coerceIn(0, 4)
        if (clampedDefaultHubIndex != defaultHubIndex) {
            changed = true
        }

        return if (changed) {
            copy(
                widgetConfigs = updatedConfigs,
                defaultHubIndex = clampedDefaultHubIndex
            )
        } else {
            this
        }
    }

    companion object {
        fun defaultWidgetMap(): Map<String, WidgetConfig> = mapOf(
            // Today Hub (clock, forecast, solat, agenda, followed by default-off widgets)
            "widget-clock-weather" to WidgetConfig("today", 0, isEnabled = true),
            "widget-weather-forecast" to WidgetConfig("today", 1, isEnabled = true),
            "widget-favourites" to WidgetConfig("today", 2, maxLinesOrItems = 8, isEnabled = true),
            "widget-solat" to WidgetConfig("today", 3, isEnabled = true),
            "widget-tasks" to WidgetConfig("today", 4, isEnabled = true),
            "widget-calendar-month" to WidgetConfig("today", 6, isEnabled = false),
            "widget-notes" to WidgetConfig("today", 7, isEnabled = false),
            "widget-health-device" to WidgetConfig("today", 8, isEnabled = false),
            "widget-health-telemetry" to WidgetConfig("today", 9, isEnabled = false),
            "widget-health-aqi" to WidgetConfig("today", 10, isEnabled = false),
            "widget-map-radar" to WidgetConfig("today", 11, isEnabled = false),
            "widget-device-telemetry" to WidgetConfig("today", 12, isEnabled = false),
            "widget-frequent-apps" to WidgetConfig("today", 13, maxLinesOrItems = 8, isEnabled = false),
            "widget-bluetooth-devices" to WidgetConfig("today", 14, isEnabled = false),
            "widget-wikipedia" to WidgetConfig("today", 15, isEnabled = false),

            // Inbox Hub (conversations, alerts / notification stream)
            "widget-conversations" to WidgetConfig("inbox", 0, isEnabled = true),
            "widget-notifications" to WidgetConfig("inbox", 1, isEnabled = true),

            // News Hub
            "widget-news-feed" to WidgetConfig("news", 0, isEnabled = true),

            // Money Hub (portfolio & watchlist, exchange rates, spending)
            "widget-finance-charts" to WidgetConfig("money", 0, isEnabled = true),
            "widget-exchange-rates" to WidgetConfig("money", 1, isEnabled = true),
            "widget-spending-summary" to WidgetConfig("money", 2, isEnabled = true)
        )
    }
}

