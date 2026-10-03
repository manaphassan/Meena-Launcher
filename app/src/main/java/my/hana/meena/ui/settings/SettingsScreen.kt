package my.hana.meena.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.R
import my.hana.meena.data.finance.FinanceRepository
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.model.WidgetConfig
import my.hana.meena.data.news.NewsRssRepository
import my.hana.meena.theme.MeenaAmber
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaCobalt
import my.hana.meena.theme.MeenaCrimson
import my.hana.meena.theme.MeenaCyan
import my.hana.meena.theme.MeenaDarkSlate
import my.hana.meena.theme.MeenaEmerald
import my.hana.meena.theme.MeenaLime
import my.hana.meena.theme.MeenaMagenta
import my.hana.meena.theme.MeenaOrange
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite

/**
 * Authentic Meena UI Settings Screen.
 * Design language: Clean, light, open, fast (inspired by Windows Phone Metro UI & AIO Launcher).
 * Rules:
 * - Pivot contains link lists only (no switches, sliders, or scrollers directly inside pivot).
 * - Tapping any link navigates to a dedicated sub-page with a clean Metro back header.
 * - Everything lowercase, square corners everywhere, no cards/boxes, 24dp margins.
 */
@Composable
fun SettingsScreen(
    currentSettings: MeenaUserSettings,
    onSaveSettings: (MeenaUserSettings) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onResetDefaults: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var settingsState by remember { mutableStateOf(currentSettings) }
    var activeTab by remember { mutableStateOf("widgets") }
    var activeSubPage by remember { mutableStateOf<String?>(null) }

    val tabs = listOf("widgets", "accounts", "look & feel")

    // Handle back button: if in a sub-page, return to pivot; otherwise close settings
    BackHandler {
        if (activeSubPage != null) {
            activeSubPage = null
        } else {
            onClose()
        }
    }

    // Windows Phone 7 Turnstile 3D Entrance Animation
    var turnstileRotation by remember { mutableFloatStateOf(20f) }
    var turnstileAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 20f,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) { value, _ -> turnstileRotation = value }
    }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = LinearEasing)
        ) { value, _ -> turnstileAlpha = value }
    }

    fun update(transform: (MeenaUserSettings) -> MeenaUserSettings) {
        val updated = transform(settingsState)
        settingsState = updated
        onSaveSettings(updated)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeenaBlack)
            .graphicsLayer {
                rotationY = turnstileRotation
                alpha = turnstileAlpha
                cameraDistance = 16f * density
            }
    ) {
        // Top Global Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (activeSubPage != null) activeSubPage = null else onClose()
                }
            ) {
                IconButton(
                    onClick = {
                        if (activeSubPage != null) activeSubPage = null else onClose()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(0.dp))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "back",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_meena_emblem),
                    contentDescription = "meena",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "meena ui • settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    letterSpacing = 1.sp
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "close", tint = MeenaTextMuted)
            }
        }

        // Sub-page view if active, otherwise main pivot view
        if (activeSubPage != null) {
            SubPageContainer(
                subPageId = activeSubPage!!,
                settingsState = settingsState,
                onUpdate = { transform -> update(transform) },
                onBack = { activeSubPage = null },
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onResetDefaults = onResetDefaults
            )
        } else {
            // Main Pivot Tabs Header
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(tabs) { tab ->
                    val isActive = tab == activeTab
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 32.sp,
                        color = if (isActive) Color.White else MeenaTextMuted,
                        modifier = Modifier
                            .clickable { activeTab = tab }
                            .padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MeenaBorder)
            )

            // Main Pivot Content Area (Link Lists only)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (activeTab) {
                    "widgets" -> {
                        item {
                            Text(
                                text = "tap any widget to configure its page, order and options",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        val widgetList = listOf(
                            "widget-clock-weather" to "clock",
                            "widget-weather-forecast" to "weather forecast",
                            "widget-solat" to "prayer times (solat)",
                            "widget-calendar-month" to "calendar",
                            "widget-tasks" to "tasks",
                            "widget-today-summary" to "brief",
                            "widget-notes" to "notes",
                            "widget-exchange-rates" to "rates",
                            "widget-finance-charts" to "portfolio",
                            "widget-spending-summary" to "spending",
                            "widget-notifications" to "alerts",
                            "widget-conversations" to "chats",
                            "widget-news-feed" to "news feed",
                            "widget-device-telemetry" to "phone",
                            "widget-map-radar" to "map radar",
                            "widget-health-device" to "companion device",
                            "widget-health-telemetry" to "health",
                            "widget-health-aqi" to "air"
                        )

                        items(widgetList) { (id, title) ->
                            val config = settingsState.widgetConfigs[id]
                            val isEnabled = config?.isEnabled != false
                            val hub = config?.hubId ?: "home"
                            val statusText = if (isEnabled) "on • $hub" else "off"

                            SettingLinkRow(
                                title = title,
                                status = statusText,
                                onClick = { activeSubPage = "widget:$id" }
                            )
                        }
                    }

                    "accounts" -> {
                        item {
                            Text(
                                text = "connected services, local hooks and api keys",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        item {
                            val geminiStatus = if (my.hana.meena.data.system.KeystoreSecretsManager.hasGeminiApiKey(context)) "configured" else "needs key"
                            SettingLinkRow(
                                title = "brief (gemini api)",
                                status = geminiStatus,
                                onClick = { activeSubPage = "account:gemini" }
                            )
                        }

                        item {
                            val solatStatus = if (settingsState.solatZone.isNotBlank()) settingsState.solatZone.lowercase() else "auto by gps"
                            SettingLinkRow(
                                title = "waktu solat (jakim)",
                                status = solatStatus,
                                onClick = { activeSubPage = "account:solat" }
                            )
                        }

                        item {
                            SettingLinkRow(
                                title = "alerts & chats",
                                status = "access for alerts, chats & spending",
                                onClick = { activeSubPage = "account:notifications" }
                            )
                        }

                        item {
                            SettingLinkRow(
                                title = "finance & portfolio",
                                status = "quotes & sheets bridge",
                                onClick = { activeSubPage = "account:finance" }
                            )
                        }

                        item {
                            SettingLinkRow(
                                title = "health",
                                status = "activity & health connect",
                                onClick = { activeSubPage = "account:health" }
                            )
                        }

                        item {
                            val calStatus = if (my.hana.meena.data.system.DeviceCalendarRepository.hasCalendarPermission(context)) {
                                "${settingsState.calendarDaysAhead}d lookahead"
                            } else {
                                "needs permission"
                            }
                            SettingLinkRow(
                                title = "calendar",
                                status = calStatus,
                                onClick = { activeSubPage = "account:calendar" }
                            )
                        }
                    }

                    "look & feel" -> {
                        item {
                            val timeMode = if (settingsState.is24HourFormat) "24-hour" else "12-hour"
                            val hijriMode = if (settingsState.showHijriDate) "hijri on" else "hijri off"
                            SettingLinkRow(
                                title = "date & time",
                                status = "$timeMode • $hijriMode",
                                onClick = { activeSubPage = "look:datetime" }
                            )
                        }

                        item {
                            SettingLinkRow(
                                title = "theme & accents",
                                status = "${settingsState.baseTheme} • ${settingsState.accentHex.lowercase()}",
                                onClick = { activeSubPage = "look:theme" }
                            )
                        }

                        item {
                            SettingLinkRow(
                                title = "typography",
                                status = "${settingsState.fontFamily} • ${settingsState.fontScalePercent}%",
                                onClick = { activeSubPage = "look:typography" }
                            )
                        }

                        item {
                            val hand = if (settingsState.isLeftHanded) "left-handed" else "right-handed"
                            SettingLinkRow(
                                title = "ergonomics & layout",
                                status = "$hand • haptics on",
                                onClick = { activeSubPage = "look:ergonomics" }
                            )
                        }

                        item {
                            SettingLinkRow(
                                title = "backup & restore",
                                status = "export or import settings json",
                                onClick = { activeSubPage = "look:backup" }
                            )
                        }

                        item {
                            SettingLinkRow(
                                title = "about meena ui",
                                status = "clean, light, open, fast",
                                onClick = { activeSubPage = "look:about" }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Sub-Page Container rendered when drilling down from pivot link lists.
 */
@Composable
private fun SubPageContainer(
    subPageId: String,
    settingsState: MeenaUserSettings,
    onUpdate: ((MeenaUserSettings) -> MeenaUserSettings) -> Unit,
    onBack: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onResetDefaults: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            subPageId.startsWith("widget:") -> {
                val widgetId = subPageId.removePrefix("widget:")
                val config = settingsState.widgetConfigs[widgetId] ?: WidgetConfig("today", 0)

            item {
                MetroSubPageHeader(
                    parentTitle = "widgets",
                    pageTitle = widgetId.replace("widget-", "").replace("-", " "),
                    onBack = onBack
                )
            }

            // Universal Widget Controls
            item {
                SettingToggleRow(
                    title = "enable widget",
                    subtitle = "show this widget on your home pages",
                    checked = config.isEnabled,
                    onCheckedChange = { checked ->
                        val newMap = settingsState.widgetConfigs.toMutableMap()
                        newMap[widgetId] = config.copy(isEnabled = checked)
                        onUpdate { it.copy(widgetConfigs = newMap) }
                    }
                )
            }

            item {
                Column {
                    Text("destination page", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    val hubs = listOf("today", "inbox", "news", "money", "apps")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hubs.forEach { hub ->
                            val isSelected = config.hubId == hub
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                    .clickable {
                                        val newMap = settingsState.widgetConfigs.toMutableMap()
                                        newMap[widgetId] = config.copy(hubId = hub)
                                        onUpdate { it.copy(widgetConfigs = newMap) }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = hub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else MeenaTextMuted
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingToggleRow(
                    title = "collapse by default",
                    subtitle = "show compact single line summary until tapped",
                    checked = config.isCollapsed,
                    onCheckedChange = { collapsed ->
                        val newMap = settingsState.widgetConfigs.toMutableMap()
                        newMap[widgetId] = config.copy(isCollapsed = collapsed)
                        onUpdate { it.copy(widgetConfigs = newMap) }
                    }
                )
            }

            item {
                Column {
                    Text("refresh cadence", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    val intervals = listOf(15 to "15m", 30 to "30m", 60 to "1h", 0 to "manual")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervals.forEach { (mins, label) ->
                            val isSelected = config.refreshMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                    .clickable {
                                        val newMap = settingsState.widgetConfigs.toMutableMap()
                                        newMap[widgetId] = config.copy(refreshMinutes = mins)
                                        onUpdate { it.copy(widgetConfigs = newMap) }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else MeenaTextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Helper lambdas for reading and saving widget-specific custom settings
            fun custom(key: String, default: String): String =
                config.customSettings[key] ?: default

            fun setCustom(key: String, value: String) {
                val updatedCustom = config.customSettings.toMutableMap().apply { put(key, value) }
                val newMap = settingsState.widgetConfigs.toMutableMap()
                newMap[widgetId] = config.copy(customSettings = updatedCustom)
                onUpdate { it.copy(widgetConfigs = newMap) }
            }

            // Widget-Specific Extended Settings for all 18 modules
            when (widgetId) {
                "widget-clock-weather" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("clock & weather options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "clock time format",
                            subtitle = "choose standard 12-hour or military 24-hour display",
                            options = listOf("12h" to "12-hour", "24h" to "24-hour"),
                            selectedKey = custom("clock_format", "24h"),
                            onSelectKey = { setCustom("clock_format", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show seconds",
                            subtitle = "render live ticking seconds in time display",
                            checked = custom("show_seconds", "false") == "true",
                            onCheckedChange = { setCustom("show_seconds", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show hijri lunar date",
                            subtitle = "display islamic date alongside gregorian date",
                            checked = custom("show_hijri", "true") == "true",
                            onCheckedChange = { setCustom("show_hijri", it.toString()) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "temperature scale",
                            subtitle = "display unit for local temperature",
                            options = listOf("c" to "celsius (°c)", "f" to "fahrenheit (°f)"),
                            selectedKey = custom("temp_unit", "c"),
                            onSelectKey = { setCustom("temp_unit", it) }
                        )
                    }
                }

                "widget-weather-forecast" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("forecast options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "forecast lookahead range",
                            subtitle = "number of upcoming days to forecast",
                            options = listOf("3" to "3 days", "5" to "5 days", "7" to "7 days"),
                            selectedKey = custom("forecast_days", "5"),
                            onSelectKey = { setCustom("forecast_days", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "precipitation probability",
                            subtitle = "show percentage chance of rain",
                            checked = custom("show_rain_prob", "true") == "true",
                            onCheckedChange = { setCustom("show_rain_prob", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "atmospheric details",
                            subtitle = "display humidity and wind velocity",
                            checked = custom("show_details", "false") == "true",
                            onCheckedChange = { setCustom("show_details", it.toString()) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "temperature unit",
                            options = listOf("c" to "celsius (°c)", "f" to "fahrenheit (°f)"),
                            selectedKey = custom("temp_unit", "c"),
                            onSelectKey = { setCustom("temp_unit", it) }
                        )
                    }
                }

                "widget-solat" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("prayer options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SolatZonePicker(
                            currentZone = settingsState.solatZone,
                            onSelectZone = { zone -> onUpdate { it.copy(solatZone = zone) } }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "celestial trajectory arc",
                            subtitle = "render dynamic solar and lunar arc visual card",
                            checked = custom("show_celestial_arc", "true") == "true",
                            onCheckedChange = { setCustom("show_celestial_arc", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "display syuruk & imsak",
                            subtitle = "include sunrise and imsak timings in timetable",
                            checked = custom("show_syuruk_imsak", "true") == "true",
                            onCheckedChange = { setCustom("show_syuruk_imsak", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "prayer time notification alerts",
                            subtitle = "vibrate device upon entering next prayer window",
                            checked = custom("prayer_alerts", "false") == "true",
                            onCheckedChange = { setCustom("prayer_alerts", it.toString()) }
                        )
                    }
                }

                "widget-calendar-month" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("calendar grid options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "first day of the week",
                            options = listOf("sun" to "sunday", "mon" to "monday"),
                            selectedKey = custom("first_day_of_week", "mon"),
                            onSelectKey = { setCustom("first_day_of_week", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show week numbers",
                            subtitle = "display calendar week 1-52 index column",
                            checked = custom("show_week_numbers", "false") == "true",
                            onCheckedChange = { setCustom("show_week_numbers", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "highlight active day",
                            subtitle = "draw accent box around current date",
                            checked = custom("highlight_today", "true") == "true",
                            onCheckedChange = { setCustom("highlight_today", it.toString()) }
                        )
                    }
                }

                "widget-tasks" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("schedule & agenda options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "agenda lookahead window",
                            subtitle = "how far into the future to search for appointments",
                            options = listOf("1" to "today", "3" to "3 days", "7" to "7 days", "14" to "14 days"),
                            selectedKey = custom("agenda_range", "3"),
                            onSelectKey = { setCustom("agenda_range", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "include all-day events",
                            subtitle = "show holidays, birthdays and whole-day blocks",
                            checked = custom("show_all_day", "true") == "true",
                            onCheckedChange = { setCustom("show_all_day", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show location and room",
                            subtitle = "display meeting venue or address",
                            checked = custom("show_event_location", "true") == "true",
                            onCheckedChange = { setCustom("show_event_location", it.toString()) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "maximum appointments displayed",
                            options = listOf("3" to "3 items", "5" to "5 items", "8" to "8 items", "12" to "12 items"),
                            selectedKey = custom("max_events", "5"),
                            onSelectKey = { setCustom("max_events", it) }
                        )
                    }
                }

                "widget-today-summary" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("brief specific options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        GeminiApiKeyInput()
                    }
                    item {
                        SettingSegmentedRow(
                            title = "gemini model",
                            subtitle = "model used to synthesize daily briefing",
                            options = listOf(
                                "gemini-1.5-flash" to "1.5 flash",
                                "gemini-2.0-flash" to "2.0 flash",
                                "gemini-1.5-pro" to "1.5 pro"
                            ),
                            selectedKey = settingsState.geminiModel,
                            onSelectKey = { onUpdate { s -> s.copy(geminiModel = it) } }
                        )
                    }
                    item {
                        Text("context sources for brief", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    item {
                        SettingToggleRow(
                            title = "include calendar in brief",
                            subtitle = "summarize upcoming agenda items",
                            checked = settingsState.geminiIncludeCalendar,
                            onCheckedChange = { onUpdate { it.copy(geminiIncludeCalendar = it.geminiIncludeCalendar.not()) } }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "include weather in brief",
                            subtitle = "summarize today's conditions",
                            checked = settingsState.geminiIncludeWeather,
                            onCheckedChange = { onUpdate { it.copy(geminiIncludeWeather = it.geminiIncludeWeather.not()) } }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "include prayer times in brief",
                            subtitle = "mention next upcoming prayer",
                            checked = settingsState.geminiIncludeSolat,
                            onCheckedChange = { onUpdate { it.copy(geminiIncludeSolat = it.geminiIncludeSolat.not()) } }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "include spending totals in brief",
                            subtitle = "summarize money spent today",
                            checked = settingsState.geminiIncludeFinance,
                            onCheckedChange = { onUpdate { it.copy(geminiIncludeFinance = it.geminiIncludeFinance.not()) } }
                        )
                    }
                }

                "widget-notes" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("quick notes options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "note font size",
                            options = listOf("compact" to "compact (12sp)", "standard" to "standard (14sp)", "large" to "large (16sp)"),
                            selectedKey = custom("note_font_size", "standard"),
                            onSelectKey = { setCustom("note_font_size", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show timestamps",
                            subtitle = "display time elapsed since note was written",
                            checked = custom("show_timestamps", "true") == "true",
                            onCheckedChange = { setCustom("show_timestamps", it.toString()) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "maximum notes displayed",
                            options = listOf("3" to "3 notes", "5" to "5 notes", "10" to "10 notes"),
                            selectedKey = custom("max_notes", "5"),
                            onSelectKey = { setCustom("max_notes", it) }
                        )
                    }
                }

                "widget-exchange-rates" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("exchange rates options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "base currency",
                            subtitle = "primary base denomination for foreign exchange",
                            options = listOf("myr" to "myr", "usd" to "usd", "sgd" to "sgd", "eur" to "eur", "gbp" to "gbp"),
                            selectedKey = custom("base_currency", "myr"),
                            onSelectKey = { setCustom("base_currency", it) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "decimal precision",
                            options = listOf("2" to "2 decimals (0.00)", "4" to "4 decimals (0.0000)"),
                            selectedKey = custom("decimals", "4"),
                            onSelectKey = { setCustom("decimals", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show 24h currency change",
                            subtitle = "display daily net change percentage",
                            checked = custom("show_delta", "true") == "true",
                            onCheckedChange = { setCustom("show_delta", it.toString()) }
                        )
                    }
                }

                "widget-finance-charts" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("financial charts options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "default chart timeframe",
                            options = listOf("1d" to "1 day", "1w" to "1 week", "1m" to "1 month", "1y" to "1 year"),
                            selectedKey = custom("chart_timeframe", "1d"),
                            onSelectKey = { setCustom("chart_timeframe", it) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "chart visual style",
                            options = listOf("sparkline" to "line sparkline", "candlestick" to "candlestick"),
                            selectedKey = custom("chart_style", "sparkline"),
                            onSelectKey = { setCustom("chart_style", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "percentage delta mode",
                            subtitle = "display relative percentage over absolute point value",
                            checked = custom("show_pct", "true") == "true",
                            onCheckedChange = { setCustom("show_pct", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "google sheets quote bridge",
                            subtitle = "read live quotes from user personal google sheet",
                            checked = custom("use_sheets_bridge", "true") == "true",
                            onCheckedChange = { setCustom("use_sheets_bridge", it.toString()) }
                        )
                    }
                }

                "widget-spending-summary" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("spending tracker options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingToggleRow(
                            title = "intercept bank notifications",
                            subtitle = "detect payment alerts from maybank, cimb, tng and grabpay on-device",
                            checked = custom("track_banks", "true") == "true",
                            onCheckedChange = { setCustom("track_banks", it.toString()) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "daily expenditure target",
                            subtitle = "alert when exceeding daily limit",
                            options = listOf("none" to "no limit", "50" to "rm 50", "100" to "rm 100", "200" to "rm 200"),
                            selectedKey = custom("daily_budget", "none"),
                            onSelectKey = { setCustom("daily_budget", it) }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                } catch (e: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("open notification listener settings", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                "widget-notifications" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("alerts & action center options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "maximum alerts displayed",
                            options = listOf("3" to "3 alerts", "5" to "5 alerts", "8" to "8 alerts", "12" to "12 alerts"),
                            selectedKey = custom("max_alerts", "5"),
                            onSelectKey = { setCustom("max_alerts", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "filter silent alerts",
                            subtitle = "hide low-priority background progress and persistent notifications",
                            checked = custom("filter_silent", "true") == "true",
                            onCheckedChange = { setCustom("filter_silent", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show dismiss action",
                            subtitle = "allow clearing individual alerts directly from launcher",
                            checked = custom("show_dismiss", "true") == "true",
                            onCheckedChange = { setCustom("show_dismiss", it.toString()) }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                } catch (e: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("open notification listener settings", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                "widget-conversations" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("conversations options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "layout architecture",
                            subtitle = "vertical stack prevents text clipping on longer messages",
                            options = listOf("vertical" to "vertical stack", "compact" to "compact row"),
                            selectedKey = custom("layout_style", "vertical"),
                            onSelectKey = { setCustom("layout_style", it) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "maximum active conversations",
                            options = listOf("3" to "3 chats", "5" to "5 chats", "8" to "8 chats"),
                            selectedKey = custom("max_chats", "5"),
                            onSelectKey = { setCustom("max_chats", it) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "message snippet preview lines",
                            options = listOf("2" to "2 lines", "3" to "3 lines", "4" to "4 lines"),
                            selectedKey = custom("snippet_lines", "4"),
                            onSelectKey = { setCustom("snippet_lines", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show messaging channel badge",
                            subtitle = "display source app name (whatsapp, telegram, sms)",
                            checked = custom("show_channel", "true") == "true",
                            onCheckedChange = { setCustom("show_channel", it.toString()) }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                } catch (e: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("open notification listener settings", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                "widget-news-feed" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        NewsFeedManager(
                            currentFeeds = settingsState.newsFeeds,
                            onUpdateFeeds = { feeds ->
                                onUpdate { it.copy(newsFeeds = feeds) }
                                NewsRssRepository.updateFeeds(feeds)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("display options", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "stories count",
                            options = listOf("4" to "4 stories", "6" to "6 stories", "8" to "8 stories", "12" to "12 stories"),
                            selectedKey = custom("max_stories", "6"),
                            onSelectKey = { setCustom("max_stories", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show story synopsis preview",
                            subtitle = "render initial lines of article description",
                            checked = custom("show_description", "true") == "true",
                            onCheckedChange = { setCustom("show_description", it.toString()) }
                        )
                    }
                }

                "widget-device-telemetry" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("phone status options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingToggleRow(
                            title = "battery temperature",
                            subtitle = "show current internal battery celsius thermal reading",
                            checked = custom("show_battery_temp", "true") == "true",
                            onCheckedChange = { setCustom("show_battery_temp", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "internal storage capacity",
                            subtitle = "display available gigabytes on device flash memory",
                            checked = custom("show_storage", "true") == "true",
                            onCheckedChange = { setCustom("show_storage", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "memory usage (ram)",
                            subtitle = "display active and free operating system ram",
                            checked = custom("show_ram", "true") == "true",
                            onCheckedChange = { setCustom("show_ram", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "network status",
                            subtitle = "display active wi-fi ssid and cellular network type",
                            checked = custom("show_network", "true") == "true",
                            onCheckedChange = { setCustom("show_network", it.toString()) }
                        )
                    }
                }

                "widget-map-radar" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("radar map options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "radar sweep perimeter",
                            subtitle = "geodesic radar coverage distance",
                            options = listOf("1" to "1.0 km", "2" to "2.0 km", "3" to "3.0 km", "5" to "5.0 km"),
                            selectedKey = custom("radar_radius", "3"),
                            onSelectKey = { setCustom("radar_radius", it) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "default zoom factor",
                            options = listOf("13" to "13x (district)", "15" to "15x (area)", "17" to "17x (street)"),
                            selectedKey = custom("zoom_level", "15"),
                            onSelectKey = { setCustom("zoom_level", it) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "map tile theme",
                            options = listOf("dark" to "dark matter", "light" to "carto light"),
                            selectedKey = custom("map_style", "dark"),
                            onSelectKey = { setCustom("map_style", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "automatic gps recentering",
                            subtitle = "smoothly pan map to new coordinates when moving",
                            checked = custom("auto_recenter", "true") == "true",
                            onCheckedChange = { setCustom("auto_recenter", it.toString()) }
                        )
                    }
                }

                "widget-health-device" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("connected health device", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "paired device profile",
                            options = listOf("watch" to "smartwatch", "band" to "fitness band", "ring" to "smart ring"),
                            selectedKey = custom("device_type", "watch"),
                            onSelectKey = { setCustom("device_type", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "show last sync timestamp",
                            subtitle = "render time elapsed since last bluetooth sync",
                            checked = custom("show_sync_time", "true") == "true",
                            onCheckedChange = { setCustom("show_sync_time", it.toString()) }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                } catch (e: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("open bluetooth settings", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                "widget-health-telemetry" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("fit telemetry options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "daily step target",
                            options = listOf("6000" to "6k steps", "8000" to "8k steps", "10000" to "10k steps", "12000" to "12k steps"),
                            selectedKey = custom("step_goal", "8000"),
                            onSelectKey = { setCustom("step_goal", it) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "daily active calories goal",
                            options = listOf("300" to "300 kcal", "500" to "500 kcal", "750" to "750 kcal", "1000" to "1000 kcal"),
                            selectedKey = custom("calorie_goal", "500"),
                            onSelectKey = { setCustom("calorie_goal", it) }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = "distance measurement unit",
                            options = listOf("km" to "kilometers (km)", "mi" to "miles (mi)"),
                            selectedKey = custom("distance_unit", "km"),
                            onSelectKey = { setCustom("distance_unit", it) }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                                    context.startActivity(intent)
                                } catch (e: Exception) { }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("open health connect settings", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                "widget-health-aqi" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("air quality index options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingSegmentedRow(
                            title = "air quality calculation standard",
                            options = listOf("us" to "us aqi (0-500)", "my" to "malaysia api (0-300)"),
                            selectedKey = custom("aqi_standard", "us"),
                            onSelectKey = { setCustom("aqi_standard", it) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "solar uv index",
                            subtitle = "include ultraviolet radiation index alongside air quality",
                            checked = custom("show_uv", "true") == "true",
                            onCheckedChange = { setCustom("show_uv", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = "particulate matter breakdown",
                            subtitle = "show pm2.5 and pm10 microgram per cubic meter readings",
                            checked = custom("show_pollutants", "false") == "true",
                            onCheckedChange = { setCustom("show_pollutants", it.toString()) }
                        )
                    }
                }
            }
        }

        // Dedicated Account Sub-Pages
        subPageId == "account:gemini" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "accounts",
                    pageTitle = "brief (gemini api)",
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = "add your gemini api key to generate an intelligent morning and evening brief. your key stays strictly on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
            item {
                GeminiApiKeyInput()
            }
            item {
                Column {
                    Text("model selection", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    val models = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        models.forEach { model ->
                            val isSelected = settingsState.geminiModel == model
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                    .clickable { onUpdate { it.copy(geminiModel = model) } }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color.White else MeenaTextMuted
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text("what brief is allowed to read", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            item {
                SettingToggleRow(
                    title = "calendar events",
                    subtitle = "allow brief to read today's schedule",
                    checked = settingsState.geminiIncludeCalendar,
                    onCheckedChange = { onUpdate { it.copy(geminiIncludeCalendar = it.geminiIncludeCalendar.not()) } }
                )
            }
            item {
                SettingToggleRow(
                    title = "weather conditions",
                    subtitle = "allow brief to read local temperature and rain",
                    checked = settingsState.geminiIncludeWeather,
                    onCheckedChange = { onUpdate { it.copy(geminiIncludeWeather = it.geminiIncludeWeather.not()) } }
                )
            }
            item {
                SettingToggleRow(
                    title = "waktu solat",
                    subtitle = "allow brief to read prayer timetable",
                    checked = settingsState.geminiIncludeSolat,
                    onCheckedChange = { onUpdate { it.copy(geminiIncludeSolat = it.geminiIncludeSolat.not()) } }
                )
            }
            item {
                SettingToggleRow(
                    title = "spending totals",
                    subtitle = "allow brief to summarize money spent today",
                    checked = settingsState.geminiIncludeFinance,
                    onCheckedChange = { onUpdate { it.copy(geminiIncludeFinance = it.geminiIncludeFinance.not()) } }
                )
            }
        }

        subPageId == "account:solat" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "accounts",
                    pageTitle = "waktu solat location",
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = "select your official jakim prayer zone in malaysia, or leave empty to auto-detect by coarse gps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
            item {
                SolatZonePicker(
                    currentZone = settingsState.solatZone,
                    onSelectZone = { zone -> onUpdate { it.copy(solatZone = zone) } }
                )
            }
        }

        subPageId == "account:notifications" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "accounts",
                    pageTitle = "notification access",
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = "meena needs notification listener access to show alerts, conversations, and bank spending tracking. no notification text leaves your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
            item {
                Button(
                    onClick = {
                        try {
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("open notification access settings", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        subPageId == "account:finance" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "accounts",
                    pageTitle = "finance & portfolio",
                    onBack = onBack
                )
            }
            item {
                FinanceAccountSettings()
            }
        }

        subPageId == "account:news" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "accounts",
                    pageTitle = "news feeds",
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = "rss feeds are fetched via okhttp3 with clean xml parsing. images are disabled by default for fast, light performance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
        }

        subPageId == "account:health" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "accounts",
                    pageTitle = "health connect",
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = "google fit has retired. health metrics sync securely through android health connect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
        }

        subPageId == "account:calendar" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "accounts",
                    pageTitle = "device calendar",
                    onBack = onBack
                )
            }
            item {
                CalendarAccountSettings(
                    settingsState = settingsState,
                    onUpdate = onUpdate
                )
            }
        }

        // Dedicated Look & Feel Sub-Pages
        subPageId == "look:datetime" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "look & feel",
                    pageTitle = "date & time",
                    onBack = onBack
                )
            }
            item {
                DateTimeSettings(
                    settingsState = settingsState,
                    onUpdate = onUpdate
                )
            }
        }

        subPageId == "look:theme" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "look & feel",
                    pageTitle = "theme & accents",
                    onBack = onBack
                )
            }
            item {
                Text("base theme", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("amoled", "slate", "light").forEach { theme ->
                        val isSelected = settingsState.baseTheme == theme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                .clickable { onUpdate { it.copy(baseTheme = theme) } }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(theme, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else MeenaTextMuted)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("lumia accent swatches", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                val accents = listOf(
                    "#00A4EF" to MeenaCyan,
                    "#A4C400" to MeenaLime,
                    "#FA6800" to MeenaOrange,
                    "#D80073" to MeenaMagenta,
                    "#0050EF" to MeenaCobalt,
                    "#008A00" to MeenaEmerald,
                    "#E51400" to MeenaCrimson,
                    "#F0A30A" to MeenaAmber
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accents.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (hex, col) ->
                                val isSelected = settingsState.accentHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .background(col)
                                        .border(if (isSelected) 2.dp else 0.dp, Color.White)
                                        .clickable { onUpdate { it.copy(accentHex = hex) } }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("border style", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("purist" to "purist flat (0dp)", "modern" to "modern subtle (1dp)").forEach { (key, display) ->
                        val isSelected = settingsState.borderStyle == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                .clickable { onUpdate { it.copy(borderStyle = key) } }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(display, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else MeenaTextMuted)
                        }
                    }
                }
            }
        }

        subPageId == "look:typography" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "look & feel",
                    pageTitle = "typography",
                    onBack = onBack
                )
            }
            item {
                Text("font family", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                val fonts = listOf("Segoe UI", "Inter", "Outfit", "System")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fonts.forEach { font ->
                        val isSelected = settingsState.fontFamily.equals(font, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                .clickable { onUpdate { it.copy(fontFamily = font) } }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(font.lowercase(), style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else MeenaTextMuted)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("font scale", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("${settingsState.fontScalePercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = settingsState.fontScalePercent.toFloat(),
                    onValueChange = { onUpdate { it.copy(fontScalePercent = it.fontScalePercent) } },
                    onValueChangeFinished = {},
                    valueRange = 85f..120f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        subPageId == "look:ergonomics" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "look & feel",
                    pageTitle = "ergonomics & layout",
                    onBack = onBack
                )
            }
            item {
                SettingToggleRow(
                    title = "left-handed mode",
                    subtitle = "mirror navigation hub and swipe gestures",
                    checked = settingsState.isLeftHanded,
                    onCheckedChange = { onUpdate { it.copy(isLeftHanded = it.isLeftHanded.not()) } }
                )
            }
            item {
                SettingToggleRow(
                    title = "haptic feedback",
                    subtitle = "tactile motor vibration on tap",
                    checked = settingsState.isHapticEnabled,
                    onCheckedChange = { onUpdate { it.copy(isHapticEnabled = it.isHapticEnabled.not()) } }
                )
            }
            item {
                SettingToggleRow(
                    title = "hide status bar",
                    subtitle = "hide top battery and clock for pure full-bleed canvas",
                    checked = settingsState.isStatusBarHidden,
                    onCheckedChange = { onUpdate { it.copy(isStatusBarHidden = it.isStatusBarHidden.not()) } }
                )
            }
            item {
                SettingSegmentedRow(
                    title = "default startup page",
                    subtitle = "initial page shown on launch and back gesture",
                    options = listOf(
                        "0" to "today",
                        "1" to "inbox",
                        "2" to "news",
                        "3" to "money",
                        "4" to "apps"
                    ),
                    selectedKey = settingsState.defaultHubIndex.coerceIn(0, 4).toString(),
                    onSelectKey = { key ->
                        val index = key.toIntOrNull() ?: 0
                        onUpdate { it.copy(defaultHubIndex = index) }
                    }
                )
            }
        }

        subPageId == "look:backup" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "look & feel",
                    pageTitle = "backup & restore",
                    onBack = onBack
                )
            }
            item {
                Button(
                    onClick = onExportBackup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("export settings json", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            item {
                Button(
                    onClick = onImportBackup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("import settings json", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            item {
                Button(
                    onClick = onResetDefaults,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1010)),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("reset to defaults", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFB4B4))
                }
            }
        }

        subPageId == "look:about" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "look & feel",
                    pageTitle = "about meena ui",
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = "meena is a personal android launcher built with jetpack compose, designed around clean typography, generous whitespace, and pure speed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            item {
                Text(
                    text = "charter: clean • light • open • fast\nzero ads. zero trackers. zero cloud storage of your keys or balances.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
        }
    }
}
}

@Composable
fun SettingLinkRow(
    title: String,
    status: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title.lowercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status.lowercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.lowercase(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle.lowercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MeenaTextMuted,
                uncheckedTrackColor = MeenaSurface
            )
        )
    }
}

@Composable
fun SettingSegmentedRow(
    title: String,
    subtitle: String? = null,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelectKey: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = title.lowercase(),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle.lowercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (key, label) ->
                val isSelected = selectedKey.equals(key, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                        .clickable { onSelectKey(key) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label.lowercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MeenaTextMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceAccountSettings() {
    val context = LocalContext.current
    val financeState by FinanceRepository.state.collectAsState()
    val data: my.hana.meena.data.finance.FinanceData? = when (val s = financeState) {
        is my.hana.meena.ui.widgets.WidgetState.Ready -> s.data
        is my.hana.meena.ui.widgets.WidgetState.Stale -> s.data
        else -> null
    }

    var sheetUrlInput by remember(data?.sheetUrl) { mutableStateOf(data?.sheetUrl ?: "") }
    var newSymbol by remember { mutableStateOf("") }
    var newShares by remember { mutableStateOf("") }
    var newAvgCost by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newGroup by remember { mutableStateOf("mplus") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Google Sheets Bridge
        Text(
            text = "google sheets bridge",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "connect your own google sheet with googlefinance() formulas. data may be delayed up to 20 minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted
        )

        OutlinedTextField(
            value = sheetUrlInput,
            onValueChange = { sheetUrlInput = it },
            label = { Text("google sheet or csv url", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MeenaSurface,
                unfocusedContainerColor = MeenaSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MeenaBorder
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    FinanceRepository.setSheetUrl(context, sheetUrlInput)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("save & load sheet", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }

            if (sheetUrlInput.isNotBlank()) {
                Button(
                    onClick = {
                        sheetUrlInput = ""
                        FinanceRepository.setSheetUrl(context, "")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("disconnect", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Local Portfolio Holdings
        Text(
            text = "portfolio holdings",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "manage local stock and crypto holdings. all balances are stored privately on your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted
        )

        // Existing Holdings
        val holdings: List<my.hana.meena.data.finance.FinanceHolding> = data?.holdings ?: emptyList()
        if (holdings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (holding in holdings) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${holding.symbol.lowercase()} (${holding.groupName.lowercase()})",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = "${holding.shares.toInt()} shares · cost rm %.2f · now rm %.2f".format(holding.averageCost, holding.currentPrice),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                        IconButton(
                            onClick = { FinanceRepository.removeHolding(context, holding.id) }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "delete", tint = Color(0xFFFF5252))
                        }
                    }
                }
            }
        } else {
            Text(
                text = "no holdings added yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted
            )
        }

        // Add New Holding Form
        Text(
            text = "add holding",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )

        OutlinedTextField(
            value = newSymbol,
            onValueChange = { newSymbol = it },
            label = { Text("symbol (e.g. bursa, hlal, btc)", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MeenaSurface,
                unfocusedContainerColor = MeenaSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newShares,
                onValueChange = { newShares = it },
                label = { Text("shares/units", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MeenaSurface,
                    unfocusedContainerColor = MeenaSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = newAvgCost,
                onValueChange = { newAvgCost = it },
                label = { Text("avg cost (rm)", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MeenaSurface,
                    unfocusedContainerColor = MeenaSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newPrice,
                onValueChange = { newPrice = it },
                label = { Text("current price (rm)", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MeenaSurface,
                    unfocusedContainerColor = MeenaSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = newGroup,
                onValueChange = { newGroup = it },
                label = { Text("account (e.g. mplus)", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MeenaSurface,
                    unfocusedContainerColor = MeenaSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Button(
            onClick = {
                val shares = newShares.toDoubleOrNull() ?: 0.0
                val cost = newAvgCost.toDoubleOrNull() ?: 0.0
                val price = newPrice.toDoubleOrNull() ?: cost
                if (newSymbol.isNotBlank() && shares > 0.0) {
                    FinanceRepository.addHolding(
                        context,
                        my.hana.meena.data.finance.FinanceHolding(
                            symbol = newSymbol.trim().uppercase(),
                            name = newSymbol.trim().uppercase(),
                            shares = shares,
                            averageCost = cost,
                            currentPrice = price,
                            currency = "MYR",
                            groupName = newGroup.trim().ifBlank { "portfolio" }
                        )
                    )
                    newSymbol = ""
                    newShares = ""
                    newAvgCost = ""
                    newPrice = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("add holding", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

@Composable
private fun CalendarAccountSettings(
    settingsState: MeenaUserSettings,
    onUpdate: ((MeenaUserSettings) -> MeenaUserSettings) -> Unit
) {
    val context = LocalContext.current
    val hasPerm = my.hana.meena.data.system.DeviceCalendarRepository.hasCalendarPermission(context)
    val availableCalendars = remember(hasPerm) {
        if (hasPerm) my.hana.meena.data.system.DeviceCalendarRepository.getAvailableCalendars(context)
        else emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "meena reads your agenda directly from your android system calendar. no google login or cloud sync needed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )

        if (!hasPerm) {
            Text(
                text = "calendar permission is needed to show your upcoming agenda on the today hub.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Button(
                onClick = {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("grant calendar permission in settings", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        } else {
            Column {
                Text("agenda lookahead", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1 to "1 day", 3 to "3 days", 7 to "7 days", 14 to "14 days").forEach { (days, label) ->
                        val isSelected = settingsState.calendarDaysAhead == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                .clickable { onUpdate { it.copy(calendarDaysAhead = days) } }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else MeenaTextMuted
                            )
                        }
                    }
                }
            }

            SettingToggleRow(
                title = "all-day events",
                subtitle = "include all-day items and holidays in agenda widgets",
                checked = settingsState.calendarShowAllDay,
                onCheckedChange = { checked -> onUpdate { it.copy(calendarShowAllDay = checked) } }
            )

            Column {
                Text("synced calendars", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                if (availableCalendars.isEmpty()) {
                    Text(
                        text = "no local calendars found on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        availableCalendars.forEach { cal ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val swatchColor = if (cal.color != 0) Color(cal.color) else MaterialTheme.colorScheme.primary
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(swatchColor)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cal.displayName.lowercase(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    if (cal.accountName.isNotBlank()) {
                                        Text(
                                            text = cal.accountName.lowercase(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MeenaTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).setData(android.provider.CalendarContract.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface)
            ) {
                Text("open system calendar app", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}

@Composable
private fun NewsFeedManager(
    currentFeeds: List<String>,
    onUpdateFeeds: (List<String>) -> Unit
) {
    var newFeedUrl by remember { mutableStateOf("") }
    val defaultPresetFeeds = listOf(
        "bernama" to "https://news.google.com/rss/search?q=bernama&hl=en-MY&gl=MY&ceid=MY:en",
        "the star" to "https://www.thestar.com.my/rss/news/nation",
        "malaysiakini" to "https://www.malaysiakini.com/rss/en/news.rss",
        "techcrunch" to "https://techcrunch.com/feed/",
        "hacker news" to "https://news.ycombinator.com/rss"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "manage feed sources",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "articles are fetched directly on-device with zero server intermediaries or cloud telemetry.",
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted
        )

        // Custom feed text field
        OutlinedTextField(
            value = newFeedUrl,
            onValueChange = { newFeedUrl = it },
            placeholder = { Text("https://example.com/rss.xml", color = MeenaTextMuted, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MeenaSurface,
                unfocusedContainerColor = MeenaSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MeenaBorder
            )
        )

        Button(
            onClick = {
                val trimmed = newFeedUrl.trim()
                if (trimmed.isNotBlank() && !currentFeeds.contains(trimmed)) {
                    val updated = currentFeeds + trimmed
                    onUpdateFeeds(updated)
                    newFeedUrl = ""
                }
            },
            enabled = newFeedUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("add feed source", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }

        // Quick presets
        Text(
            text = "quick presets",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            defaultPresetFeeds.take(3).forEach { (label, url) ->
                val alreadyAdded = currentFeeds.contains(url)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (alreadyAdded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MeenaSurface)
                        .clickable(enabled = !alreadyAdded) {
                            onUpdateFeeds(currentFeeds + url)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (alreadyAdded) "added • $label" else "+ $label",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = if (alreadyAdded) Color.White else MeenaTextMuted
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            defaultPresetFeeds.drop(3).forEach { (label, url) ->
                val alreadyAdded = currentFeeds.contains(url)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (alreadyAdded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MeenaSurface)
                        .clickable(enabled = !alreadyAdded) {
                            onUpdateFeeds(currentFeeds + url)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (alreadyAdded) "added • $label" else "+ $label",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = if (alreadyAdded) Color.White else MeenaTextMuted
                    )
                }
            }
        }

        // Configured feeds
        if (currentFeeds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${currentFeeds.size} configured feeds",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                currentFeeds.forEach { feedUrl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MeenaSurface)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feedUrl.lowercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "remove",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.clickable {
                                onUpdateFeeds(currentFeeds.filter { it != feedUrl })
                            }
                        )
                    }
                }
            }
        } else {
            Text(
                text = "using default feeds (amanz & suami sihat). add custom feeds above to expand your headlines.",
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted
            )
        }
    }
}

@Composable
fun MetroSubPageHeader(
    parentTitle: String,
    pageTitle: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 6.dp)
        ) {
            Text(
                text = "‹ $parentTitle".lowercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = pageTitle.lowercase(),
            style = MaterialTheme.typography.displayMedium,
            fontSize = 30.sp,
            color = Color.White
        )
    }
}

@Composable
private fun GeminiApiKeyInput() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var keyText by remember { mutableStateOf(my.hana.meena.data.system.KeystoreSecretsManager.getGeminiApiKey(context)) }
    var statusMessage by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("api key", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = keyText,
            onValueChange = {
                keyText = it
                statusMessage = ""
            },
            placeholder = { Text("paste your gemini key here...", color = MeenaTextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MeenaSurface,
                unfocusedContainerColor = MeenaSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MeenaBorder
            )
        )
        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.labelSmall,
                color = if (statusMessage.contains("valid") || statusMessage.contains("securely")) MaterialTheme.colorScheme.primary else Color(0xFFE51400),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    my.hana.meena.data.system.KeystoreSecretsManager.saveGeminiApiKey(context, keyText.trim())
                    statusMessage = "key saved securely in keystore"
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("save key", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }

            if (keyText.isNotBlank()) {
                Button(
                    onClick = {
                        isTesting = true
                        statusMessage = "testing key..."
                        coroutineScope.launch {
                            val res = my.hana.meena.data.system.GeminiBriefRepository.testApiKeyAndFetchModels(keyText.trim())
                            isTesting = false
                            statusMessage = if (res.isSuccess) {
                                val count = res.getOrNull()?.size ?: 0
                                "key is valid ($count models ready)"
                            } else {
                                "test failed: ${res.exceptionOrNull()?.message ?: "network error"}"
                            }
                        }
                    },
                    enabled = !isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(if (isTesting) "testing..." else "test key", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }

                Button(
                    onClick = {
                        keyText = ""
                        my.hana.meena.data.system.KeystoreSecretsManager.clearGeminiApiKey(context)
                        statusMessage = "key forgotten"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("forget key", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SolatZonePicker(
    currentZone: String,
    onSelectZone: (String) -> Unit
) {
    val zones = listOf(
        "" to "auto-detect by location (gps)",
        "WLY01" to "WLY01 • Kuala Lumpur & Putrajaya",
        "SGR01" to "SGR01 • Gombak, Petaling, Sepang, Hulu Langat",
        "SGR02" to "SGR02 • Kuala Selangor, Sabak Bernam",
        "SGR03" to "SGR03 • Klang, Kuala Langat",
        "JHR01" to "JHR01 • Pulau Aur & Pemanggil",
        "JHR02" to "JHR02 • Johor Bahru, Kota Tinggi, Kulai",
        "PNG01" to "PNG01 • Pulau Pinang",
        "PRK01" to "PRK01 • Tapah, Slim River, Tanjung Malim",
        "PRK02" to "PRK02 • Ipoh, Batu Gajah, Kampar",
        "SBH01" to "SBH01 • Kota Kinabalu, Ranau, Penampang",
        "SWK01" to "SWK01 • Kuching, Bau, Lundu, Sematan"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        zones.forEach { (code, label) ->
            val isSelected = currentZone.equals(code, ignoreCase = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                    .clickable { onSelectZone(code) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = label.lowercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) Color.White else MeenaTextMuted
                )
            }
        }
    }
}

@Composable
private fun DateTimeSettings(
    settingsState: MeenaUserSettings,
    onUpdate: ((MeenaUserSettings) -> MeenaUserSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "customize clock display, date formats and islamic hijri calendar offset.",
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )

        SettingToggleRow(
            title = "24-hour clock",
            subtitle = "display time in 24-hour format (e.g. 14:00 vs 2:00 pm)",
            checked = settingsState.is24HourFormat,
            onCheckedChange = { onUpdate { it.copy(is24HourFormat = it.is24HourFormat.not()) } }
        )

        SettingToggleRow(
            title = "show seconds",
            subtitle = "display live seconds on the main clock",
            checked = settingsState.showClockSeconds,
            onCheckedChange = { onUpdate { it.copy(showClockSeconds = it.showClockSeconds.not()) } }
        )

        Column {
            Text("date pattern", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("live preview using today's date", style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val patterns = listOf(
                "EEEE, MMMM d",
                "EEE, d MMM yyyy",
                "d MMMM yyyy",
                "yyyy-MM-dd",
                "d MMM"
            )
            val today = java.util.Date()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                patterns.forEach { pattern ->
                    val isSelected = settingsState.dateFormatPattern == pattern
                    val preview = try {
                        java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(today).lowercase()
                    } catch (_: Exception) {
                        pattern
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                            .clickable { onUpdate { it.copy(dateFormatPattern = pattern) } }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = pattern,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MeenaTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        SettingToggleRow(
            title = "hijri date",
            subtitle = "show islamic hijri date beneath the clock",
            checked = settingsState.showHijriDate,
            onCheckedChange = { onUpdate { it.copy(showHijriDate = it.showHijriDate.not()) } }
        )

        if (settingsState.showHijriDate) {
            Column {
                Text("hijri day adjustment", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("compensate for moon sighting differences", style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-2 to "-2d", -1 to "-1d", 0 to "exact", 1 to "+1d", 2 to "+2d").forEach { (offset, label) ->
                        val isSelected = settingsState.hijriDayOffset == offset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                .clickable { onUpdate { it.copy(hijriDayOffset = offset) } }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else MeenaTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

