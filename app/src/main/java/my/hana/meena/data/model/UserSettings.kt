package my.hana.meena.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WidgetConfig(
    val hubId: String,
    val position: Int,
    val isCollapsed: Boolean = false,
    val maxLinesOrItems: Int = 5,
    val refreshMinutes: Int = 15,
    val isEnabled: Boolean = true
)

@Serializable
data class MeenaUserSettings(
    val language: String = "en",
    val isStatusBarHidden: Boolean = false,
    val isLeftHanded: Boolean = false,
    val defaultHubIndex: Int = 0,
    val isHapticEnabled: Boolean = true,
    val baseTheme: String = "amoled", // "amoled", "slate", "light"
    val accentHex: String = "#00A4EF", // Meena Cyan default
    val fontFamily: String = "Segoe UI",
    val fontScalePercent: Int = 100,
    val acrylicBlurDp: Int = 20,
    val wallpaperUri: String? = null,
    val wallpaperDimPercent: Int = 40,
    val borderStyle: String = "modern", // "modern" (1dp outline) or "purist" (0dp borderless flat block)
    val pinnedAppIds: List<String> = listOf("com.google.android.gm", "org.telegram.messenger"),
    val pinnedAppSizes: Map<String, String> = emptyMap(), // appId -> "1x1", "2x2", or "4x2" (default "2x2")
    val solatZone: String = "",
    val newsFeeds: List<String> = emptyList(),
    val widgetConfigs: Map<String, WidgetConfig> = defaultWidgetMap()
) {
    companion object {
        fun defaultWidgetMap(): Map<String, WidgetConfig> = mapOf(
            "widget-clock-weather" to WidgetConfig("start", 0, isEnabled = true),
            "widget-weather-forecast" to WidgetConfig("start", 1, isEnabled = true),
            "widget-today-summary" to WidgetConfig("start", 2, isEnabled = true),
            "widget-conversations" to WidgetConfig("start", 3, isEnabled = true),
            "widget-notifications" to WidgetConfig("start", 4, isEnabled = true),
            "widget-device-telemetry" to WidgetConfig("start", 5, isEnabled = true),
            "widget-solat" to WidgetConfig("agenda", 0, isEnabled = true),
            "widget-calendar-month" to WidgetConfig("agenda", 1, isEnabled = true),
            "widget-tasks" to WidgetConfig("agenda", 2, isEnabled = true),
            "widget-notes" to WidgetConfig("agenda", 3, isEnabled = true),
            "widget-news-feed" to WidgetConfig("news", 0, isEnabled = true),
            "widget-finance-charts" to WidgetConfig("finance", 0, isEnabled = true),
            "widget-exchange-rates" to WidgetConfig("finance", 1, isEnabled = true),
            "widget-spending-summary" to WidgetConfig("finance", 2, isEnabled = true),
            "widget-health-device" to WidgetConfig("health", 0, isEnabled = true),
            "widget-health-telemetry" to WidgetConfig("health", 1, isEnabled = true),
            "widget-health-aqi" to WidgetConfig("health", 2, isEnabled = true),
            "widget-map-radar" to WidgetConfig("health", 3, isEnabled = true)
        )
    }
}

