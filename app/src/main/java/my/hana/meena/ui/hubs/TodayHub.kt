package my.hana.meena.ui.hubs

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import my.hana.meena.ui.components.MetroDottedProgress
import my.hana.meena.ui.components.MetroLineProgress
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.components.metroFeatherIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.data.health.HealthRepository
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.CalendarState
import my.hana.meena.data.system.DeviceCalendarRepository
import my.hana.meena.data.system.DeviceTelemetryHelper
import my.hana.meena.data.system.JakimSchedule
import my.hana.meena.data.system.JakimSolatRepository
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.CollapsibleWidget
import my.hana.meena.ui.components.DynamicSolatCelestialCard
import my.hana.meena.ui.components.ForecastTiles
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlin.math.roundToInt
import my.hana.meena.data.system.WeatherRepository
import my.hana.meena.ui.widgets.WidgetState
import my.hana.meena.data.system.InstalledAppsRepository
import my.hana.meena.data.system.ClockInfo
import my.hana.meena.R
import androidx.compose.ui.res.stringResource
import my.hana.meena.ui.components.OpenMapsRadarWidget
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayHub(
    settings: MeenaUserSettings,
    listState: LazyListState,
    onUpdateSolatZone: (String) -> Unit = {},
    onLaunchPackage: (String) -> Unit = {},
    onOpenDialer: () -> Unit = {},
    onOpenMessages: () -> Unit = {},
    onOpenEmail: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onOpenCalculator: () -> Unit = {},
    onRequestCalendarPermission: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenWeatherDetail: () -> Unit = {},
    onOpenApps: () -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    onReorderPin: (Int, Int) -> Unit = { _, _ -> },
    onToggleWidgetCollapse: (String, Boolean) -> Unit = { _, _ -> }
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val initialDate = remember { Date() }
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(initialDate))
    }
    var currentDate by remember {
        mutableStateOf(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(initialDate).lowercase())
    }
    var telemetry by remember { mutableStateOf(DeviceTelemetryHelper.getTelemetry(context)) }

    val solatState by JakimSolatRepository.state.collectAsState()
    val jakimSchedule = (solatState as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data ?:
                        (solatState as? my.hana.meena.ui.widgets.WidgetState.Stale)?.data ?:
                        JakimSchedule()

    val healthState by HealthRepository.state.collectAsState()
    val healthData = (healthState as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data ?:
                     (healthState as? my.hana.meena.ui.widgets.WidgetState.Stale)?.data ?:
                     my.hana.meena.data.health.HealthData()
    val fitTelemetry = healthData.fitTelemetry
    val airIndex = healthData.airIndex

    val weatherState by WeatherRepository.state.collectAsState()
    val calendarState by DeviceCalendarRepository.state.collectAsState()
    val deviceLocation by my.hana.meena.data.system.DeviceLocationHelper.locationFlow.collectAsState()
    val quickNotes = remember { mutableStateListOf<String>() }
    var appsVersion by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        InstalledAppsRepository.loadInstalledApps(context)
        appsVersion++
    }

    LaunchedEffect(settings.calendarDaysAhead, settings.calendarShowAllDay) {
        DeviceCalendarRepository.refresh(
            context = context,
            daysAhead = settings.calendarDaysAhead,
            showAllDay = settings.calendarShowAllDay
        )
    }

    val notifications by my.hana.meena.data.system.NotificationRepository.notificationsFlow.collectAsState(initial = emptyList())
    val notificationCounts = remember(notifications) {
        notifications.groupingBy { it.packageName }.eachCount()
    }

    val hijriDateStr = remember(currentDate, settings.showHijriDate, settings.hijriDayOffset) {
        if (settings.showHijriDate) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val hijrahDate = java.time.chrono.HijrahDate.now().plus(settings.hijriDayOffset.toLong(), java.time.temporal.ChronoUnit.DAYS)
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
                    "${formatter.format(hijrahDate).lowercase()} ah"
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    LaunchedEffect(settings.is24HourFormat, settings.dateFormatPattern, settings.showClockSeconds) {
        val timePattern = when {
            settings.is24HourFormat && settings.showClockSeconds -> "HH:mm:ss"
            settings.is24HourFormat -> "HH:mm"
            settings.showClockSeconds -> "h:mm:ss a"
            else -> "h:mm a"
        }
        val timeFormat = SimpleDateFormat(timePattern, Locale.getDefault())
        val dateFormat = SimpleDateFormat(settings.dateFormatPattern, Locale.getDefault())
        WeatherRepository.refresh(context)
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now).lowercase()
            currentDate = dateFormat.format(now).lowercase()
            telemetry = DeviceTelemetryHelper.getTelemetry(context)
            delay(if (settings.showClockSeconds) 1000L else 10000L)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        val todayWidgets = settings.widgetConfigs.entries
            .filter {
                val isTodayHub = it.value.hubId == "today" || it.value.hubId == "start" || it.value.hubId == "agenda" || it.value.hubId == "health"
                val isNotInboxWidget = it.key != "widget-conversations" && it.key != "widget-notifications"
                val isNotRemovedWidget = it.key != "widget-health-device"
                isTodayHub && isNotInboxWidget && isNotRemovedWidget && it.value.isEnabled
            }
            .sortedBy { it.value.position }

        items(todayWidgets.size, key = { todayWidgets[it].key }) { index ->
            val widgetId = todayWidgets[index].key
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .metroFeatherIn(index = index, reducedMotion = settings.reducedMotion)
            ) {
                when (widgetId) {
                "widget-clock-weather" -> {
                    // Clock and current weather row
                    val clockWeatherConfig = settings.widgetConfigs["widget-clock-weather"]
                    val tempUnit = clockWeatherConfig?.customSettings?.get("temp_unit") ?: "c"
                    val isFahrenheit = tempUnit.equals("f", ignoreCase = true)

                    val weatherData = (weatherState as? WidgetState.Ready)?.data ?:
                                      (weatherState as? WidgetState.Stale)?.data
                    val isWeatherStale = weatherState is WidgetState.Stale

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = my.hana.meena.theme.MeenaSpacing.s)
                            .metroClickable {
                                if (weatherState is WidgetState.NeedsSetup) {
                                    onRequestLocationPermission()
                                } else {
                                    WeatherRepository.refresh(context, force = true)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = currentTime,
                                style = MaterialTheme.typography.displayMedium,
                                color = MeenaTextWhite
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                val tempText = if (weatherData != null) {
                                    val tempVal = if (isFahrenheit) {
                                        (weatherData.current.temperature * 9f / 5f + 32f).roundToInt()
                                    } else {
                                        weatherData.current.temperature.roundToInt()
                                    }
                                    "$tempVal°"
                                } else {
                                    "--°"
                                }
                                Text(
                                    text = tempText,
                                    fontSize = 28.sp,
                                    color = MeenaTextWhite,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                val conditionText = when (weatherState) {
                                    is WidgetState.NeedsSetup -> "allow location"
                                    is WidgetState.Loading -> "checking..."
                                    is WidgetState.Ready, is WidgetState.Stale -> {
                                        val cond = weatherData?.current?.condition ?: "clear"
                                        if (isWeatherStale) "$cond (stale)" else cond
                                    }
                                    is WidgetState.Error -> "offline"
                                    else -> "needs setup"
                                }
                                Text(
                                    text = conditionText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = my.hana.meena.theme.MeenaSpacing.s),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (hijriDateStr != null) {
                                Text(
                                    text = hijriDateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted
                                )
                            }
                        }
                        // Info lines (content from AIO, D14): place, today's low and high, wind; next alarm.
                        val infoLine = run {
                            val parts = mutableListOf<String>()
                            ClockInfo.placeName(deviceLocation.locationName, deviceLocation.isGpsActive)?.let { parts += it }
                            val today = weatherData?.daily?.firstOrNull()
                            if (today != null) {
                                fun unit(c: Float) = if (isFahrenheit) (c * 9f / 5f + 32f).roundToInt() else c.roundToInt()
                                parts += stringResource(R.string.clock_low_high, unit(today.minTemp), unit(today.maxTemp))
                            }
                            if (weatherData != null) {
                                parts += stringResource(R.string.clock_wind, weatherData.current.windSpeed.roundToInt())
                            }
                            parts.joinToString("  ·  ")
                        }
                        if (infoLine.isNotBlank()) {
                            Text(
                                text = infoLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.s)
                            )
                        }
                        val nextAlarm = remember(currentTime) {
                            (context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager)
                                ?.nextAlarmClock?.triggerTime
                        }
                        if (nextAlarm != null) {
                            Text(
                                text = stringResource(
                                    R.string.clock_next_alarm,
                                    ClockInfo.formatAlarm(nextAlarm, System.currentTimeMillis(), settings.is24HourFormat)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                        // Next line: the sooner of the next event or prayer. Hidden when there is no data.
                        val nextUp = remember(currentTime, calendarState, jakimSchedule) {
                            val events = (calendarState as? my.hana.meena.data.system.CalendarState.Ready)?.events ?: emptyList()
                            my.hana.meena.data.system.NextUpCalculator.compute(System.currentTimeMillis(), events, jakimSchedule.slots)
                        }
                        if (nextUp != null) {
                            val nowMs = System.currentTimeMillis()
                            val nextText = if (nextUp.isPrayer) {
                                "${nextUp.title.lowercase()} ${my.hana.meena.data.system.NextUpCalculator.formatRemaining(nextUp.minutesUntil(nowMs))}"
                            } else {
                                val pattern = if (settings.is24HourFormat) "HH:mm" else "h:mm a"
                                "${nextUp.title.lowercase()} ${SimpleDateFormat(pattern, Locale.getDefault()).format(Date(nextUp.startMillis)).lowercase()}"
                            }
                            Text(
                                text = nextText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MeenaTextWhite,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.m)
                            )
                        }
                    }
                }

                "widget-weather-forecast" -> {
                    // 5-day Weather Forecast
                    val tempUnit = settings.widgetConfigs["widget-clock-weather"]?.customSettings?.get("temp_unit") ?: "c"
                    val isFahrenheit = tempUnit.equals("f", ignoreCase = true)

                    ForecastTiles(
                        state = weatherState,
                        isFahrenheit = isFahrenheit,
                        onNavigateToDetail = onOpenWeatherDetail,
                        onRequestLocation = onRequestLocationPermission,
                        onRetry = { WeatherRepository.refresh(context, force = true) }
                    )
                }
                
                "widget-favourites" -> {
                    my.hana.meena.ui.components.FavouriteTiles(
                        pinnedAppIds = settings.pinnedAppIds,
                        notificationCounts = notificationCounts,
                        isMonochrome = settings.isMonochromeFavourites,
                        onLaunchApp = onLaunchPackage,
                        onOpenApps = onOpenApps,
                        onTogglePin = onTogglePin,
                        onMoveApp = onReorderPin
                    )
                }

                "widget-solat" -> {
                    // Waktu Solat JAKIM with dynamic celestial arc
                    CollapsibleWidget(
                        title = "waktu solat · jakim",
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = jakimSchedule.nextPrayerLabel.lowercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        if (jakimSchedule.slots.isEmpty()) {
                            // Honest state instead of an empty card.
                            Text(
                                text = when (solatState) {
                                    is my.hana.meena.ui.widgets.WidgetState.Loading -> "loading prayer times..."
                                    is my.hana.meena.ui.widgets.WidgetState.Error -> "couldn't load prayer times. check your connection."
                                    else -> "no prayer times for this place yet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MeenaTextMuted
                            )
                        } else {
                            DynamicSolatCelestialCard(
                                schedule = jakimSchedule,
                                onZoneChanged = onUpdateSolatZone
                            )
                        }
                    }
                }

                "widget-calendar-month" -> {
                    // Device calendar events & agenda
                    val calEvents = (calendarState as? CalendarState.Ready)?.events ?: emptyList()
                    val isNeedsPermission = calendarState is CalendarState.NeedsPermission
                    CollapsibleWidget(
                        title = "calendar",
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = when {
                                    isNeedsPermission -> "needs setup"
                                    calEvents.isEmpty() -> "all quiet"
                                    else -> "${calEvents.size} upcoming"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNeedsPermission) MeenaTextMuted else MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        when {
                            isNeedsPermission -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .metroClickable { onRequestCalendarPermission() }
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "want your schedule here? tap to allow calendar access.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "allow",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            calEvents.isEmpty() -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "all quiet · no upcoming events",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                    Text(
                                        text = "+ event",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .metroClickable { DeviceCalendarRepository.openNewEvent(context) }
                                            .padding(vertical = my.hana.meena.theme.MeenaSpacing.m, horizontal = my.hana.meena.theme.MeenaSpacing.s)
                                    )
                                }
                            }
                            else -> {
                                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                    calEvents.take(4).forEach { event ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .metroClickable { DeviceCalendarRepository.openEventDetail(context, event.id) }
                                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = event.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Normal,
                                                    color = MeenaTextWhite
                                                )
                                                Text(
                                                    text = buildString {
                                                        append(event.formattedDate)
                                                        append(" · ")
                                                        append(event.formattedTime)
                                                        if (event.location.isNotBlank()) {
                                                            append(" · ")
                                                            append(event.location)
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MeenaTextMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "+ new event",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .metroClickable { DeviceCalendarRepository.openNewEvent(context) }
                                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.m, horizontal = my.hana.meena.theme.MeenaSpacing.s)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "widget-tasks" -> {
                    // Agenda: events first, then tasks
                    val isNeedsPermission = calendarState is CalendarState.NeedsPermission
                    val calEvents = (calendarState as? CalendarState.Ready)?.events ?: emptyList()
                    val (timedEvents, tasks) = calEvents.partition { !it.isAllDay }
                    val agendaItems = timedEvents + tasks
                    val agendaConfig = settings.widgetConfigs[widgetId]
                    val maxItems = agendaConfig?.maxLinesOrItems ?: 5

                    CollapsibleWidget(
                        title = "agenda",
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = when {
                                    isNeedsPermission -> "needs setup"
                                    agendaItems.isEmpty() -> "all quiet"
                                    else -> {
                                        val todayCount = agendaItems.count { it.formattedDate == "today" }
                                        if (todayCount > 0) "$todayCount scheduled today" else "${agendaItems.size} upcoming"
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNeedsPermission || agendaItems.isEmpty()) MeenaTextMuted else MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        when {
                            isNeedsPermission -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .metroClickable { onRequestCalendarPermission() }
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "want your schedule here? tap to allow calendar access.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "allow",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            agendaItems.isEmpty() -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "all quiet · no upcoming events",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                    Text(
                                        text = "+ event",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .metroClickable { DeviceCalendarRepository.openNewEvent(context) }
                                            .padding(vertical = my.hana.meena.theme.MeenaSpacing.m, horizontal = my.hana.meena.theme.MeenaSpacing.s)
                                    )
                                }
                            }
                            else -> {
                                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                    agendaItems.take(maxItems).forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .metroClickable { DeviceCalendarRepository.openEventDetail(context, item.id) }
                                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MeenaTextWhite
                                                )
                                                Text(
                                                    text = buildString {
                                                        append(item.formattedDate)
                                                        append(" · ")
                                                        append(item.formattedTime)
                                                        if (item.location.isNotBlank()) {
                                                            append(" · ")
                                                            append(item.location)
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MeenaTextMuted
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = my.hana.meena.theme.MeenaSpacing.s),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "+ new event",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .metroClickable { DeviceCalendarRepository.openNewEvent(context) }
                                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.m, horizontal = my.hana.meena.theme.MeenaSpacing.s)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "widget-notes" -> {
                    // Quick notes
                    CollapsibleWidget(
                        title = "quick notes",
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = if (quickNotes.isEmpty()) "empty" else "${quickNotes.size} notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        if (quickNotes.isEmpty()) {
                            Text(
                                text = "no notes pinned. write a note in settings.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                quickNotes.forEachIndexed { index, note ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MeenaTextWhite,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "remove",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextMuted,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .metroClickable { quickNotes.removeAt(index) }
                                                .padding(horizontal = my.hana.meena.theme.MeenaSpacing.s, vertical = my.hana.meena.theme.MeenaSpacing.m)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "widget-health-telemetry" -> {
                    // Merged health & activity widget
                    CollapsibleWidget(
                        title = "health",
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = when (healthState) {
                                    is WidgetState.NeedsSetup -> "connect health"
                                    is WidgetState.Ready, is WidgetState.Stale -> {
                                        if (airIndex.aqiValue > 0) {
                                            "${fitTelemetry.stepsToday} steps · aqi ${airIndex.aqiValue}"
                                        } else {
                                            "${fitTelemetry.stepsToday} steps"
                                        }
                                    }
                                    else -> "no data"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (healthState is WidgetState.Ready) MeenaTextWhite else MeenaTextMuted
                            )
                        }
                    ) {
                        when (healthState) {
                            is WidgetState.NeedsSetup, is WidgetState.Empty -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .metroClickable {
                                            HealthRepository.openHealthConnectSettings(context)
                                        }
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "connect health connect to track steps, sleep and activity.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "connect",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            is WidgetState.Ready, is WidgetState.Stale -> {
                                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("steps", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        Text(
                                            text = "${fitTelemetry.stepsToday} / ${fitTelemetry.stepGoal}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextWhite
                                        )
                                    }
                                    MetroLineProgress(
                                        progress = if (fitTelemetry.stepGoal > 0) (fitTelemetry.stepsToday.toFloat() / fitTelemetry.stepGoal.toFloat()).coerceIn(0f, 1f) else 0f,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MeenaProfitGreen,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        height = 3.dp
                                    )

                                    if (fitTelemetry.distanceKm > 0.0 || fitTelemetry.activeCaloriesKcal > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (fitTelemetry.distanceKm > 0.0) {
                                                Text(
                                                    text = "${String.format(java.util.Locale.US, "%.1f", fitTelemetry.distanceKm)} km",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MeenaTextMuted
                                                )
                                            }
                                            if (fitTelemetry.activeCaloriesKcal > 0) {
                                                Text(
                                                    text = "${fitTelemetry.activeCaloriesKcal} kcal",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MeenaTextMuted
                                                )
                                            }
                                        }
                                    }

                                    if (fitTelemetry.sleepHoursFormatted.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("sleep", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                            Text(
                                                text = fitTelemetry.sleepHoursFormatted,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MeenaTextWhite
                                            )
                                        }
                                    }

                                    // Merged Air quality
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = my.hana.meena.theme.MeenaSpacing.s)
                                            .metroClickable { HealthRepository.openAirQualityDetails(context) }
                                    ) {
                                        Text("air quality · ${airIndex.stationName.ifBlank { "local" }.lowercase()}", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                                        ) {
                                            Text(
                                                text = "${airIndex.aqiValue}",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = try { Color(airIndex.aqiColorHex.toColorInt()) } catch (_: Exception) { MeenaTextWhite }
                                            )
                                            Text(
                                                text = airIndex.aqiStatus.lowercase(),
                                                fontSize = 12.sp,
                                                color = try { Color(airIndex.aqiColorHex.toColorInt()) } catch (_: Exception) { MeenaTextMuted }
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                "widget-health-aqi" -> {
                    // Air quality
                    CollapsibleWidget(
                        title = "air quality",
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = "${airIndex.stationName.ifBlank { "local" }.lowercase()} · aqi ${airIndex.aqiValue}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s),
                            modifier = Modifier
                                .fillMaxWidth()
                                .metroClickable { HealthRepository.openAirQualityDetails(context) }
                        ) {
                            Text(airIndex.stationName.ifBlank { "local station" }.lowercase(), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                            ) {
                                Text(
                                    text = "${airIndex.aqiValue}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = try { Color(airIndex.aqiColorHex.toColorInt()) } catch (_: Exception) { MeenaTextWhite }
                                )
                                Text(
                                    text = airIndex.aqiStatus.lowercase(),
                                    fontSize = 13.sp,
                                    color = try { Color(airIndex.aqiColorHex.toColorInt()) } catch (_: Exception) { MeenaTextMuted }
                                )
                            }
                            if (airIndex.advisory.isNotBlank()) {
                                Text(
                                    text = airIndex.advisory.lowercase(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MeenaTextSecondary
                                )
                            }
                        }
                    }
                }

                "widget-map-radar" -> {
                    // Map radar
                    OpenMapsRadarWidget()
                }

                "widget-device-telemetry" -> {
                    // Phone status
                    CollapsibleWidget(
                        title = "phone",
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = "battery ${telemetry.batteryPercent}% · ${String.format(Locale.US, "%.1f gb free", (telemetry.totalRamMb - telemetry.usedRamMb) / 1024.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                            // Battery
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("battery", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Text(
                                        text = "${telemetry.batteryPercent}% · ${if (telemetry.isCharging) "charging" else "on battery"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (telemetry.batteryPercent <= 20) MaterialTheme.colorScheme.error else MeenaTextWhite
                                    )
                                }
                            }

                            // Memory
                            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("memory", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Text(
                                        text = String.format(Locale.US, "%.1f gb free", (telemetry.totalRamMb - telemetry.usedRamMb) / 1024.0),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextWhite
                                    )
                                }
                                MetroLineProgress(
                                    progress = (telemetry.ramPercent / 100f).coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    height = 2.dp
                                )
                            }

                            // Storage
                            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("storage", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Text(
                                        text = String.format(Locale.US, "%.1f gb free", telemetry.totalStorageGb - telemetry.usedStorageGb),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextWhite
                                    )
                                }
                                MetroLineProgress(
                                    progress = (telemetry.storagePercent / 100f).coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MeenaTextWhite,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    height = 2.dp
                                )
                            }

                            // Screen
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("screen", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Text("${telemetry.screenBrightnessPercent}%", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                            }
                        }
                    }
                }
            }
        }
    }

        // Quick Actions Row (phone, messages, email, camera, calc)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.m),
                verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
            ) {
                Text(
                    text = "quick actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    val quickActions = listOf(
                        Triple("phone", MaterialTheme.colorScheme.primary, onOpenDialer),
                        Triple("messages", MaterialTheme.colorScheme.primary, onOpenMessages),
                        Triple("email", MaterialTheme.colorScheme.primary, onOpenEmail),
                        Triple("camera", MaterialTheme.colorScheme.primary, onOpenCamera),
                        Triple("calc", MaterialTheme.colorScheme.primary, onOpenCalculator)
                    )
                    quickActions.forEach { (label, bg, action) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .background(bg, RoundedCornerShape(0.dp))
                                .metroClickable(onClick = action),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = MeenaTextWhite,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Pinned App Tiles (WP8.1 Metro Live Tiles)
        if (settings.pinnedAppIds.isNotEmpty()) {
            val currentAppsVersion = appsVersion
            item {
                Text(
                    text = "pinned",
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite,
                    modifier = Modifier.padding(top = my.hana.meena.theme.MeenaSpacing.m, bottom = my.hana.meena.theme.MeenaSpacing.s)
                )
            }
            val chunked = settings.pinnedAppIds.chunked(3)
            items(chunked) { rowApps ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                    horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    rowApps.forEach { pkgId ->
                        // Read currentAppsVersion so recomposition triggers when cache is loaded
                        if (currentAppsVersion >= 0) { }
                        val app = InstalledAppsRepository.getCachedApp(pkgId)
                        val label = app?.label?.lowercase() ?: pkgId.substringAfterLast('.').lowercase()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                                .metroClickable { onLaunchPackage(pkgId) }
                                .padding(my.hana.meena.theme.MeenaSpacing.s)
                        ) {
                            if (app?.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap,
                                    contentDescription = label,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(Alignment.Center)
                                )
                            }
                            Text(
                                text = label,
                                color = MeenaTextWhite,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.BottomStart),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(3 - rowApps.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

