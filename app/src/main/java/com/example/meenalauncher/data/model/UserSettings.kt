package com.example.meenalauncher.data.model

import kotlinx.serialization.Serializable

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
    val enabledWidgets: Map<String, Boolean> = defaultWidgetMap()
) {
    companion object {
        fun defaultWidgetMap(): Map<String, Boolean> = mapOf(
            "widget-clock-weather" to true,
            "widget-weather-forecast" to true,
            "widget-my-apps" to true,
            "widget-calendar-month" to true,
            "widget-today-summary" to true,
            "widget-mailbox" to true,
            "widget-notes" to true,
            "widget-tasks" to true,
            "widget-exchange-rates" to true,
            "widget-finance-charts" to true,
            "widget-spending-summary" to true,
            "widget-notifications" to true,
            "widget-conversations" to true,
            "widget-news-feed" to true,
            "widget-device-telemetry" to true,
            "widget-map-radar" to true
        )
    }
}
