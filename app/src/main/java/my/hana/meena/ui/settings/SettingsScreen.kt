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
import androidx.compose.foundation.layout.statusBarsPadding
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
    // A page opened from another page (clock -> date and time) goes back to that page.
    var parentSubPage by remember(initialSubPage) { mutableStateOf<String?>(null) }
    var subPageForward by remember { mutableStateOf(true) }
    fun openSubPage(id: String) {
        parentSubPage = activeSubPage
        subPageForward = true
        activeSubPage = id
    }
    fun closeSubPage() {
        subPageForward = false
        activeSubPage = parentSubPage
        parentSubPage = null
    }

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
        if (activeSubPage != null) closeSubPage() else onClose()
    }

    // Windows Phone turnstile entrance (respects reduced motion)
    var turnstileRotation by remember { mutableFloatStateOf(if (currentSettings.reducedMotion) 0f else 20f) }
    var turnstileAlpha by remember { mutableFloatStateOf(if (currentSettings.reducedMotion) 1f else 0f) }

    LaunchedEffect(currentSettings.reducedMotion) {
        if (!currentSettings.reducedMotion) {
            animate(
                initialValue = 20f,
                targetValue = 0f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) { value, _ -> turnstileRotation = value }
        }
    }

    LaunchedEffect(currentSettings.reducedMotion) {
        if (!currentSettings.reducedMotion) {
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
            .statusBarsPadding()
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
                    .metroClickable { if (activeSubPage != null) closeSubPage() else onClose() },
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
            isForward = subPageForward,
            reducedMotion = settingsState.reducedMotion
        ) { currentSubPage ->
            if (currentSubPage != null) {
                SubPageContainer(
                    subPageId = currentSubPage,
                    settingsState = settingsState,
                    onUpdate = { transform -> update(transform) },
                    onBack = { closeSubPage() },
                    onOpenSubPage = { openSubPage(it) },
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
                                SETTINGS_TAB_PAGES -> pagesTab(settingsState, onSortStory = { confirmStoryOrder = true }) { openSubPage(it) }
                                SETTINGS_TAB_CONNECTIONS -> connectionsTab(settingsState, context) { openSubPage(it) }
                                SETTINGS_TAB_PERSONALIZE -> personalizeTab(settingsState) { openSubPage(it) }
                                SETTINGS_TAB_SYSTEM -> systemTab(settingsState, context) { openSubPage(it) }
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

/** Name shown for a base theme id. */
internal fun themeLabelRes(baseTheme: String): Int = when (baseTheme) {
    "light" -> R.string.theme_light
    "paper" -> R.string.theme_paper
    else -> R.string.theme_dark
}

/** Widget id to the name used on its row and sub-page. */
internal val SETTINGS_WIDGET_TITLES = linkedMapOf<String, Int>(
    "widget-clock-weather" to R.string.widget_name_clock,
    "widget-weather-forecast" to R.string.widget_name_forecast,
    "widget-tasks" to R.string.widget_name_week,
    "widget-solat" to R.string.widget_name_solat,
    "widget-favourites" to R.string.widget_name_favourites,
    "widget-frequent-apps" to R.string.widget_name_frequent,
    "widget-notes" to R.string.widget_name_notes,
    "widget-calendar-month" to R.string.widget_name_month,
    "widget-health-telemetry" to R.string.widget_name_health,
    "widget-bluetooth-devices" to R.string.widget_name_devices,
    "widget-device-telemetry" to R.string.widget_name_phone,
    "widget-map-radar" to R.string.widget_name_nearby,
    "widget-wikipedia" to R.string.widget_name_wikipedia,
    "widget-conversations" to R.string.widget_name_chats,
    "widget-email" to R.string.widget_name_email,
    "widget-notifications" to R.string.widget_name_alerts,
    "widget-news-feed" to R.string.widget_name_news,
    "widget-markets" to R.string.widget_name_markets,
    "widget-finance-charts" to R.string.widget_name_portfolio,
    "widget-exchange-rates" to R.string.widget_name_rates,
    "widget-spending-summary" to R.string.widget_name_spending
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
    item {
        SettingsPivotRow(
            title = stringResource(R.string.widget_lines_title),
            status = stringResource(R.string.widget_lines_status, settingsState.widgetLines),
            onClick = { open("look:widgetlines") }
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
                    title = stringResource(SETTINGS_WIDGET_TITLES.getValue(id)),
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
            status = stringResource(
                R.string.settings_status_pair,
                stringResource(themeLabelRes(settingsState.baseTheme)),
                if (settingsState.baseTheme == "paper") stringResource(R.string.theme_paper_mono)
                else Meena10AccentSwatches.find { it.hex.equals(settingsState.accentHex, ignoreCase = true) }?.name
                    ?: settingsState.accentHex.lowercase()
            ),
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
    item {
        SettingsPivotRow(
            title = stringResource(R.string.live_tiles_title),
            status = stringResource(if (settingsState.liveTiles) R.string.live_tiles_status_on else R.string.live_tiles_status_off),
            isDimmed = !settingsState.liveTiles,
            onClick = { open("look:livetiles") }
        )
    }
    item {
        SettingsPivotRow(
            title = stringResource(R.string.home_title_title),
            status = my.hana.meena.data.model.HomeTitle.display(settingsState.homeTitle)
                .ifEmpty { stringResource(R.string.home_title_hidden) },
            onClick = { open("look:hometitle") }
        )
    }
    settingsGroupLabel(R.string.settings_group_feel)
    item {
        val timeMode = stringResource(if (settingsState.is24HourFormat) R.string.datetime_status_24h else R.string.datetime_status_12h)
        val hijriMode = stringResource(if (settingsState.showHijriDate) R.string.datetime_status_hijri_on else R.string.datetime_status_hijri_off)
        SettingsPivotRow(
            title = stringResource(R.string.settings_datetime_title),
            status = stringResource(R.string.settings_status_pair, timeMode, hijriMode),
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

private fun LazyListScope.systemTab(settingsState: MeenaUserSettings, context: android.content.Context, open: (String) -> Unit) {
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
            title = stringResource(R.string.shake_hide_title),
            status = stringResource(R.string.shake_hide_status),
            onClick = { open("look:shake") }
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
        // Read each time the tab shows, since the choice is made outside Meena.
        val isHome = my.hana.meena.data.system.HomeRole.isDefault(context)
        SettingsPivotRow(
            title = stringResource(R.string.home_app_title),
            status = stringResource(if (isHome) R.string.home_app_status_on else R.string.home_app_status_off),
            onClick = { open("look:homeapp") }
        )
    }
    item {
        val isOn = settingsState.doubleTapToLock && my.hana.meena.data.system.DeviceLock.isActive(context)
        SettingsPivotRow(
            title = stringResource(R.string.lock_title),
            status = stringResource(if (isOn) R.string.lock_status_on else R.string.lock_status_off),
            isDimmed = !isOn,
            onClick = { open("look:lock") }
        )
    }
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
    var confirmReset by remember { mutableStateOf(false) }

    if (confirmReset) {
        MetroDialog(
            title = stringResource(R.string.backup_reset),
            message = stringResource(R.string.backup_reset_question),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
            onConfirm = {
                confirmReset = false
                onResetDefaults()
            },
            onDismiss = { confirmReset = false }
        )
    }

    if (showNotificationDisclosure) {
        MetroDialog(
            title = stringResource(R.string.notif_access_title),
            message = stringResource(R.string.notif_access_question),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
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
                    parentTitle = stringResource(R.string.settings_tab_pages),
                    pageTitle = SETTINGS_WIDGET_TITLES[widgetId]?.let { stringResource(it) } ?: widgetId.replace("widget-", "").replace("-", " "),
                    onBack = onBack
                )
            }

            // Universal Widget Controls
            item {
                SettingToggleRow(
                    title = stringResource(R.string.widget_show_title),
                    subtitle = stringResource(R.string.widget_show_hint),
                    checked = config.isEnabled,
                    onCheckedChange = { checked ->
                        val newMap = settingsState.widgetConfigs.toMutableMap()
                        newMap[widgetId] = config.copy(isEnabled = checked)
                        onUpdate { it.copy(widgetConfigs = newMap) }
                    }
                )
            }

            item {
                SettingSegmentedRow(
                    title = stringResource(R.string.widget_page_title),
                    options = listOf(
                        "today" to stringResource(R.string.settings_page_today),
                        "inbox" to stringResource(R.string.settings_page_inbox),
                        "news" to stringResource(R.string.settings_page_news),
                        "money" to stringResource(R.string.settings_page_money)
                    ),
                    selectedKey = config.hubId,
                    onSelectKey = { hub ->
                        val newMap = settingsState.widgetConfigs.toMutableMap()
                        newMap[widgetId] = config.copy(hubId = hub)
                        onUpdate { it.copy(widgetConfigs = newMap) }
                    }
                )
            }

            item {
                SettingToggleRow(
                    title = stringResource(R.string.widget_folded_title),
                    subtitle = stringResource(R.string.widget_folded_hint),
                    checked = config.isCollapsed,
                    onCheckedChange = { collapsed ->
                        val newMap = settingsState.widgetConfigs.toMutableMap()
                        newMap[widgetId] = config.copy(isCollapsed = collapsed)
                        onUpdate { it.copy(widgetConfigs = newMap) }
                    }
                )
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

            // Only rows a widget really reads; SettingsKeysTest keeps it that way.
            when (widgetId) {
                "widget-clock-weather" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.widget_more_group), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingsPivotRow(
                            title = stringResource(R.string.settings_datetime_title),
                            status = stringResource(R.string.settings_clock_link_status),
                            onClick = { onOpenSubPage("look:datetime") }
                        )
                    }
                    item {
                        SettingSegmentedRow(
                            title = stringResource(R.string.widget_temp_title),
                            subtitle = stringResource(R.string.widget_temp_hint),
                            options = listOf("c" to stringResource(R.string.temp_celsius), "f" to stringResource(R.string.temp_fahrenheit)),
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

                "widget-spending-summary" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.widget_more_group), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        Button(
                            onClick = { showNotificationDisclosure = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text(stringResource(R.string.notif_open_settings), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
                        }
                    }
                }

                "widget-notifications" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.widget_more_group), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        Button(
                            onClick = { showNotificationDisclosure = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text(stringResource(R.string.notif_open_settings), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
                        }
                    }
                }

                "widget-conversations" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.widget_more_group), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        Button(
                            onClick = { showNotificationDisclosure = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text(stringResource(R.string.notif_open_settings), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
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
                        val storyCounts = listOf(6, 12, 20, 40)
                        SettingSegmentedRow(
                            title = stringResource(R.string.news_stories_title),
                            options = storyCounts.map { it.toString() to androidx.compose.ui.res.pluralStringResource(R.plurals.news_stories_count, it, it) },
                            selectedKey = custom("max_stories", my.hana.meena.ui.hubs.DEFAULT_VISIBLE_STORIES.toString()),
                            onSelectKey = { setCustom("max_stories", it) }
                        )
                    }
                }

                "widget-health-telemetry" -> {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.widget_more_group), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        SettingsPivotRow(
                            title = stringResource(R.string.settings_health_title),
                            status = stringResource(R.string.settings_health_link_status),
                            onClick = { onOpenSubPage("account:health") }
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
                            colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text(stringResource(R.string.health_open_settings), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
                        }
                    }
                }
            }
        }

        // Dedicated Account Sub-Pages
        subPageId == "account:solat" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_connections),
                    pageTitle = stringResource(R.string.settings_solat_title),
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = stringResource(R.string.solat_page_intro),
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
                    parentTitle = stringResource(R.string.settings_tab_connections),
                    pageTitle = stringResource(R.string.settings_notifications_title),
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = stringResource(R.string.notif_page_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
            item {
                Button(
                    onClick = { showNotificationDisclosure = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(stringResource(R.string.notif_open_settings), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
                }
            }
        }

        subPageId == "account:finance" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_connections),
                    pageTitle = stringResource(R.string.settings_money_title),
                    onBack = onBack
                )
            }
            item {
                FinanceAccountSettings(
                    settingsState = settingsState,
                    onUpdate = onUpdate,
                    onOpenGuide = { onOpenSubPage("account:sheetguide") }
                )
            }
        }

        subPageId == "account:sheetguide" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_money_title),
                    pageTitle = stringResource(R.string.finance_guide_page),
                    onBack = onBack
                )
            }
            item {
                Text(stringResource(R.string.finance_guide_step_1), style = MaterialTheme.typography.bodyLarge, color = MeenaTextWhite)
            }
            item {
                Column(
                    modifier = Modifier.padding(start = my.hana.meena.theme.MeenaSpacing.m),
                    verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    listOf(
                        R.string.finance_guide_col_a, R.string.finance_guide_col_b, R.string.finance_guide_col_c,
                        R.string.finance_guide_col_d, R.string.finance_guide_col_e
                    ).forEach { line ->
                        Text(stringResource(line), style = MaterialTheme.typography.bodyMedium, color = MeenaTextMuted)
                    }
                }
            }
            item {
                Text(stringResource(R.string.finance_guide_step_2), style = MaterialTheme.typography.bodyLarge, color = MeenaTextWhite)
            }
            item {
                Text(stringResource(R.string.finance_guide_step_3), style = MaterialTheme.typography.bodyLarge, color = MeenaTextWhite)
            }
            item {
                Text(stringResource(R.string.finance_delay_notice), style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted)
            }
        }

        subPageId == "account:news" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_connections),
                    pageTitle = stringResource(R.string.settings_news_title),
                    onBack = onBack
                )
            }
            item {
                NewsFeedManager(
                    currentFeeds = settingsState.newsFeeds,
                    nsfwFeeds = settingsState.nsfwFeeds,
                    onUpdateFeeds = { feeds ->
                        // Saving exactly the built-in list keeps "using built-in feeds" behaviour.
                        val toSave = if (feeds == NewsRssRepository.DEFAULT_FEEDS) emptyList() else feeds
                        // A removed feed drops its nsfw mark too.
                        val shown = NewsRssRepository.effectiveFeeds(toSave)
                        onUpdate { it.copy(newsFeeds = toSave, nsfwFeeds = it.nsfwFeeds.filter { url -> url in shown }) }
                        NewsRssRepository.updateFeeds(toSave)
                    },
                    onUpdateNsfw = { nsfw -> onUpdate { it.copy(nsfwFeeds = nsfw) } }
                )
            }
        }

        subPageId == "account:health" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_connections),
                    pageTitle = stringResource(R.string.settings_health_title),
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
                    parentTitle = stringResource(R.string.settings_tab_connections),
                    pageTitle = stringResource(R.string.settings_calendar_title),
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
                    parentTitle = stringResource(R.string.settings_tab_personalize),
                    pageTitle = stringResource(R.string.settings_datetime_title),
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
                    parentTitle = stringResource(R.string.settings_tab_personalize),
                    pageTitle = stringResource(R.string.settings_theme_title),
                    onBack = onBack
                )
            }
            item {
                SettingSegmentedRow(
                    title = stringResource(R.string.theme_background_title),
                    options = listOf(
                        "dark" to stringResource(R.string.theme_dark),
                        "light" to stringResource(R.string.theme_light),
                        "paper" to stringResource(R.string.theme_paper)
                    ),
                    selectedKey = settingsState.baseTheme,
                    onSelectKey = { theme -> onUpdate { it.copy(baseTheme = theme) } }
                )
            }
            if (settingsState.baseTheme == "paper") {
                item {
                    Text(
                        text = stringResource(R.string.theme_paper_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            } else

            item {
                Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.m))
                Text(
                    text = stringResource(R.string.theme_accent_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))

                val currentBg = my.hana.meena.theme.meenaPaletteFor(settingsState.baseTheme).background
                val currentAccentColor = runCatching {
                    Color(settingsState.accentHex.toColorInt())
                }.getOrDefault(MeenaBlue)
                val isAccessible = isAccessibleContrast(currentAccentColor, currentBg)
                val activeSwatch = Meena10AccentSwatches.find { it.hex.equals(settingsState.accentHex, ignoreCase = true) }
                val swatchName = activeSwatch?.name ?: settingsState.accentHex.lowercase()

                Text(
                    text = stringResource(
                        if (isAccessible) R.string.theme_accent_readable else R.string.theme_accent_hard,
                        swatchName,
                        stringResource(themeLabelRes(settingsState.baseTheme))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextSecondary
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
                    parentTitle = stringResource(R.string.settings_tab_personalize),
                    pageTitle = stringResource(R.string.settings_text_title),
                    onBack = onBack
                )
            }


            item {
                val textSizes = (85..120 step 5).toList()
                SettingSegmentedRow(
                    title = stringResource(R.string.text_size_row),
                    options = textSizes.map { it.toString() to stringResource(R.string.settings_text_status, it) },
                    selectedKey = settingsState.fontScalePercent.toString(),
                    onSelectKey = { key ->
                        val percent = key.toIntOrNull() ?: 100
                        onUpdate { it.copy(fontScalePercent = percent) }
                    }
                )
            }
        }

        subPageId == "look:ergonomics" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_personalize),
                    pageTitle = stringResource(R.string.settings_hand_title),
                    onBack = onBack
                )
            }
            item {
                SettingToggleRow(
                    title = stringResource(R.string.hand_left_title),
                    subtitle = stringResource(R.string.hand_left_hint),
                    checked = settingsState.isLeftHanded,
                    onCheckedChange = { onUpdate { it.copy(isLeftHanded = it.isLeftHanded.not()) } }
                )
            }
            item {
                SettingToggleRow(
                    title = stringResource(R.string.motion_title),
                    subtitle = stringResource(R.string.motion_hint),
                    checked = settingsState.isReducedMotion,
                    onCheckedChange = { onUpdate { it.copy(isReducedMotion = it.isReducedMotion.not()) } }
                )
            }
            item {
                SettingToggleRow(
                    title = stringResource(R.string.haptics_title),
                    subtitle = stringResource(R.string.haptics_hint),
                    checked = settingsState.isHapticEnabled,
                    onCheckedChange = { onUpdate { it.copy(isHapticEnabled = it.isHapticEnabled.not()) } }
                )
            }
            item {
                SettingToggleRow(
                    title = stringResource(R.string.status_bar_title),
                    subtitle = stringResource(R.string.status_bar_hint),
                    checked = settingsState.isStatusBarHidden,
                    onCheckedChange = { onUpdate { it.copy(isStatusBarHidden = it.isStatusBarHidden.not()) } }
                )
            }
            item {
                SettingSegmentedRow(
                    title = stringResource(R.string.start_page_title),
                    subtitle = stringResource(R.string.start_page_hint),
                    options = listOf(
                        "0" to stringResource(R.string.settings_page_today),
                        "1" to stringResource(R.string.settings_page_inbox),
                        "2" to stringResource(R.string.settings_page_news),
                        "3" to stringResource(R.string.settings_page_money),
                        "4" to stringResource(R.string.settings_page_apps)
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

        subPageId == "look:hometitle" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_personalize),
                    pageTitle = stringResource(R.string.home_title_title),
                    onBack = onBack
                )
            }
            item {
                Text(stringResource(R.string.home_title_intro), style = MaterialTheme.typography.bodyMedium, color = MeenaTextMuted)
            }
            item {
                OutlinedTextField(
                    value = settingsState.homeTitle,
                    onValueChange = { typed -> onUpdate { it.copy(homeTitle = my.hana.meena.data.model.HomeTitle.clean(typed)) } },
                    placeholder = { Text(stringResource(R.string.home_title_hidden), color = MeenaTextMuted) },
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
            }
            if (settingsState.homeTitle != my.hana.meena.data.model.HomeTitle.DEFAULT) {
                item {
                    SettingLinkRow(
                        title = stringResource(R.string.home_title_reset),
                        status = stringResource(R.string.home_title_reset_hint),
                        onClick = { onUpdate { it.copy(homeTitle = my.hana.meena.data.model.HomeTitle.DEFAULT) } }
                    )
                }
            }
        }

        subPageId == "look:widgetlines" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_pages),
                    pageTitle = stringResource(R.string.widget_lines_title),
                    onBack = onBack
                )
            }
            item {
                SettingToggleRow(
                    title = stringResource(R.string.widget_lines_toggle),
                    subtitle = stringResource(R.string.widget_lines_toggle_hint),
                    checked = settingsState.shortWidgets,
                    onCheckedChange = { on -> onUpdate { it.copy(shortWidgets = on) } }
                )
            }
        }

        subPageId == "look:shake" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_system),
                    pageTitle = stringResource(R.string.shake_hide_title),
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = stringResource(R.string.shake_hide_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted,
                    modifier = Modifier.padding(bottom = my.hana.meena.theme.MeenaSpacing.m)
                )
            }
            item {
                SettingToggleRow(
                    title = stringResource(R.string.shake_hide_toggle),
                    subtitle = stringResource(R.string.shake_hide_toggle_hint),
                    checked = settingsState.shakeToHide,
                    onCheckedChange = { on -> onUpdate { it.copy(shakeToHide = on) } }
                )
            }
        }

        subPageId == "look:lock" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_system),
                    pageTitle = stringResource(R.string.lock_title),
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = stringResource(R.string.lock_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted,
                    modifier = Modifier.padding(bottom = my.hana.meena.theme.MeenaSpacing.m)
                )
            }
            item {
                val lockContext = LocalContext.current
                val explanation = stringResource(R.string.lock_admin_description)
                // Android's own page answers; only an active admin turns the setting on.
                val adminLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                ) {
                    val active = my.hana.meena.data.system.DeviceLock.isActive(lockContext)
                    onUpdate { it.copy(doubleTapToLock = active) }
                }
                SettingToggleRow(
                    title = stringResource(R.string.lock_toggle),
                    subtitle = stringResource(R.string.lock_toggle_hint),
                    checked = settingsState.doubleTapToLock && my.hana.meena.data.system.DeviceLock.isActive(lockContext),
                    onCheckedChange = { on ->
                        if (!on) {
                            my.hana.meena.data.system.DeviceLock.deactivate(lockContext)
                            onUpdate { it.copy(doubleTapToLock = false) }
                        } else if (my.hana.meena.data.system.DeviceLock.isActive(lockContext)) {
                            onUpdate { it.copy(doubleTapToLock = true) }
                        } else {
                            runCatching {
                                adminLauncher.launch(my.hana.meena.data.system.DeviceLock.requestIntent(lockContext, explanation))
                            }
                        }
                    }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.lock_after_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
            item {
                Text(
                    text = stringResource(R.string.lock_uninstall_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
        }

        subPageId == "look:homeapp" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_system),
                    pageTitle = stringResource(R.string.home_app_title),
                    onBack = onBack
                )
            }
            item {
                val homeContext = LocalContext.current
                val isHome = my.hana.meena.data.system.HomeRole.isDefault(homeContext)
                Text(
                    text = stringResource(if (isHome) R.string.home_app_page_on else R.string.home_app_page_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted,
                    modifier = Modifier.padding(bottom = my.hana.meena.theme.MeenaSpacing.m)
                )
            }
            item {
                val homeContext = LocalContext.current
                SettingLinkRow(
                    title = stringResource(R.string.home_app_pick),
                    status = stringResource(R.string.home_app_pick_hint),
                    onClick = { my.hana.meena.data.system.HomeRole.openPicker(homeContext) }
                )
            }
        }

        subPageId == "look:livetiles" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_personalize),
                    pageTitle = stringResource(R.string.live_tiles_title),
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = stringResource(R.string.live_tiles_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted,
                    modifier = Modifier.padding(bottom = my.hana.meena.theme.MeenaSpacing.m)
                )
            }
            item {
                SettingToggleRow(
                    title = stringResource(R.string.live_tiles_title),
                    subtitle = stringResource(R.string.live_tiles_toggle_hint),
                    checked = settingsState.liveTiles,
                    onCheckedChange = { on -> onUpdate { it.copy(liveTiles = on) } }
                )
            }
            item {
                SettingToggleRow(
                    title = stringResource(R.string.live_tiles_names),
                    subtitle = stringResource(R.string.live_tiles_names_hint),
                    checked = settingsState.liveTileNames,
                    onCheckedChange = { on -> onUpdate { it.copy(liveTileNames = on) } }
                )
            }
            if (settingsState.isReducedMotion) {
                item {
                    Text(
                        text = stringResource(R.string.live_tiles_reduced_motion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted,
                        modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.m)
                    )
                }
            }
        }

        subPageId == "look:backup" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_system),
                    pageTitle = stringResource(R.string.settings_backup_title),
                    onBack = onBack
                )
            }
            item {
                Button(
                    onClick = onExportBackup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(stringResource(R.string.backup_share), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
                }
            }
            item {
                Button(
                    onClick = onImportBackup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(stringResource(R.string.backup_import), style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                }
            }
            item {
                Button(
                    onClick = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(stringResource(R.string.backup_reset), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        subPageId == "look:about" -> {
            val aboutVersion = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                .getOrNull() ?: "1.0"
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_system),
                    pageTitle = stringResource(R.string.settings_about_title),
                    onBack = onBack
                )
            }
            item {
                Text(
                    text = stringResource(R.string.about_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextWhite
                )
            }
            item {
                Text(
                    text = stringResource(R.string.about_charter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_inspired),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
            item {
                SettingLinkRow(
                    title = stringResource(R.string.settings_privacy_title),
                    status = stringResource(R.string.privacy_status),
                    onClick = { onOpenSubPage("look:privacy") }
                )
            }
            item {
                val policyContext = LocalContext.current
                val policyFailed = stringResource(R.string.privacy_policy_open_failed)
                SettingLinkRow(
                    title = stringResource(R.string.privacy_policy_title),
                    status = stringResource(R.string.privacy_policy_status),
                    onClick = {
                        if (!my.hana.meena.data.system.PrivacyPolicy.open(policyContext)) {
                            my.hana.meena.ui.components.MetroMessage.show(policyFailed)
                        }
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_open_source_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_open_source_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
            item {
                // The map libraries' and font's licences ask for their notices to ship with the app; shown word for word.
                var showMapLicences by remember { mutableStateOf(false) }
                Text(
                    text = stringResource(R.string.about_map_libraries),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
                Text(
                    text = stringResource(if (showMapLicences) R.string.about_map_licences_hide else R.string.about_map_licences_show),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .metroClickable { showMapLicences = !showMapLicences }
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
                )
                if (showMapLicences) {
                    val licences = remember { my.hana.meena.ui.components.MapAssets.licences(context) }
                    Text(
                        text = licences,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_version, aboutVersion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        subPageId == "look:privacy" -> {
            item {
                MetroSubPageHeader(
                    parentTitle = stringResource(R.string.settings_tab_system),
                    pageTitle = stringResource(R.string.settings_privacy_title),
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
                    text = stringResource(R.string.privacy_on_device_heading),
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
                    text = stringResource(R.string.privacy_reads_heading),
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
                        text = stringResource(R.string.privacy_health_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                    Text(
                        text = stringResource(R.string.privacy_portfolio_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.privacy_backup_heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.privacy_backup_body),
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
    onUpdate: ((MeenaUserSettings) -> MeenaUserSettings) -> Unit,
    onOpenGuide: () -> Unit
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
            title = stringResource(R.string.finance_delete_title),
            message = stringResource(R.string.finance_delete_question, holding.symbol.lowercase()),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
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
            text = stringResource(R.string.finance_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.finance_sheet_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted
        )

        OutlinedTextField(
            value = sheetUrlInput,
            onValueChange = { sheetUrlInput = it },
            label = { Text(stringResource(R.string.finance_sheet_link), style = MaterialTheme.typography.labelSmall) },
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
                colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(stringResource(R.string.finance_sheet_save), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
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
                    Text(stringResource(R.string.finance_sheet_unlink), style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                }
            }
        }

        Text(
            text = stringResource(R.string.finance_delay_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted
        )
        SettingLinkRow(
            title = stringResource(R.string.finance_guide_title),
            status = stringResource(R.string.finance_guide_status),
            onClick = onOpenGuide
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Local Portfolio Holdings
        Text(
            text = stringResource(R.string.finance_holdings_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.finance_holdings_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted
        )

        SettingToggleRow(
            title = stringResource(R.string.finance_hide_amounts),
            subtitle = stringResource(R.string.finance_hide_amounts_hint),
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
                                text = stringResource(R.string.settings_status_pair, holding.symbol.lowercase(), holding.groupName.lowercase()),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MeenaTextWhite
                            )
                            Text(
                                text = stringResource(
                                    R.string.finance_holding_detail,
                                    java.math.BigDecimal.valueOf(holding.shares).stripTrailingZeros().toPlainString(),
                                    "%.2f".format(holding.averageCost),
                                    "%.2f".format(holding.currentPrice)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                        IconButton(
                            onClick = { holdingToDelete = holding }
                        ) {
                            Icon(ImageVector.vectorResource(id = my.hana.meena.R.drawable.ic_close), contentDescription = stringResource(R.string.finance_delete_desc), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.finance_holdings_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted
            )
        }

        // Add New Holding Form
        Text(
            text = stringResource(R.string.finance_add_title),
            style = MaterialTheme.typography.titleSmall,
            color = MeenaTextWhite
        )

        OutlinedTextField(
            value = newSymbol,
            onValueChange = { newSymbol = it },
            label = { Text(stringResource(R.string.finance_symbol_label), style = MaterialTheme.typography.labelSmall) },
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
                label = { Text(stringResource(R.string.finance_units_label), style = MaterialTheme.typography.labelSmall) },
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
                label = { Text(stringResource(R.string.finance_cost_label), style = MaterialTheme.typography.labelSmall) },
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
                label = { Text(stringResource(R.string.finance_price_label), style = MaterialTheme.typography.labelSmall) },
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
                label = { Text(stringResource(R.string.finance_account_label), style = MaterialTheme.typography.labelSmall) },
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
            colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(stringResource(R.string.finance_add_button), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
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
            text = stringResource(R.string.health_connect_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )

        val statusText = when (availability) {
            HealthConnectAvailability.AVAILABLE -> stringResource(R.string.health_status_ready)
            HealthConnectAvailability.UPDATE_REQUIRED -> stringResource(R.string.health_status_update)
            HealthConnectAvailability.NOT_INSTALLED -> stringResource(R.string.health_status_missing)
            HealthConnectAvailability.NOT_SUPPORTED -> stringResource(R.string.health_status_unsupported)
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
            colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(
                text = stringResource(if (availability == HealthConnectAvailability.AVAILABLE) R.string.health_manage_permissions else R.string.health_install_app),
                style = MaterialTheme.typography.labelSmall,
                color = my.hana.meena.theme.MeenaOnAccent
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(stringResource(R.string.health_goals_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        SettingSegmentedRow(
            title = stringResource(R.string.health_step_goal_title),
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(stringResource(R.string.health_companion_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (installedApps.isNotEmpty()) {
            val appOptions = listOf("" to stringResource(R.string.health_companion_auto)) + installedApps.map { it.packageName to it.displayName }
            SettingSegmentedRow(
                title = stringResource(R.string.health_companion_pick),
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
            Text(stringResource(R.string.health_open_companion), style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(stringResource(R.string.air_quality_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.air_quality_source),
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
            Text(stringResource(R.string.air_quality_open), style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
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
            text = stringResource(R.string.calendar_page_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )

        if (!hasPerm) {
            Text(
                text = stringResource(R.string.calendar_page_needs_access),
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
                colors = ButtonDefaults.buttonColors(containerColor = my.hana.meena.theme.MeenaAccentFill)
            ) {
                Text(stringResource(R.string.calendar_turn_on), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
            }
        } else {
            val dayChoices = listOf(1, 3, 7, 14)
            SettingSegmentedRow(
                title = stringResource(R.string.calendar_ahead_title),
                options = dayChoices.map { it.toString() to androidx.compose.ui.res.pluralStringResource(R.plurals.calendar_days_count, it, it) },
                selectedKey = settingsState.calendarDaysAhead.toString(),
                onSelectKey = { key -> key.toIntOrNull()?.let { days -> onUpdate { it.copy(calendarDaysAhead = days) } } }
            )

            SettingToggleRow(
                title = stringResource(R.string.calendar_all_day_title),
                subtitle = stringResource(R.string.calendar_all_day_hint),
                checked = settingsState.calendarShowAllDay,
                onCheckedChange = { checked -> onUpdate { it.copy(calendarShowAllDay = checked) } }
            )

            Column {
                Text(stringResource(R.string.calendar_list_title), style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
                Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.s))
                if (availableCalendars.isEmpty()) {
                    Text(
                        text = stringResource(R.string.calendar_list_empty),
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
                Text(stringResource(R.string.calendar_open_app), style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
            }
        }
    }
}

@Composable
private fun NewsFeedManager(
    currentFeeds: List<String>,
    nsfwFeeds: List<String>,
    onUpdateFeeds: (List<String>) -> Unit,
    onUpdateNsfw: (List<String>) -> Unit
) {
    var newFeedUrl by remember { mutableStateOf("") }
    var addProblem by remember { mutableStateOf<String?>(null) }
    var feedToRemove by remember { mutableStateOf<String?>(null) }
    val statuses by NewsRssRepository.feedStatuses.collectAsState()
    // Settings can open before the news page has ever loaded; make sure the saved feeds are the ones checked.
    LaunchedEffect(Unit) { NewsRssRepository.updateFeeds(currentFeeds) }
    val shownFeeds = NewsRssRepository.effectiveFeeds(currentFeeds)
    val usingDefaults = currentFeeds.isEmpty()
    val presetFeeds = listOf(
        "amanz" to NewsRssRepository.AMANZ_FEED_URL,
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
            text = stringResource(R.string.news_nsfw_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.l)
        )
        Text(
            text = stringResource(R.string.news_nsfw_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextMuted
        )
        shownFeeds.forEach { url ->
            SettingToggleRow(
                title = NewsRssRepository.sourceNameFor(url).lowercase(),
                subtitle = stringResource(if (url in nsfwFeeds) R.string.news_nsfw_on else R.string.news_nsfw_off),
                checked = url in nsfwFeeds,
                onCheckedChange = { on -> onUpdateNsfw(if (on) (nsfwFeeds + url).distinct() else nsfwFeeds - url) }
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
            placeholder = { Text(stringResource(R.string.news_feed_url_hint), color = MeenaTextMuted, fontSize = 12.sp) },
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
    val zones = listOf("" to stringResource(R.string.settings_solat_auto)) +
        my.hana.meena.data.system.JakimSolatRepository.AVAILABLE_ZONES.map {
            it.code to stringResource(R.string.settings_status_pair, it.code.lowercase(), it.name.lowercase())
        }

    SettingSegmentedRow(
        title = stringResource(R.string.solat_zone_title),
        options = zones,
        selectedKey = zones.firstOrNull { it.first.equals(currentZone, ignoreCase = true) }?.first ?: currentZone,
        onSelectKey = onSelectZone
    )
}

@Composable
private fun DateTimeSettings(
    settingsState: MeenaUserSettings,
    onUpdate: ((MeenaUserSettings) -> MeenaUserSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
        Text(
            text = stringResource(R.string.datetime_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )

        SettingToggleRow(
            title = stringResource(R.string.datetime_24h_title),
            subtitle = stringResource(R.string.datetime_24h_hint),
            checked = settingsState.is24HourFormat,
            onCheckedChange = { onUpdate { it.copy(is24HourFormat = it.is24HourFormat.not()) } }
        )

        SettingToggleRow(
            title = stringResource(R.string.datetime_seconds_title),
            subtitle = stringResource(R.string.datetime_seconds_hint),
            checked = settingsState.showClockSeconds,
            onCheckedChange = { onUpdate { it.copy(showClockSeconds = it.showClockSeconds.not()) } }
        )

        val patterns = listOf(
            MeenaUserSettings.DEFAULT_DATE_PATTERN,
            "EEE, d MMM yyyy",
            "d MMMM yyyy",
            "yyyy-MM-dd",
            "d MMM"
        )
        val today = remember { java.util.Date() }
        SettingSegmentedRow(
            title = stringResource(R.string.datetime_pattern_title),
            subtitle = stringResource(R.string.datetime_pattern_hint),
            options = patterns.map { pattern ->
                pattern to (runCatching {
                    java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(today).lowercase()
                }.getOrNull() ?: pattern)
            },
            selectedKey = settingsState.dateFormatPattern,
            onSelectKey = { pattern -> onUpdate { it.copy(dateFormatPattern = pattern) } }
        )

        SettingToggleRow(
            title = stringResource(R.string.datetime_hijri_title),
            subtitle = stringResource(R.string.datetime_hijri_hint),
            checked = settingsState.showHijriDate,
            onCheckedChange = { onUpdate { it.copy(showHijriDate = it.showHijriDate.not()) } }
        )

        if (settingsState.showHijriDate) {
            SettingSegmentedRow(
                title = stringResource(R.string.datetime_hijri_offset_title),
                subtitle = stringResource(R.string.datetime_hijri_offset_hint),
                options = (-2..2).map { offset ->
                    offset.toString() to when {
                        offset < 0 -> androidx.compose.ui.res.pluralStringResource(R.plurals.datetime_hijri_earlier, -offset, -offset)
                        offset > 0 -> androidx.compose.ui.res.pluralStringResource(R.plurals.datetime_hijri_later, offset, offset)
                        else -> stringResource(R.string.datetime_hijri_offset_none)
                    }
                },
                selectedKey = settingsState.hijriDayOffset.toString(),
                onSelectKey = { key -> key.toIntOrNull()?.let { offset -> onUpdate { it.copy(hijriDayOffset = offset) } } }
            )
        }
    }
}


