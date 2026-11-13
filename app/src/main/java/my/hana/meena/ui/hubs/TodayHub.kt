package my.hana.meena.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.pluralStringResource
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
    onRequestBluetoothPermission: () -> Unit = {},
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
    val launchVersion by my.hana.meena.data.system.AppLaunchStats.version.collectAsState()
    val bluetoothState by my.hana.meena.data.system.BluetoothDevicesRepository.state.collectAsState()
    val wikipediaState by my.hana.meena.data.system.WikipediaRepository.state.collectAsState()
    val isBluetoothWidgetOn = settings.widgetConfigs["widget-bluetooth-devices"]?.isEnabled == true
    val isWikipediaWidgetOn = settings.widgetConfigs["widget-wikipedia"]?.isEnabled == true
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
                    my.hana.meena.data.system.MalaysianEnglish.hijriDate(
                        hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH),
                        hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                        hijrahDate.get(java.time.temporal.ChronoField.YEAR)
                    )
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    // Connection changes have no callback here, so poll gently while the widget is on screen.
    LaunchedEffect(isBluetoothWidgetOn) {
        while (isBluetoothWidgetOn) {
            my.hana.meena.data.system.BluetoothDevicesRepository.refresh(context)
            delay(30_000L)
        }
    }

    LaunchedEffect(isWikipediaWidgetOn) {
        if (isWikipediaWidgetOn) my.hana.meena.data.system.WikipediaRepository.refresh(context)
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
                                color = MeenaTextWhite,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier
                                    .padding(start = my.hana.meena.theme.MeenaSpacing.m)
                                    .widthIn(max = 128.dp)
                            ) {
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
                                    is WidgetState.NeedsSetup -> stringResource(R.string.state_allow_location)
                                    is WidgetState.Loading -> stringResource(R.string.state_checking)
                                    is WidgetState.Ready, is WidgetState.Stale -> {
                                        val cond = weatherData?.current?.condition.orEmpty()
                                        if (isWeatherStale) stringResource(R.string.weather_last_known, cond) else cond
                                    }
                                    is WidgetState.Error -> stringResource(R.string.state_offline)
                                    else -> stringResource(R.string.state_needs_setup)
                                }
                                Text(
                                    text = conditionText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                    val tempUnit = settings.widgetConfigs["widget-clock-weather"]?.customSettings?.get("temp_unit") ?: "c"
                    val isFahrenheit = tempUnit.equals("f", ignoreCase = true)
                    val tomorrow = ((weatherState as? WidgetState.Ready)?.data ?: (weatherState as? WidgetState.Stale)?.data)
                        ?.daily?.getOrNull(1)

                    CollapsibleWidget(
                        title = stringResource(R.string.forecast_title),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            if (tomorrow != null) {
                                Text(
                                    text = stringResource(R.string.forecast_summary, tomorrow.dayLabel, tomorrow.condition),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted
                                )
                            }
                        }
                    ) {
                        ForecastTiles(
                            state = weatherState,
                            isFahrenheit = isFahrenheit,
                            onNavigateToDetail = onOpenWeatherDetail,
                            onRequestLocation = onRequestLocationPermission,
                            onRetry = { WeatherRepository.refresh(context, force = true) }
                        )
                        // Rain in the next few hours (a forecast, not radar), and the real radar one tap away.
                        val rainData =
                            (weatherState as? WidgetState.Ready)?.data ?: (weatherState as? WidgetState.Stale)?.data
                        val rainOutlook = remember(rainData, currentTime) {
                            rainData?.let {
                                my.hana.meena.data.system.RainOutlookCalculator.compute(
                                    it.rainHours,
                                    System.currentTimeMillis()
                                )
                            }
                        }
                        if (rainOutlook != null) {
                            val hourFormat =
                                SimpleDateFormat(if (settings.is24HourFormat) "HH:mm" else "h a", Locale.UK)
                            val rainText = when (rainOutlook) {
                                is my.hana.meena.data.system.RainOutlook.Dry -> stringResource(
                                    R.string.rain_dry,
                                    my.hana.meena.data.system.RainOutlookCalculator.HOURS
                                )

                                is my.hana.meena.data.system.RainOutlook.AllWet -> stringResource(
                                    R.string.rain_all,
                                    my.hana.meena.data.system.RainOutlookCalculator.HOURS
                                )

                                is my.hana.meena.data.system.RainOutlook.WetUntil -> stringResource(
                                    R.string.rain_until,
                                    hourFormat.format(Date(rainOutlook.untilMillis)).lowercase(Locale.UK)
                                )

                                is my.hana.meena.data.system.RainOutlook.WetFrom -> stringResource(
                                    R.string.rain_from,
                                    hourFormat.format(Date(rainOutlook.fromMillis)).lowercase(Locale.UK)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable { my.hana.meena.data.system.RainRadar.open(context) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.rain_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MeenaTextWhite,
                                    modifier = Modifier.width(124.dp)
                                )
                                Text(
                                    text = rainText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MeenaTextMuted,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = stringResource(R.string.rain_radar),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = my.hana.meena.theme.MeenaSpacing.m)
                                )
                            }
                        }
                        // Air quality belongs with the weather (it used to repeat under health and on its own).
                        if (airIndex.aqiValue > 0) {
                            val aqiColor = try { Color(airIndex.aqiColorHex.toColorInt()) } catch (_: Exception) { MeenaTextWhite }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable { HealthRepository.openAirQualityDetails(context) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.forecast_air),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MeenaTextWhite,
                                    modifier = Modifier.width(124.dp)
                                )
                                Text(
                                    text = "${airIndex.aqiValue}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = aqiColor
                                )
                                Text(
                                    text = airIndex.aqiStatus.lowercase(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MeenaTextMuted,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = my.hana.meena.theme.MeenaSpacing.m)
                                )
                            }
                        }
                    }
                }
                
                "widget-favourites" -> {
                    val liveOn = settings.liveTiles
                    val andMorePattern = stringResource(R.string.live_tile_and_more)
                    val alarmPattern = stringResource(R.string.live_tile_alarm)
                    val words = my.hana.meena.data.system.LiveTileWords(
                        new = stringResource(R.string.live_tile_new),
                        andMore = { name, more -> String.format(Locale.UK, andMorePattern, name, more) },
                        alarm = { day -> String.format(Locale.UK, alarmPattern, day) },
                        onNow = stringResource(R.string.live_tile_on_now)
                    )
                    // Apps that open as the calendar; visible to Meena through its launcher query.
                    val calendarPackages = remember {
                        runCatching {
                            val selector = android.content.Intent.makeMainSelectorActivity(
                                android.content.Intent.ACTION_MAIN, android.content.Intent.CATEGORY_APP_CALENDAR
                            )
                            context.packageManager.queryIntentActivities(selector, 0)
                                .map { it.activityInfo.packageName }.toSet()
                        }.getOrDefault(emptySet())
                    }
                    val nextAlarm = remember(currentTime) {
                        (context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager)
                            ?.nextAlarmClock?.let {
                                my.hana.meena.data.system.NextAlarm(
                                    it.triggerTime,
                                    it.showIntent?.creatorPackage
                                )
                            }
                    }
                    val calEvents = (calendarState as? CalendarState.Ready)?.events ?: emptyList()
                    val liveFaces = remember(
                        liveOn,
                        settings.pinnedAppIds,
                        notifications,
                        calEvents,
                        nextAlarm,
                        currentTime,
                        settings.liveTileNames,
                        settings.is24HourFormat
                    ) {
                        if (!liveOn) emptyMap() else settings.pinnedAppIds.associateWith { pkg ->
                            my.hana.meena.data.system.LiveTiles.facesFor(
                                packageName = pkg,
                                notifications = notifications,
                                calendarPackages = calendarPackages,
                                events = calEvents,
                                alarm = nextAlarm,
                                nowMillis = System.currentTimeMillis(),
                                showNames = settings.liveTileNames,
                                is24Hour = settings.is24HourFormat,
                                words = words
                            )
                        }
                    }
                    my.hana.meena.ui.components.FavouriteTiles(
                        pinnedAppIds = settings.pinnedAppIds,
                        notificationCounts = notificationCounts,
                        isMonochrome = settings.isMonochromeFavourites,
                        onLaunchApp = onLaunchPackage,
                        onOpenApps = onOpenApps,
                        onTogglePin = onTogglePin,
                        onMoveApp = onReorderPin,
                        liveFaces = liveFaces,
                        animateLive = liveOn && !settings.reducedMotion
                    )
                }

                "widget-frequent-apps" -> {
                    val limit = settings.widgetConfigs[widgetId]?.maxLinesOrItems ?: 8
                    val frequentApps = remember(launchVersion, appsVersion, settings.pinnedAppIds, limit) {
                        val stats = my.hana.meena.data.system.AppLaunchStats.load(context)
                        val installed = stats.keys.filter { InstalledAppsRepository.getCachedApp(it) != null }.toSet()
                        my.hana.meena.data.system.AppLaunchStats.rank(stats, installed, settings.pinnedAppIds.toSet(), limit)
                    }
                    CollapsibleWidget(
                        title = stringResource(R.string.widget_frequent_apps_title),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) }
                    ) {
                        my.hana.meena.ui.components.FrequentAppTiles(
                            packageNames = frequentApps,
                            isMonochrome = settings.isMonochromeFavourites,
                            onLaunchApp = onLaunchPackage
                        )
                    }
                }

                "widget-bluetooth-devices" -> {
                    CollapsibleWidget(
                        title = stringResource(R.string.widget_devices_title),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) }
                    ) {
                        when (val state = bluetoothState) {
                            is WidgetState.NeedsSetup -> Text(
                                text = stringResource(R.string.devices_needs_setup),
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable { onRequestBluetoothPermission() }
                            )
                            is WidgetState.Loading -> Text(
                                text = stringResource(R.string.devices_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted
                            )
                            is WidgetState.Empty -> Text(
                                text = stringResource(R.string.devices_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted
                            )
                            is WidgetState.Error -> Text(
                                text = "${state.message}. ${stringResource(R.string.tap_to_retry)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable { my.hana.meena.data.system.BluetoothDevicesRepository.refresh(context) }
                            )
                            is WidgetState.Ready, is WidgetState.Stale -> {
                                val devices = (state as? WidgetState.Ready)?.data ?: (state as WidgetState.Stale).data
                                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                    devices.forEach { device ->
                                        val companion = settings.healthCompanionPackage.takeIf {
                                            device.battery == null && device.kind == my.hana.meena.data.system.BluetoothDevicesRepository.KIND_WEARABLE && it.isNotBlank()
                                        }
                                        val kindText = when (device.kind) {
                                            my.hana.meena.data.system.BluetoothDevicesRepository.KIND_AUDIO -> stringResource(
                                                R.string.devices_kind_audio
                                            )

                                            my.hana.meena.data.system.BluetoothDevicesRepository.KIND_WEARABLE -> stringResource(
                                                R.string.devices_kind_wearable
                                            )

                                            else -> stringResource(R.string.devices_kind_other)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .then(if (companion != null) Modifier.metroClickable {
                                                    onLaunchPackage(
                                                        companion
                                                    )
                                                } else Modifier),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = device.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MeenaTextWhite
                                                )
                                                Text(
                                                    text = if (companion != null) {
                                                        stringResource(
                                                            R.string.devices_battery_in_app,
                                                            kindText,
                                                            settings.healthCompanionName.ifBlank { stringResource(R.string.devices_its_app) }
                                                                .lowercase()
                                                        )
                                                    } else kindText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (companion != null) MaterialTheme.colorScheme.primary else MeenaTextMuted
                                                )
                                            }
                                            device.battery?.let { level ->
                                                val batteryDescription = stringResource(
                                                    R.string.devices_battery_description,
                                                    device.name,
                                                    level
                                                )
                                                Text(
                                                    text = stringResource(R.string.devices_battery_percent, level),
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Light,
                                                    color = if (level <= 15) MaterialTheme.colorScheme.error else MeenaTextWhite,
                                                    modifier = Modifier.semantics {
                                                        contentDescription = batteryDescription
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "widget-wikipedia" -> {
                    CollapsibleWidget(
                        title = stringResource(R.string.widget_wikipedia_title),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) }
                    ) {
                        when (val state = wikipediaState) {
                            is WidgetState.Ready, is WidgetState.Stale -> {
                                val article = (state as? WidgetState.Ready)?.data ?: (state as WidgetState.Stale).data
                                val updatedAt = (state as? WidgetState.Ready)?.updatedAt ?: (state as WidgetState.Stale).updatedAt
                                val openFailedMessage = stringResource(R.string.error_generic)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .metroClickable {
                                            try {
                                                context.startActivity(
                                                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(article.url))
                                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            } catch (_: Exception) {
                                                my.hana.meena.ui.components.MetroMessage.show(openFailedMessage)
                                            }
                                        },
                                    verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                                ) {
                                    Text(
                                        text = article.title.lowercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MeenaTextWhite
                                    )
                                    if (article.description.isNotBlank()) {
                                        Text(
                                            text = article.description.lowercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextMuted
                                        )
                                    }
                                    Text(
                                        text = article.extract,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MeenaTextWhite,
                                        maxLines = 4,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.wikipedia_source, my.hana.meena.ui.widgets.updatedAgoText(updatedAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.wikipedia_another),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .metroClickable { my.hana.meena.data.system.WikipediaRepository.refresh(context, force = true) }
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
                                )
                            }
                            is WidgetState.Error -> Text(
                                text = "${state.message}. ${stringResource(R.string.tap_to_retry)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable { my.hana.meena.data.system.WikipediaRepository.refresh(context, force = true) }
                            )
                            else -> Text(
                                text = stringResource(R.string.wikipedia_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted
                            )
                        }
                    }
                }

                "widget-solat" -> {
                    val solatConfig = settings.widgetConfigs[widgetId]
                    CollapsibleWidget(
                        title = stringResource(R.string.solat_title),
                        isCollapsed = solatConfig?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            val nextPrayer = remember(currentTime, jakimSchedule) {
                                my.hana.meena.data.system.NextUpCalculator.compute(System.currentTimeMillis(), emptyList(), jakimSchedule.slots)
                            }
                            Text(
                                text = nextPrayer?.let {
                                    "${it.title.lowercase()} ${my.hana.meena.data.system.NextUpCalculator.formatRemaining(it.minutesUntil(System.currentTimeMillis()))}"
                                } ?: stringResource(R.string.settings_needs_okay),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        if (jakimSchedule.slots.isEmpty()) {
                            // Honest state instead of an empty card.
                            Text(
                                text = when (solatState) {
                                    is my.hana.meena.ui.widgets.WidgetState.Loading -> stringResource(R.string.solat_loading)
                                    is my.hana.meena.ui.widgets.WidgetState.Error -> stringResource(R.string.solat_load_failed)
                                    else -> stringResource(R.string.solat_no_times)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MeenaTextMuted
                            )
                        } else {
                            DynamicSolatCelestialCard(
                                schedule = jakimSchedule,
                                onZoneChanged = onUpdateSolatZone,
                                showSunPath = solatConfig?.customSettings?.get("show_celestial_arc") == "true",
                                showSyuruk = solatConfig?.customSettings?.get("show_syuruk_imsak") != "false"
                            )
                        }
                    }
                }

                "widget-calendar-month" -> {
                    // Month grid with event dots (AIO content, D14); events themselves are listed by agenda.
                    val calEvents = (calendarState as? CalendarState.Ready)?.events ?: emptyList()
                    val isNeedsPermission = calendarState is CalendarState.NeedsPermission
                    CollapsibleWidget(
                        title = stringResource(R.string.calendar_month_title),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = when {
                                    isNeedsPermission -> stringResource(R.string.state_needs_setup)
                                    calEvents.isEmpty() -> stringResource(R.string.state_all_quiet)
                                    else -> pluralStringResource(R.plurals.events_upcoming, calEvents.size, calEvents.size)
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
                                        text = stringResource(R.string.calendar_allow_prompt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_allow),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            else -> {
                                // Month grid only; the event list lives in the agenda widget.
                                my.hana.meena.ui.components.MonthCalendarGrid(
                                    nowMillis = System.currentTimeMillis(),
                                    eventStartTimes = calEvents.map { it.startTime }
                                )
                            }
                        }
                    }
                }

                "widget-tasks" -> {
                    // Your week: the 7-day strip, then events in time order.
                    val isNeedsPermission = calendarState is CalendarState.NeedsPermission
                    val calEvents = (calendarState as? CalendarState.Ready)?.events ?: emptyList()
                    val agendaItems = calEvents.sortedBy { it.startTime }
                    val agendaConfig = settings.widgetConfigs[widgetId]
                    val maxItems = agendaConfig?.maxLinesOrItems ?: 5
                    val showStrip = agendaConfig?.customSettings?.get("show_week_strip") != "false"
                    val stripDays = remember(currentDate, calEvents, settings.calendarDaysAhead) {
                        my.hana.meena.data.system.WeekStripBuilder.build(
                            nowMillis = System.currentTimeMillis(),
                            events = calEvents.map { my.hana.meena.data.system.StripEvent(it.startTime, it.isAllDay) },
                            knownDays = settings.calendarDaysAhead
                        )
                    }

                    CollapsibleWidget(
                        title = stringResource(R.string.agenda_title),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = when {
                                    isNeedsPermission -> stringResource(R.string.state_needs_setup)
                                    agendaItems.isEmpty() -> stringResource(R.string.state_all_quiet)
                                    else -> {
                                        val todayCount = agendaItems.count { it.formattedDate == "today" }
                                        if (todayCount > 0) pluralStringResource(R.plurals.events_today, todayCount, todayCount) else pluralStringResource(R.plurals.events_upcoming, agendaItems.size, agendaItems.size)
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNeedsPermission || agendaItems.isEmpty()) MeenaTextMuted else MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        if (showStrip) {
                            my.hana.meena.ui.components.WeekStrip(
                                days = stripDays,
                                showEvents = calendarState is CalendarState.Ready,
                                onDayClick = { DeviceCalendarRepository.openDay(context, it.startMillis) },
                                modifier = Modifier.padding(bottom = my.hana.meena.theme.MeenaSpacing.m)
                            )
                        }
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
                                        text = stringResource(R.string.calendar_allow_prompt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_allow),
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
                                        text = stringResource(R.string.agenda_empty),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                    Text(
                                        text = stringResource(R.string.agenda_add_event),
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
                                            text = stringResource(R.string.action_new_event),
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
                        title = stringResource(R.string.widget_name_notes),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = if (quickNotes.isEmpty()) stringResource(R.string.notes_empty_short) else pluralStringResource(R.plurals.notes_count, quickNotes.size, quickNotes.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        if (quickNotes.isEmpty()) {
                            Text(
                                text = stringResource(R.string.notes_empty),
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
                                            text = stringResource(R.string.action_remove),
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
                        title = stringResource(R.string.settings_health_title),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            Text(
                                text = when (healthState) {
                                    is WidgetState.NeedsSetup -> stringResource(R.string.health_connect_short)
                                    is WidgetState.Ready, is WidgetState.Stale -> pluralStringResource(R.plurals.health_steps_count, fitTelemetry.stepsToday, fitTelemetry.stepsToday)
                                    else -> stringResource(R.string.health_no_data)
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
                                        text = stringResource(R.string.health_connect_prompt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.health_connect_action),
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
                                        Text(stringResource(R.string.health_steps_label), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
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
                                                    text = stringResource(R.string.health_distance_km, String.format(java.util.Locale.US, "%.1f", fitTelemetry.distanceKm)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MeenaTextMuted
                                                )
                                            }
                                            if (fitTelemetry.activeCaloriesKcal > 0) {
                                                Text(
                                                    text = pluralStringResource(R.plurals.health_calories_kcal, fitTelemetry.activeCaloriesKcal, fitTelemetry.activeCaloriesKcal),
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
                                            Text(stringResource(R.string.health_sleep_label), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                            Text(
                                                text = fitTelemetry.sleepHoursFormatted,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MeenaTextWhite
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
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
                        title = stringResource(R.string.widget_name_phone),
                        isCollapsed = settings.widgetConfigs[widgetId]?.isCollapsed ?: false,
                        onToggleCollapse = { onToggleWidgetCollapse(widgetId, it) },
                        collapsedSummary = {
                            val unknown = stringResource(R.string.phone_unknown)
                            val batteryPart = telemetry.batteryPercent?.let { stringResource(R.string.phone_summary_battery, it) }
                            val memoryPart = telemetry.freeMemoryGb?.let { stringResource(R.string.phone_free_gb, String.format(Locale.US, "%.1f", it)) }
                            Text(
                                text = when {
                                    batteryPart != null && memoryPart != null -> stringResource(R.string.settings_status_pair, batteryPart, memoryPart)
                                    else -> batteryPart ?: memoryPart ?: unknown
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        val unknown = stringResource(R.string.phone_unknown)
                        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                            // Battery
                            Column {
                                Text(stringResource(R.string.phone_battery_label), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                val battery = telemetry.batteryPercent
                                Text(
                                    text = if (battery == null) unknown
                                    else stringResource(if (telemetry.isCharging) R.string.phone_battery_charging else R.string.phone_battery_unplugged, battery),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (battery != null && battery <= 20) MaterialTheme.colorScheme.error else MeenaTextWhite
                                )
                            }

                            // Memory, then storage: free space, with a bar only when the phone reports it
                            listOf(
                                Triple(R.string.phone_memory_label, telemetry.freeMemoryGb, telemetry.memoryUsedPercent),
                                Triple(R.string.phone_storage_label, telemetry.freeStorageGb, telemetry.storageUsedPercent)
                            ).forEach { (label, freeGb, usedPercent) ->
                                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        Text(
                                            text = freeGb?.let { stringResource(R.string.phone_free_gb, String.format(Locale.US, "%.1f", it)) } ?: unknown,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextWhite
                                        )
                                    }
                                    if (usedPercent != null) {
                                        MetroLineProgress(
                                            progress = (usedPercent / 100f).coerceIn(0f, 1f),
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            height = 2.dp
                                        )
                                    }
                                }
                            }

                            // Screen
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.phone_screen_label), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Text(
                                    text = telemetry.screenBrightnessPercent?.let { stringResource(R.string.percent_value, it) } ?: unknown,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextWhite
                                )
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
                    text = stringResource(R.string.quick_actions_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MeenaTextWhite
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    val quickActions = listOf(
                        Triple(stringResource(R.string.quick_action_phone), MaterialTheme.colorScheme.primary, onOpenDialer),
                        Triple(stringResource(R.string.quick_action_messages), MaterialTheme.colorScheme.primary, onOpenMessages),
                        Triple(stringResource(R.string.quick_action_email), MaterialTheme.colorScheme.primary, onOpenEmail),
                        Triple(stringResource(R.string.quick_action_camera), MaterialTheme.colorScheme.primary, onOpenCamera),
                        Triple(stringResource(R.string.quick_action_calculator), MaterialTheme.colorScheme.primary, onOpenCalculator)
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
                                color = my.hana.meena.theme.MeenaOnAccent,
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
                    text = stringResource(R.string.pinned_title),
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
                                color = my.hana.meena.theme.MeenaOnAccent,
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

