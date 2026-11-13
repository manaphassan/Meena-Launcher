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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.R
import my.hana.meena.ui.components.MetroTurnstileContainer
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.data.finance.FinanceRepository
import my.hana.meena.data.health.HealthConnectAvailability
import my.hana.meena.data.health.HealthRepository
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.model.WidgetConfig
import my.hana.meena.data.news.NewsRssRepository
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.ui.components.MetroDialog
import my.hana.meena.ui.components.MetroListPicker
import my.hana.meena.ui.components.MetroListPickerRow
import my.hana.meena.ui.components.MetroPickerItem
import my.hana.meena.ui.components.MetroToggle
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.theme.Meena10AccentSwatches
import my.hana.meena.theme.isAccessibleContrast
import my.hana.meena.theme.calculateContrastRatio
import my.hana.meena.theme.MeenaLossRed
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaAmber
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaBlue
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaCobalt
import my.hana.meena.theme.MeenaCrimson
import my.hana.meena.theme.MeenaCyan
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaSolarAmber
import my.hana.meena.theme.MeenaDarkSlate
import my.hana.meena.theme.MeenaEmerald
import my.hana.meena.theme.MeenaLime
import my.hana.meena.theme.MeenaMagenta
import my.hana.meena.theme.MeenaOrange
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite

/**
 * Meena UI settings, laid out like a Zune HD pivot: pages, connections, personalize, system.
 * Rules:
 * - Pivots hold link lists only (no switches, sliders or scrollers); toggles live on sub-pages.
 * - Groups are small accent labels, rows are large light titles with a grey status line.
 * - Everything lowercase, square corners, no cards or boxes, 24dp margins.
 */
@Composable
fun SettingsScreen(
    currentSettings: MeenaUserSettings,
    initialTab: String = SETTINGS_TAB_PAGES,
    initialSubPage: String? = null,
    onSaveSettings: (MeenaUserSettings) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onResetDefaults: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var settingsState by remember { mutableStateOf(currentSettings) }
    var activeSubPage by remember(initialSubPage) { mutableStateOf(initialSubPage) }

    val tabs = SETTINGS_TABS
    val tabLabels = listOf(
        stringResource(R.string.settings_tab_pages),
        stringResource(R.string.settings_tab_connections),
        stringResource(R.string.settings_tab_personalize),
        stringResource(R.string.settings_tab_system)
    )
    val pagerState = rememberPagerState(initialPage = tabs.indexOf(normalizeSettingsTab(initialTab)).coerceAtLeast(0)) { tabs.size }
    val scope = rememberCoroutineScope()

    // Back: leave a sub-page first, otherwise close settings
    BackHandler {
        if (activeSubPage != null) activeSubPage = null else onClose()
    }

    // Windows Phone turnstile entrance (respects reduced motion)
    var turnstileRotation by remember { mutableFloatStateOf(if (currentSettings.isReducedMotion) 0f else 20f) }
    var turnstileAlpha by remember { mutableFloatStateOf(if (currentSettings.isReducedMotion) 1f else 0f) }

    LaunchedEffect(currentSettings.isReducedMotion) {
        if (!currentSettings.isReducedMotion) {
            animate(
                initialValue = 20f,
                targetValue = 0f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) { value, _ -> turnstileRotation = value }
        }
    }

    LaunchedEffect(currentSettings.isReducedMotion) {
        if (!currentSettings.isReducedMotion) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180, easing = LinearEasing)
            ) { value, _ -> turnstileAlpha = value }
        }
    }

    fun update(transform: (MeenaUserSettings) -> MeenaUserSettings) {
        val updated = transform(settingsState)
        settingsState = updated
        onSaveSettings(updated)
    }

    var confirmStoryOrder by remember { mutableStateOf(false) }
    if (confirmStoryOrder) {
        MetroDialog(
            title = stringResource(R.string.settings_story_title),
            message = stringResource(R.string.settings_story_question),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
            onConfirm = {
                update { it.withStoryOrder() }
                confirmStoryOrder = false
            },
            onDismiss = { confirmStoryOrder = false }
        )
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
        // Small caption bar, Zune HD style: back target on the left, nothing else competing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .metroClickable { if (activeSubPage != null) activeSubPage = null else onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.settings_back),
                    tint = MeenaTextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = stringResource(R.string.settings_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextMuted
            )
        }

        MetroTurnstileContainer(
            targetState = activeSubPage,
            modifier = Modifier.fillMaxSize(),
            isForward = activeSubPage != null,
            reducedMotion = settingsState.reducedMotion
        ) { currentSubPage ->
            if (currentSubPage != null) {
                SubPageContainer(
                    subPageId = currentSubPage,
                    settingsState = settingsState,
                    onUpdate = { transform -> update(transform) },
                    onBack = { activeSubPage = null },
                    onOpenSubPage = { activeSubPage = it },
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onResetDefaults = onResetDefaults
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Pivot headers: the current one first in white, the rest trail off the edge in grey
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .padding(start = my.hana.meena.theme.MeenaSpacing.l)
                    ) {
                        Row(
                            modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                            horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.l)
                        ) {
                            val current = pagerState.currentPage
                            for (offset in tabs.indices) {
                                val index = (current + offset) % tabs.size
                                Text(
                                    text = tabLabels[index],
                                    style = MaterialTheme.typography.displayLarge,
                                    color = if (offset == 0) MeenaTextWhite else MeenaTextMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.metroClickable {
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    }
                                )
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            when (tabs[page]) {
                                SETTINGS_TAB_PAGES -> pagesTab(settingsState, onSortStory = { confirmStoryOrder = true }) { activeSubPage = it }
                                SETTINGS_TAB_CONNECTIONS -> connectionsTab(settingsState, context) { activeSubPage = it }
                                SETTINGS_TAB_PERSONALIZE -> personalizeTab(settingsState) { activeSubPage = it }
                                SETTINGS_TAB_SYSTEM -> systemTab(context) { activeSubPage = it }
                            }
                            item { Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.l)) }
                        }
                    }
                }
            }
        }
    }
}

const val SETTINGS_TAB_PAGES = "pages"
const val SETTINGS_TAB_CONNECTIONS = "connections"
const val SETTINGS_TAB_PERSONALIZE = "personalize"
const val SETTINGS_TAB_SYSTEM = "system"
private val SETTINGS_TABS = listOf(SETTINGS_TAB_PAGES, SETTINGS_TAB_CONNECTIONS, SETTINGS_TAB_PERSONALIZE, SETTINGS_TAB_SYSTEM)

/** Old callers still pass "widgets", "accounts" or "look & feel"; map them to the new pivots. */
fun normalizeSettingsTab(tab: String): String = when (tab) {
    "widgets" -> SETTINGS_TAB_PAGES
    "accounts" -> SETTINGS_TAB_CONNECTIONS
    "look & feel" -> SETTINGS_TAB_PERSONALIZE
    in SETTINGS_TABS -> tab
    else -> SETTINGS_TAB_PAGES
}

/** Widget id to the name used on its row and sub-page. */
internal val SETTINGS_WIDGET_TITLES = linkedMapOf(
    "widget-clock-weather" to "clock",
    "widget-weather-forecast" to "forecast and air",
    "widget-tasks" to "your week",
    "widget-solat" to "prayer times",
    "widget-favourites" to "favourites",
    "widget-frequent-apps" to "frequent apps",
    "widget-notes" to "notes",
    "widget-calendar-month" to "month",
    "widget-health-telemetry" to "health",
    "widget-bluetooth-devices" to "connected devices",
    "widget-device-telemetry" to "phone",
    "widget-map-radar" to "nearby",
    "widget-wikipedia" to "wikipedia",
    "widget-conversations" to "chats",
    "widget-notifications" to "alerts",
    "widget-news-feed" to "news feed",
    "widget-finance-charts" to "portfolio",
    "widget-exchange-rates" to "rates",
    "widget-spending-summary" to "spending"
)

/**
 * Groups widgets by the page they are on (so a moved widget shows under its new page), in page
 * order, each group sorted by position. Widgets without a known title are skipped.
 */
internal fun groupWidgetsByPage(configs: Map<String, WidgetConfig>): List<Pair<String, List<String>>> {
    val pageOrder = listOf("today", "inbox", "news", "money")
    return configs.entries
        .filter { it.key in SETTINGS_WIDGET_TITLES }
        .groupBy { it.value.hubId }
        .toList()
        .sortedBy { (hub, _) -> pageOrder.indexOf(hub).let { if (it < 0) pageOrder.size else it } }
        .map { (hub, entries) -> hub to entries.sortedBy { it.value.position }.map { it.key } }
}

private fun LazyListScope.settingsGroupLabel(labelRes: Int) {
    item {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                top = my.hana.meena.theme.MeenaSpacing.l,
                bottom = my.hana.meena.theme.MeenaSpacing.s
            )
        )
    }
}

private fun LazyListScope.pagesTab(settingsState: MeenaUserSettings, onSortStory: () -> Unit, open: (String) -> Unit) {
    item {
        Text(
            text = stringResource(R.string.settings_pages_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted,
            modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.m)
        )
    }
    item {
        SettingsPivotRow(
            title = stringResource(R.string.settings_story_title),
            status = stringResource(R.string.settings_story_status),
            onClick = onSortStory
        )
    }
    groupWidgetsByPage(settingsState.widgetConfigs).forEach { (hub, ids) ->
        item(key = "page-$hub") {
            val label = when (hub) {
                "today" -> stringResource(R.string.settings_page_today)
                "inbox" -> stringResource(R.string.settings_page_inbox)
                "news" -> stringResource(R.string.settings_page_news)
                "money" -> stringResource(R.string.settings_page_money)
                else -> hub
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    top = my.hana.meena.theme.MeenaSpacing.l,
                    bottom = my.hana.meena.theme.MeenaSpacing.s
                )
            )
        }
        ids.forEach { id ->
            item(key = id) {
                val isOn = settingsState.widgetConfigs[id]?.isEnabled != false
                SettingsPivotRow(
                    title = SETTINGS_WIDGET_TITLES.getValue(id),
                    status = stringResource(if (isOn) R.string.settings_widget_on else R.string.settings_widget_off),
                    isDimmed = !isOn,
                    onClick = { open("widget:$id") }
                )
            }
        }
    }
}

private fun LazyListScope.connectionsTab(settingsState: MeenaUserSettings, context: android.content.Context, open: (String) -> Unit) {
    settingsGroupLabel(R.string.settings_group_where)
    item {
        SettingsPivotRow(
            title = stringResource(R.string.settings_solat_title),
            status = settingsState.solatZone.ifBlank { stringResource(R.string.settings_solat_auto) }.lowercase(),
            onClick = { open("account:solat") }
        )
    }
    settingsGroupLabel(R.string.settings_group_your_day)
    item {
        val hasCalendar = my.hana.meena.data.system.DeviceCalendarRepository.hasCalendarPermission(context)
        SettingsPivotRow(
            title = stringResource(R.string.settings_calendar_title),
            status = if (hasCalendar) stringResource(R.string.settings_calendar_on, settingsState.calendarDaysAhead) else stringResource(R.string.settings_needs_okay),
            onClick = { open("account:calendar") }
        )
    }
    item {
        val hasAccess = NotificationRepository.isNotificationAccessGranted(context)
        SettingsPivotRow(
            title = stringResource(R.string.settings_notifications_title),
            status = if (hasAccess) stringResource(R.string.settings_notifications_on) else stringResource(R.string.settings_needs_okay),
            onClick = { open("account:notifications") }
        )
    }
    item {
        SettingsPivotRow(
            title = stringResource(R.string.settings_health_title),
            status = stringResource(R.string.settings_health_status),
            onClick = { open("account:health") }
        )
    }
    settingsGroupLabel(R.string.settings_group_reading_money)
    item {
        val count = settingsState.newsFeeds.size
        SettingsPivotRow(
            title = stringResource(R.string.settings_news_title),
            status = if (count > 0) stringResource(R.string.settings_news_status, count) else stringResource(R.string.settings_news_default),
            onClick = { open("account:news") }
        )
    }
    item {
        SettingsPivotRow(
            title = stringResource(R.string.settings_money_title),
            status = stringResource(R.string.settings_money_status),
            onClick = { open("account:finance") }
        )
    }
}

private fun LazyListScope.personalizeTab(settingsState: MeenaUserSettings, open: (String) -> Unit) {
    settingsGroupLabel(R.string.settings_group_look)
    item {
        SettingsPivotRow(
            title = stringResource(R.string.settings_theme_title),
            status = "${settingsState.baseTheme} · ${settingsState.accentHex.lowercase()}",
            onClick = { open("look:theme") }
        )
    }
    item {
        SettingsPivotRow(
            title = stringResource(R.string.settings_text_title),
            status = stringResource(R.string.settings_text_status, settingsState.fontScalePercent),
            onClick = { open("look:typography") }
        )
    }
    item {
        val context = LocalContext.current
        val packLabel by androidx.compose.runtime.produceState<String?>(null, settingsState.iconPack) {
            value = my.hana.meena.data.system.IconPackRepository.labelFor(context, settingsState.iconPack)
        }
        SettingsPivotRow(
            title = stringResource(R.string.settings_icon_pack_title),
            status = packLabel ?: stringResource(R.string.settings_icon_pack_system),
            onClick = { open("look:iconpack") }
        )
    }
    settingsGroupLabel(R.string.settings_group_feel)
    item {
        val timeMode = if (settingsState.is24HourFormat) "24-hour" else "12-hour"
        val hijriMode = if (settingsState.showHijriDate) "hijri on" else "hijri off"
        SettingsPivotRow(
            title = stringResource(R.string.settings_datetime_title),
            status = "$timeMode · $hijriMode",
            onClick = { open("look:datetime") }
        )
    }
    item {
        val parts = buildList {
            add(stringResource(if (settingsState.isLeftHanded) R.string.settings_hand_left else R.string.settings_hand_right))
            add(stringResource(if (settingsState.isHapticEnabled) R.string.settings_haptics_on else R.string.settings_haptics_off))
            if (settingsState.isReducedMotion) add(stringResource(R.string.settings_motion_reduced))
        }
        SettingsPivotRow(
            title = stringResource(R.string.settings_hand_title),
            status = parts.joinToString(" · "),
            onClick = { open("look:ergonomics") }
        )
    }
}

private fun LazyListScope.systemTab(context: android.content.Context, open: (String) -> Unit) {
    settingsGroupLabel(R.string.settings_group_your_data)
    item {
        SettingsPivotRow(
            title = stringResource(R.string.settings_privacy_title),
            status = stringResource(R.string.privacy_status),
            onClick = { open("look:privacy") }
        )
    }
    item {
        SettingsPivotRow(
            title = stringResource(R.string.settings_backup_title),
            status = stringResource(R.string.settings_backup_status),
            onClick = { open("look:backup") }
        )
    }
    settingsGroupLabel(R.string.settings_group_meena)
    item {
        val version = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
        }
        SettingsPivotRow(
            title = stringResource(R.string.settings_about_title),
            status = stringResource(R.string.settings_about_status, version),
            onClick = { open("look:about") }
        )
    }
}

/** Zune HD style list row: large light title, small grey status underneath, nothing boxed. */
@Composable
private fun SettingsPivotRow(
    title: String,
    status: String,
    isDimmed: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
    ) {
        Text(
            text = title.lowercase(),
            style = MaterialTheme.typography.titleLarge,
            color = if (isDimmed) MeenaTextMuted else MeenaTextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = status.lowercase(),
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    onOpenSubPage: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onResetDefaults: () -> Unit
) {
    val context = LocalContext.current
    var showNotificationDisclosure by remember { mutableStateOf(false) }

    if (showNotificationDisclosure) {
        MetroDialog(
            title = "notification access",
            message = "meena reads incoming notifications solely on your phone to show alerts and chats. your messages and notifications are processed locally in memory and never sent to any server, cloud, or third party.\n\ndo you want to open notification access settings?",
            confirmText = "yes",
            dismissText = "no",
            onConfirm = {
                showNotificationDisclosure = false
                NotificationRepository.openNotificationSettings(context)
            },
            onDismiss = { showNotificationDisclosure = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l, vertical = my.hana.meena.theme.MeenaSpacing.s),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
    ) {
        when {
            subPageId.startsWith("widget:") -> {
                val widgetId = subPageId.removePrefix("widget:")
                val config = settingsState.widgetConfigs[widgetId] ?: WidgetConfig("today", 0)

            item {
                MetroSubPageHeader(
                    parentTitle = "pages",
                    pageTitle = SETTINGS_WIDGET_TITLES[widgetId] ?: widgetId.replace("widget-", "").replace("-", " "),
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
                    Text("destination page", style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
                    Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.s))
                    val hubs = listOf("today", "inbox", "news", "money", "apps")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                    ) {
                        hubs.forEach { hub ->
                            val isSelected = config.hubId == hub
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                    .metroClickable {
                                        val newMap = settingsState.widgetConfigs.toMutableMap()
                                        newMap[widgetId] = config.copy(hubId = hub)
                                        onUpdate { it.copy(widgetConfigs = newMap) }
                                    }
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = hub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MeenaTextWhite else MeenaTextMuted
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
                    Text("refresh cadence", style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
                    Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.s))
                    val intervals = listOf(15 to "15m", 30 to "30m", 60 to "1h", 0 to "manual")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                    ) {
                        intervals.forEach { (mins, label) ->
                            val isSelected = config.refreshMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                    .metroClickable {
                                        val newMap = settingsState.widgetConfigs.toMutableMap()
                                        newMap[widgetId] = config.copy(refreshMinutes = mins)
                                        onUpdate { it.copy(widgetConfigs = newMap) }
                                    }
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MeenaTextWhite else MeenaTextMuted
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
                        SolatZonePicker(
                            currentZone = settingsState.solatZone,
                            onSelectZone = { zone -> onUpdate { it.copy(solatZone = zone) } }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = stringResource(R.string.solat_sun_path),
                            subtitle = stringResource(R.string.solat_sun_path_hint),
                            checked = custom("show_celestial_arc", "false") == "true",
                            onCheckedChange = { setCustom("show_celestial_arc", it.toString()) }
                        )
                    }
                    item {
                        SettingToggleRow(
                            title = stringResource(R.string.solat_show_syuruk),
                            subtitle = stringResource(R.string.solat_show_syuruk_hint),
                            checked = custom("show_syuruk_imsak", "true") == "true",
                            onCheckedChange = { setCustom("show_syuruk_imsak", it.toString()) }
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
                        SettingToggleRow(
                            title = stringResource(R.string.agenda_show_strip),
                            subtitle = stringResource(R.string.agenda_show_strip_hint),
                            checked = custom("show_week_strip", "true") == "true",
                            onCheckedChange = { setCustom("show_week_strip", it.toString()) }
                        )
                    }
                    item {
                        SettingsPivotRow(
                            title = stringResource(R.string.settings_calendar_title),
                            status = stringResource(R.string.agenda_calendar_link),
                            onClick = { onOpenSubPage("account:calendar") }
                        )
                    }
                }

                "widget-frequent-apps" -> {
                    item {
                        val settingsContext = LocalContext.current
                        var confirmClear by remember { mutableStateOf(false) }
                        if (confirmClear) {
                            MetroDialog(
                                title = stringResource(R.string.dialog_clear_app_history_title),
                                message = stringResource(R.string.dialog_clear_app_history_question),
                                confirmText = stringResource(R.string.action_yes),
                                dismissText = stringResource(R.string.action_no),
                                onConfirm = {
                                    my.hana.meena.data.system.AppLaunchStats.clear(settingsContext)
                                    confirmClear = false
                                },
                                onDismiss = { confirmClear = false }
                            )
                        }
                        SettingLinkRow(
                            title = stringResource(R.string.frequent_apps_clear),
                            status = stringResource(R.string.frequent_apps_clear_hint),
                            onClick = { confirmClear = true }
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
                            onClick = { showNotificationDisclosure = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("open notification listener settings", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
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
                            onClick = { showNotificationDisclosure = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("open notification listener settings", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
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
                            onClick = { showNotificationDisclosure = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("open notification listener settings", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                        }
                    }
                }

                "widget-news-feed" -> {
                    item {
                        SettingsPivotRow(
                            title = stringResource(R.string.settings_news_title),
                            status = stringResource(R.string.news_feeds_link_status),
                            onClick = { onOpenSubPage("account:news") }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("display options", style = MaterialTheme.typography.titleSmall, color = MeenaTextWhite)
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
                            title = "memory usage",
                            subtitle = "display active and free memory",
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
                            Text("open bluetooth settings", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                        }
                    }
                }

                "widget-health-telemetry" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("health options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
                            Text("open health connect settings", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                        }
                    }
                }
            }
        }

        // Dedicated Account Sub-Pages
        subPageId == "account:solat" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "connections",
                    pageTitle = "prayer times",
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
                    parentTitle = "connections",
                    pageTitle = "notifications",
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
                    onClick = { showNotificationDisclosure = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("open notification access settings", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                }
            }
        }

        subPageId == "account:finance" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "connections",
                    pageTitle = "money",
                    onBack = onBack
                )
            }
            item {
                FinanceAccountSettings(
                    settingsState = settingsState,
                    onUpdate = onUpdate
                )
            }
        }

        subPageId == "account:news" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "connections",
                    pageTitle = "news feeds",
                    onBack = onBack
                )
            }
            item {
                NewsFeedManager(
                    currentFeeds = settingsState.newsFeeds,
                    onUpdateFeeds = { feeds ->
                        // Saving exactly the built-in list keeps "using built-in feeds" behaviour.
                        val toSave = if (feeds == NewsRssRepository.DEFAULT_FEEDS) emptyList() else feeds
                        onUpdate { it.copy(newsFeeds = toSave) }
                        NewsRssRepository.updateFeeds(toSave)
                    }
                )
            }
        }

        subPageId == "account:health" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "connections",
                    pageTitle = "health",
                    onBack = onBack
                )
            }
            item {
                HealthAccountSettings(
                    settingsState = settingsState,
                    onUpdate = onUpdate
                )
            }
        }

        subPageId == "account:calendar" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "connections",
                    pageTitle = "calendar",
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
                    parentTitle = "personalize",
                    pageTitle = "date and time",
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
                    parentTitle = "personalize",
                    pageTitle = "theme and accent",
                    onBack = onBack
                )
            }
            item {
                SettingSegmentedRow(
                    title = "base theme",
                    subtitle = "choose surface background contrast",
                    options = listOf(
                        "dark" to "dark (pure black)",
                        "light" to "light (high key)"
                    ),
                    selectedKey = settingsState.baseTheme,
                    onSelectKey = { theme -> onUpdate { it.copy(baseTheme = theme) } }
                )
            }

            item {
                Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.m))
                Text(
                    text = "lumia accent color",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))

                val currentBg = when (settingsState.baseTheme) {
                    "light" -> MeenaTextWhite
                    else -> MeenaBlack
                }
                val currentAccentColor = runCatching {
                    Color(settingsState.accentHex.toColorInt())
                }.getOrDefault(MeenaBlue)
                val currentRatio = calculateContrastRatio(currentAccentColor, currentBg)
                val isAccessible = isAccessibleContrast(currentAccentColor, currentBg)
                val activeSwatch = Meena10AccentSwatches.find { it.hex.equals(settingsState.accentHex, ignoreCase = true) }
                val swatchName = activeSwatch?.name ?: settingsState.accentHex.lowercase()

                Text(
                    text = "$swatchName · ${String.format(java.util.Locale.US, "%.1f", currentRatio)}:1 contrast against ${settingsState.baseTheme} · ${if (isAccessible) "wcag aa compliant" else "low contrast"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAccessible) MeenaTextSecondary else MeenaLossRed
                )
                Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.m))

                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                    Meena10AccentSwatches.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                        ) {
                            row.forEach { swatch ->
                                val isSelected = settingsState.accentHex.equals(swatch.hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .aspectRatio(1f)
                                        .background(swatch.color)
                                        .graphicsLayer { scaleX = if (isSelected) 1f else 0.7f; scaleY = if (isSelected) 1f else 0.7f }
                                        .metroClickable { onUpdate { it.copy(accentHex = swatch.hex) } }
                                )
                            }
                        }
                    }
                }
            }


        }

        subPageId == "look:typography" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "personalize",
                    pageTitle = "text size",
                    onBack = onBack
                )
            }


            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("font scale", style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
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
                    parentTitle = "personalize",
                    pageTitle = "hand and touch",
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
                    title = "reduced motion",
                    subtitle = "subdued animations for transitions and turnstile",
                    checked = settingsState.isReducedMotion,
                    onCheckedChange = { onUpdate { it.copy(isReducedMotion = it.isReducedMotion.not()) } }
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
                    subtitle = "hide top battery and clock for a clean screen",
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

        subPageId == "look:iconpack" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_personalize),
                    pageTitle = stringResource(R.string.settings_icon_pack_title),
                    onBack = onBack
                )
            }
            item {
                IconPackSettings(
                    settingsState = settingsState,
                    onUpdate = onUpdate
                )
            }
        }

        subPageId == "look:backup" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "system",
                    pageTitle = "backup and restore",
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
                    Text("export settings json", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                }
            }
            item {
                Button(
                    onClick = onImportBackup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("import settings json", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                }
            }
            item {
                Button(
                    onClick = onResetDefaults,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("reset to defaults", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        subPageId == "look:about" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "system",
                    pageTitle = "about meena ui",
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = "meena is a personal android launcher built with jetpack compose, designed around clean typography, generous whitespace, and pure speed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextWhite
                )
            }
            item {
                Text(
                    text = "charter: clean · light · open · fast",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "inspired by windows phone metro ui and aio launcher. no boxes, no cards, no elevation. hierarchy comes from type and space alone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "privacy & data safety",
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                    Text(
                        text = stringResource(R.string.privacy_notifications_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                    Text(
                        text = stringResource(R.string.privacy_location_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                    Text(
                        text = stringResource(R.string.privacy_calendar_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                    Text(
                        text = "secure storage: api keys and sensitive settings are kept locally with android keystore encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                    Text(
                        text = stringResource(R.string.privacy_backup_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "open source licenses",
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "built using kotlin, jetpack compose, kotlinx serialization, datastore preferences, and okhttp. license information is available under apache 2.0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "version 1.0 · my.hana.meena",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        subPageId == "look:privacy" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = "system",
                    pageTitle = "privacy",
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = stringResource(R.string.privacy_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextWhite
                )
            }
            item {
                Text(
                    text = "on-device architecture",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.privacy_on_device_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
            item {
                Text(
                    text = "what meena reads",
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                    Text(
                        text = stringResource(R.string.privacy_location_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                    Text(
                        text = stringResource(R.string.privacy_calendar_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                    Text(
                        text = stringResource(R.string.privacy_notifications_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                    Text(
                        text = "health connect: reads daily step counts, distance, active calories, sleep, and heart rate for display. never shared.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }
            item {
                Text(
                    text = "backup & data safety",
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "android cloud backup is permanently disabled (allowbackup=false) to ensure your data stays exclusively on this device. clearing app data in android settings deletes all local information immediately.",
                    style = MaterialTheme.typography.bodySmall,
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
            .heightIn(min = 52.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title.lowercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MeenaTextWhite
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
            .heightIn(min = 48.dp)
            .metroClickable { onCheckedChange(!checked) }
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = my.hana.meena.theme.MeenaSpacing.m),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = title.lowercase(),
                style = MaterialTheme.typography.bodyLarge,
                color = MeenaTextWhite
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle.lowercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
        }
        MetroToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            showLabel = true
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
    var showPicker by remember { mutableStateOf(false) }
    val currentLabel = options.find { it.first.equals(selectedKey, ignoreCase = true) }?.second ?: selectedKey

    MetroListPickerRow(
        title = title,
        currentValue = currentLabel,
        description = subtitle,
        onClick = { showPicker = true }
    )

    if (showPicker) {
        val pickerItems = remember(options) {
            options.map { (key, label) ->
                MetroPickerItem(key = key, title = label)
            }
        }
        MetroListPicker(
            title = title,
            items = pickerItems,
            selectedKey = selectedKey,
            onSelect = onSelectKey,
            onDismiss = { showPicker = false }
        )
    }
}

/** Icon pack choice: "system icons" first, then installed packs by name, in a full-screen list picker. */
@Composable
private fun IconPackSettings(
    settingsState: MeenaUserSettings,
    onUpdate: ((MeenaUserSettings) -> MeenaUserSettings) -> Unit
) {
    val context = LocalContext.current
    val packs by androidx.compose.runtime.produceState<List<my.hana.meena.data.system.IconPackInfo>?>(null) {
        value = my.hana.meena.data.system.IconPackRepository.installedPacks(context)
    }
    val installed = packs
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
        when {
            installed == null -> Text(
                text = stringResource(R.string.settings_icon_pack_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MeenaTextMuted
            )
            installed.isEmpty() -> Text(
                text = stringResource(R.string.settings_icon_pack_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MeenaTextMuted
            )
            else -> {
                Text(
                    text = stringResource(R.string.settings_icon_pack_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
                val options = listOf("" to stringResource(R.string.settings_icon_pack_system)) +
                    installed.map { it.packageName to it.label }
                // A chosen pack that has since been removed shows as system icons, which is what is drawn.
                val selectedKey = settingsState.iconPack.takeIf { chosen -> installed.any { it.packageName == chosen } } ?: ""
                SettingSegmentedRow(
                    title = stringResource(R.string.settings_icon_pack_title),
                    options = options,
                    selectedKey = selectedKey,
                    onSelectKey = { key -> onUpdate { it.copy(iconPack = key) } }
                )
            }
        }
    }
}

@Composable
private fun FinanceAccountSettings(
    settingsState: MeenaUserSettings,
    onUpdate: ((MeenaUserSettings) -> MeenaUserSettings) -> Unit
) {
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
    var holdingToDelete by remember { mutableStateOf<my.hana.meena.data.finance.FinanceHolding?>(null) }

    if (holdingToDelete != null) {
        val holding = holdingToDelete!!
        MetroDialog(
            title = "delete holding",
            message = "delete this holding (${holding.symbol.lowercase()})?",
            confirmText = "yes",
            dismissText = "no",
            onConfirm = {
                FinanceRepository.removeHolding(context, holding.id)
                holdingToDelete = null
            },
            onDismiss = { holdingToDelete = null }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
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
                focusedTextColor = MeenaTextWhite,
                unfocusedTextColor = MeenaTextWhite,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MeenaBorder
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
        ) {
            Button(
                onClick = {
                    FinanceRepository.setSheetUrl(context, sheetUrlInput)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("save & load sheet", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
            }

            if (sheetUrlInput.isNotBlank()) {
                Button(
                    onClick = {
                        sheetUrlInput = ""
                        FinanceRepository.setSheetUrl(context, "")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("disconnect", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MeenaSurface)
                .padding(my.hana.meena.theme.MeenaSpacing.m),
            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
        ) {
            Text(
                text = "google sheets setup guide",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "1. create a sheet with columns:\n   a: symbol (e.g. klse:bursa, btc, aapl)\n   b: price (=googlefinance(a2, \"price\"))\n   c: change (=googlefinance(a2, \"change\"))\n   d: changepct (=googlefinance(a2, \"changepct\"))\n   e: name\n2. share as 'anyone with link can view'\n3. paste the url above.",
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted
            )
            Text(
                text = "note: google finance quotes can be delayed ~20 min.",
                style = MaterialTheme.typography.labelSmall,
                color = MeenaAmber
            )
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

        SettingToggleRow(
            title = "mask balances in public",
            subtitle = "hide exact portfolio totals and share counts on home and money pages",
            checked = !settingsState.showFinanceAmounts,
            onCheckedChange = { masked ->
                onUpdate { it.copy(showFinanceAmounts = !masked) }
            }
        )

        // Existing Holdings
        val holdings: List<my.hana.meena.data.finance.FinanceHolding> = data?.holdings ?: emptyList()
        if (holdings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                for (holding in holdings) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${holding.symbol.lowercase()} (${holding.groupName.lowercase()})",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MeenaTextWhite
                            )
                            Text(
                                text = "${holding.shares.toInt()} shares · cost rm %.2f · now rm %.2f".format(holding.averageCost, holding.currentPrice),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                        IconButton(
                            onClick = { holdingToDelete = holding }
                        ) {
                            Icon(ImageVector.vectorResource(id = my.hana.meena.R.drawable.ic_close), contentDescription = "delete", tint = MaterialTheme.colorScheme.error)
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
            color = MeenaTextWhite
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
                focusedTextColor = MeenaTextWhite,
                unfocusedTextColor = MeenaTextWhite
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
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
                    focusedTextColor = MeenaTextWhite,
                    unfocusedTextColor = MeenaTextWhite
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
                    focusedTextColor = MeenaTextWhite,
                    unfocusedTextColor = MeenaTextWhite
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
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
                    focusedTextColor = MeenaTextWhite,
                    unfocusedTextColor = MeenaTextWhite
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
                    focusedTextColor = MeenaTextWhite,
                    unfocusedTextColor = MeenaTextWhite
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
            Text("add holding", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
        }
    }
}

@Composable
private fun HealthAccountSettings(
    settingsState: MeenaUserSettings,
    onUpdate: ((MeenaUserSettings) -> MeenaUserSettings) -> Unit
) {
    val context = LocalContext.current
    val availability = remember { HealthRepository.getHealthConnectAvailability(context) }
    val installedApps = remember { HealthRepository.getInstalledFitnessApps(context) }

    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
        Text(
            text = "google fit has retired. health metrics sync securely on-device through android health connect.",
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )

        val statusText = when (availability) {
            HealthConnectAvailability.AVAILABLE -> "status · installed and ready"
            HealthConnectAvailability.UPDATE_REQUIRED -> "status · update required"
            HealthConnectAvailability.NOT_INSTALLED -> "status · not installed"
            HealthConnectAvailability.NOT_SUPPORTED -> "status · not supported on this device"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = if (availability == HealthConnectAvailability.AVAILABLE) MeenaProfitGreen else MeenaSolarAmber
        )

        Button(
            onClick = {
                if (availability == HealthConnectAvailability.AVAILABLE) {
                    HealthRepository.openHealthConnectSettings(context)
                } else {
                    HealthRepository.openHealthConnectPlayStore(context)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(
                text = if (availability == HealthConnectAvailability.AVAILABLE) "manage health connect" else "get health connect",
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextWhite
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("goals", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        SettingSegmentedRow(
            title = "daily step goal",
            options = listOf(
                "6000" to "6,000",
                "8000" to "8,000",
                "10000" to "10,000",
                "12000" to "12,000",
                "15000" to "15,000"
            ),
            selectedKey = settingsState.stepGoal.toString(),
            onSelectKey = { key ->
                val goal = key.toIntOrNull() ?: 10000
                onUpdate { it.copy(stepGoal = goal) }
                HealthRepository.refreshTelemetry(context, settingsState.copy(stepGoal = goal))
            }
        )

        SettingSegmentedRow(
            title = "daily sleep target",
            options = listOf(
                "7.0" to "7 hrs",
                "7.5" to "7.5 hrs",
                "8.0" to "8 hrs",
                "8.5" to "8.5 hrs",
                "9.0" to "9 hrs"
            ),
            selectedKey = String.format(java.util.Locale.US, "%.1f", settingsState.sleepHoursGoal),
            onSelectKey = { key ->
                val sleep = key.toFloatOrNull() ?: 8.0f
                onUpdate { it.copy(sleepHoursGoal = sleep) }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("companion app", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (installedApps.isNotEmpty()) {
            val appOptions = listOf("" to "auto (detect installed)") + installedApps.map { it.packageName to it.displayName }
            SettingSegmentedRow(
                title = "paired device app",
                options = appOptions,
                selectedKey = settingsState.healthCompanionPackage,
                onSelectKey = { pkg ->
                    val appName = installedApps.find { it.packageName == pkg }?.displayName ?: ""
                    onUpdate { it.copy(healthCompanionPackage = pkg, healthCompanionName = appName) }
                    HealthRepository.refreshTelemetry(context, settingsState.copy(healthCompanionPackage = pkg, healthCompanionName = appName))
                }
            )
        }

        Button(
            onClick = {
                HealthRepository.openFitnessApp(context, settingsState.healthCompanionPackage)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("open companion app", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("air quality", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "air quality metrics are provided live by open-meteo using your coarse approximate location.",
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted
        )
        Button(
            onClick = {
                HealthRepository.openAirQualityDetails(context)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("view open-meteo air quality", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
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

    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
        Text(
            text = "meena reads your agenda directly from your android system calendar. no google login or cloud sync needed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )

        if (!hasPerm) {
            Text(
                text = "meena needs calendar access to show your upcoming agenda on today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MeenaTextWhite
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
                Text("turn on calendar access in settings", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
            }
        } else {
            Column {
                Text("agenda lookahead", style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
                Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.s))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    listOf(1 to "1 day", 3 to "3 days", 7 to "7 days", 14 to "14 days").forEach { (days, label) ->
                        val isSelected = settingsState.calendarDaysAhead == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                .metroClickable { onUpdate { it.copy(calendarDaysAhead = days) } }
                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.m),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MeenaTextWhite else MeenaTextMuted
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
                Text("synced calendars", style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
                Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.s))
                if (availableCalendars.isEmpty()) {
                    Text(
                        text = "no local calendars found on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                        availableCalendars.forEach { cal ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
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
                                        color = MeenaTextWhite
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
                Text("open system calendar app", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
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
    var addProblem by remember { mutableStateOf<String?>(null) }
    var feedToRemove by remember { mutableStateOf<String?>(null) }
    val statuses by NewsRssRepository.feedStatuses.collectAsState()
    val shownFeeds = NewsRssRepository.effectiveFeeds(currentFeeds)
    val usingDefaults = currentFeeds.isEmpty()
    val presetFeeds = listOf(
        "bernama" to "https://news.google.com/rss/search?q=bernama&hl=en-MY&gl=MY&ceid=MY:en",
        "the star" to "https://www.thestar.com.my/rss/news/nation",
        "malaysiakini" to "https://www.malaysiakini.com/rss/en/news.rss",
        "techcrunch" to "https://techcrunch.com/feed/",
        "hacker news" to "https://news.ycombinator.com/rss"
    )
    val httpsOnly = stringResource(R.string.news_feeds_https_only)
    val alreadyThere = stringResource(R.string.news_feeds_already_added)

    feedToRemove?.let { url ->
        MetroDialog(
            title = stringResource(R.string.news_feeds_remove_title),
            message = stringResource(R.string.news_feeds_remove_question, NewsRssRepository.sourceNameFor(url).lowercase()),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
            onConfirm = {
                onUpdateFeeds(shownFeeds - url)
                feedToRemove = null
            },
            onDismiss = { feedToRemove = null }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
    ) {
        Text(
            text = stringResource(if (usingDefaults) R.string.news_feeds_using_defaults else R.string.news_feeds_yours),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        shownFeeds.forEach { url ->
            val status = statuses.firstOrNull { it.url == url }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = NewsRssRepository.sourceNameFor(url).lowercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MeenaTextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            status == null -> stringResource(R.string.news_feeds_not_checked)
                            status.problem != null -> stringResource(R.string.news_feeds_problem, status.problem)
                            else -> androidx.compose.ui.res.pluralStringResource(
                                R.plurals.news_feeds_ok,
                                status.articleCount,
                                status.articleCount,
                                my.hana.meena.ui.widgets.updatedAgoText(status.checkedAt)
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status?.problem != null) MaterialTheme.colorScheme.error else MeenaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = url.removePrefix("https://").lowercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.news_feeds_remove),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .metroClickable { feedToRemove = url }
                        .padding(start = my.hana.meena.theme.MeenaSpacing.m, top = my.hana.meena.theme.MeenaSpacing.m, bottom = my.hana.meena.theme.MeenaSpacing.m)
                )
            }
        }
        if (usingDefaults) {
            Text(
                text = stringResource(R.string.news_feeds_defaults_note),
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted
            )
        }

        Text(
            text = stringResource(R.string.news_feeds_add_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.l)
        )
        OutlinedTextField(
            value = newFeedUrl,
            onValueChange = {
                newFeedUrl = it
                addProblem = null
            },
            placeholder = { Text("https://example.com/feed", color = MeenaTextMuted, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MeenaBlack,
                unfocusedContainerColor = MeenaBlack,
                focusedTextColor = MeenaTextWhite,
                unfocusedTextColor = MeenaTextWhite,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MeenaBorder
            )
        )
        addProblem?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Text(
            text = stringResource(R.string.news_feeds_add),
            style = MaterialTheme.typography.labelSmall,
            color = if (newFeedUrl.isNotBlank()) MaterialTheme.colorScheme.primary else MeenaTextMuted,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .metroClickable(enabled = newFeedUrl.isNotBlank()) {
                    val trimmed = newFeedUrl.trim()
                    addProblem = when {
                        !trimmed.startsWith("https://", ignoreCase = true) -> httpsOnly
                        trimmed in shownFeeds -> alreadyThere
                        else -> null
                    }
                    if (addProblem == null) {
                        onUpdateFeeds(shownFeeds + trimmed)
                        newFeedUrl = ""
                    }
                }
                .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
        )

        val available = presetFeeds.filter { it.second !in shownFeeds }
        if (available.isNotEmpty()) {
            Text(
                text = stringResource(R.string.news_feeds_suggested),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.l)
            )
            available.forEach { (label, url) ->
                Text(
                    text = stringResource(R.string.news_feeds_add_named, label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onUpdateFeeds(shownFeeds + url) }
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
                )
            }
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
            .padding(bottom = my.hana.meena.theme.MeenaSpacing.m)
    ) {
        // Parent pivot name as a small accent link (Zune HD), no arrow symbol (Meena UI rule 7)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .metroClickable(onClick = onBack)
                .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
        ) {
            Text(
                text = parentTitle.lowercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
        Text(
            text = pageTitle.lowercase(),
            style = MaterialTheme.typography.displayLarge,
            color = MeenaTextWhite,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SolatZonePicker(
    currentZone: String,
    onSelectZone: (String) -> Unit
) {
    val zones = listOf(
        "" to "auto-detect by location (gps)",
        "WLY01" to "wly01 · kuala lumpur & putrajaya",
        "SGR01" to "sgr01 · gombak, petaling, sepang, hulu langat",
        "SGR02" to "sgr02 · kuala selangor, sabak bernam",
        "SGR03" to "sgr03 · klang, kuala langat",
        "JHR01" to "jhr01 · pulau aur & pemanggil",
        "JHR02" to "jhr02 · johor bahru, kota tinggi, kulai",
        "PNG01" to "png01 · pulau pinang",
        "PRK01" to "prk01 · tapah, slim river, tanjung malim",
        "PRK02" to "prk02 · ipoh, batu gajah, kampar",
        "SBH01" to "sbh01 · kota kinabalu, ranau, penampang",
        "SWK01" to "swk01 · kuching, bau, lundu, sematan"
    )

    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        zones.forEach { (code, label) ->
            val isSelected = currentZone.equals(code, ignoreCase = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                    .metroClickable { onSelectZone(code) }
                    .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.m),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = label.lowercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MeenaTextWhite else MeenaTextMuted
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
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
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
            Text("date pattern", style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
            Spacer(modifier = Modifier.height(4.dp))
            Text("live preview using today's date", style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val patterns = listOf(
                MeenaUserSettings.DEFAULT_DATE_PATTERN,
                "EEE, d MMM yyyy",
                "d MMMM yyyy",
                "yyyy-MM-dd",
                "d MMM"
            )
            val today = java.util.Date()
            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
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
                            .heightIn(min = 48.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                            .metroClickable { onUpdate { it.copy(dateFormatPattern = pattern) } }
                            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.m)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MeenaTextWhite
                            )
                            Text(
                                text = pattern,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MeenaTextWhite.copy(alpha = 0.8f) else MeenaTextMuted,
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
                Text("hijri day adjustment", style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
                Spacer(modifier = Modifier.height(4.dp))
                Text("compensate for moon sighting differences", style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    listOf(-2 to "-2d", -1 to "-1d", 0 to "exact", 1 to "+1d", 2 to "+2d").forEach { (offset, label) ->
                        val isSelected = settingsState.hijriDayOffset == offset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                .metroClickable { onUpdate { it.copy(hijriDayOffset = offset) } }
                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.m),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MeenaTextWhite else MeenaTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}


