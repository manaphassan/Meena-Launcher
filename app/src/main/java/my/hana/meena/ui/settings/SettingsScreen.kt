package my.hana.meena.ui.settings

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.R
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.model.WidgetConfig
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
                            "widget-clock-weather" to "clock & weather",
                            "widget-weather-forecast" to "weather forecast",
                            "widget-solat" to "prayer times (solat)",
                            "widget-calendar-month" to "calendar grid",
                            "widget-tasks" to "schedule & agenda",
                            "widget-today-summary" to "brief (gemini flash)",
                            "widget-notes" to "quick notes",
                            "widget-exchange-rates" to "exchange rates",
                            "widget-finance-charts" to "financial charts",
                            "widget-spending-summary" to "spending tracker",
                            "widget-notifications" to "alerts & notifications",
                            "widget-conversations" to "conversations",
                            "widget-news-feed" to "news feed",
                            "widget-device-telemetry" to "phone status",
                            "widget-map-radar" to "map radar",
                            "widget-health-device" to "connected device",
                            "widget-health-telemetry" to "fit telemetry",
                            "widget-health-aqi" to "air quality index"
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
                            val geminiStatus = if (settingsState.geminiApiKey.isNotBlank()) "api key configured" else "needs key"
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
                                title = "notification access",
                                status = "interception for alerts & spending",
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
                            val count = settingsState.newsFeeds.size
                            val newsStatus = if (count > 0) "$count feeds saved" else "default feeds"
                            SettingLinkRow(
                                title = "news feeds",
                                status = newsStatus,
                                onClick = { activeSubPage = "account:news" }
                            )
                        }

                        item {
                            SettingLinkRow(
                                title = "health connect",
                                status = "device telemetry & metrics",
                                onClick = { activeSubPage = "account:health" }
                            )
                        }
                    }

                    "look & feel" -> {
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

            // Widget-Specific Extended Settings
            when (widgetId) {
                "widget-today-summary" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("brief specific options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        GeminiApiKeyInput(
                            currentKey = settingsState.geminiApiKey,
                            onSaveKey = { key -> onUpdate { it.copy(geminiApiKey = key) } }
                        )
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
                }

                "widget-map-radar" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("radar map options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        Text("radar sweep perimeter: 3.0 km geodesic radius with dark matter tiles", style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted)
                    }
                }

                "widget-spending-summary" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("bank notification interception", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        Text(
                            "detects banking notifications on-device only (maybank, cimb, tng, grabpay).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MeenaTextMuted
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
                GeminiApiKeyInput(
                    currentKey = settingsState.geminiApiKey,
                    onSaveKey = { key -> onUpdate { it.copy(geminiApiKey = key) } }
                )
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
                Text(
                    text = "quotes are sourced from the google sheets bridge or licensed free quote providers. no synthetic prices or fake balances are ever used.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
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

        // Dedicated Look & Feel Sub-Pages
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
        Column(modifier = Modifier.weight(1f)) {
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
        Text(
            text = "›",
            fontSize = 20.sp,
            color = MeenaTextMuted,
            modifier = Modifier.padding(start = 8.dp)
        )
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
private fun GeminiApiKeyInput(
    currentKey: String,
    onSaveKey: (String) -> Unit
) {
    var keyText by remember { mutableStateOf(currentKey) }
    var isSaved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("api key", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = keyText,
            onValueChange = {
                keyText = it
                isSaved = false
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSaveKey(keyText.trim())
                    isSaved = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(if (isSaved) "key saved" else "save key", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
            if (keyText.isNotBlank()) {
                Button(
                    onClick = {
                        keyText = ""
                        onSaveKey("")
                        isSaved = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("clear key", style = MaterialTheme.typography.labelSmall, color = Color.White)
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
