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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlin.math.roundToInt
import my.hana.meena.data.system.WeatherRepository
import my.hana.meena.ui.widgets.WidgetState
import my.hana.meena.data.system.GeminiBriefRepository
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
    onOpenSettings: () -> Unit = {}
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
    val deviceInfo = healthData.deviceInfo
    val fitTelemetry = healthData.fitTelemetry
    val airIndex = healthData.airIndex

    val weatherState by WeatherRepository.state.collectAsState()
    val calendarState by DeviceCalendarRepository.state.collectAsState()
    val briefState by GeminiBriefRepository.state.collectAsState()
    val quickNotes = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        DeviceCalendarRepository.refresh(context)
        WeatherRepository.refresh(context)
        GeminiBriefRepository.refresh(context, settings)
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).lowercase()
            telemetry = DeviceTelemetryHelper.getTelemetry(context)
            delay(10000L)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        val todayWidgets = settings.widgetConfigs.entries
            .filter {
                val isTodayHub = it.value.hubId == "today" || it.value.hubId == "start" || it.value.hubId == "agenda" || it.value.hubId == "health"
                val isNotInboxWidget = it.key != "widget-conversations" && it.key != "widget-notifications"
                isTodayHub && isNotInboxWidget && it.value.isEnabled
            }
            .sortedBy { it.value.position }

        items(todayWidgets.size, key = { todayWidgets[it].key }) { index ->
            val widgetId = todayWidgets[index].key
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
                            .padding(bottom = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
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
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                "widget-weather-forecast" -> {
                    // 5-day Weather Forecast
                    val forecastConfig = settings.widgetConfigs["widget-weather-forecast"]
                    val tempUnit = settings.widgetConfigs["widget-clock-weather"]?.customSettings?.get("temp_unit") ?: "c"
                    val isFahrenheit = tempUnit.equals("f", ignoreCase = true)
                    val forecastDaysStr = forecastConfig?.customSettings?.get("forecast_days") ?: "5"
                    val maxForecastDays = (forecastDaysStr.toIntOrNull() ?: 5).coerceIn(1, 7)
                    val showRainProb = forecastConfig?.customSettings?.get("show_rain_prob") != "false"
                    val showDetails = forecastConfig?.customSettings?.get("show_details") == "true"

                    val weatherData = (weatherState as? WidgetState.Ready)?.data ?:
                                      (weatherState as? WidgetState.Stale)?.data
                    val forecastItems = weatherData?.daily?.take(maxForecastDays) ?: emptyList()

                    CollapsibleWidget(
                        title = "next $maxForecastDays days",
                        collapsedSummary = {
                            Text(
                                text = when (weatherState) {
                                    is WidgetState.NeedsSetup -> "allow location"
                                    is WidgetState.Loading -> "loading"
                                    is WidgetState.Ready, is WidgetState.Stale -> {
                                        val loc = weatherData?.locationName ?: "local"
                                        val first = forecastItems.firstOrNull()
                                        if (first != null) {
                                            val minVal = if (isFahrenheit) (first.minTemp * 9f / 5f + 32f).roundToInt() else first.minTemp.roundToInt()
                                            val maxVal = if (isFahrenheit) (first.maxTemp * 9f / 5f + 32f).roundToInt() else first.maxTemp.roundToInt()
                                            "$minVal° / $maxVal° · $loc"
                                        } else {
                                            loc
                                        }
                                    }
                                    is WidgetState.Error -> "offline"
                                    else -> "needs setup"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (weatherState) {
                                is WidgetState.NeedsSetup -> {
                                    Text(
                                        text = "meena needs location to check the weather. tap to allow.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onRequestLocationPermission()
                                            }
                                            .padding(vertical = 8.dp)
                                    )
                                }
                                is WidgetState.Loading -> {
                                    Text(
                                        text = "checking the skies...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MeenaTextMuted
                                    )
                                }
                                is WidgetState.Error -> {
                                    Text(
                                        text = "couldn't reach the weather. tap to try again.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                WeatherRepository.refresh(context, force = true)
                                            }
                                            .padding(vertical = 8.dp)
                                    )
                                }
                                is WidgetState.Ready, is WidgetState.Stale -> {
                                    if (forecastItems.isEmpty()) {
                                        Text(
                                            text = "no forecast available",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MeenaTextMuted
                                        )
                                    } else {
                                        forecastItems.forEach { day ->
                                            val minVal = if (isFahrenheit) (day.minTemp * 9f / 5f + 32f).roundToInt() else day.minTemp.roundToInt()
                                            val maxVal = if (isFahrenheit) (day.maxTemp * 9f / 5f + 32f).roundToInt() else day.maxTemp.roundToInt()
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = day.dayLabel,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MeenaTextWhite
                                                    )
                                                    Text(
                                                        text = if (showRainProb && day.precipitationProbability > 0) {
                                                            "${day.condition} · ${day.precipitationProbability}% rain"
                                                        } else {
                                                            day.condition
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MeenaTextMuted
                                                    )
                                                }
                                                Text(
                                                    text = "$minVal° / $maxVal°",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Atmospheric details if enabled
                                        if (showDetails && weatherData != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "humidity · ${weatherData.current.humidity}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MeenaTextMuted
                                                )
                                                Text(
                                                    text = "wind · ${weatherData.current.windSpeed.toInt()} km/h",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MeenaTextMuted
                                                )
                                            }
                                        }

                                        // Freshness caption
                                        val minutesAgo = ((System.currentTimeMillis() - weatherData!!.fetchedAt) / 60000L).coerceAtLeast(0)
                                        val freshnessText = if (minutesAgo == 0L) "updated just now" else "updated ${minutesAgo}m ago"
                                        Text(
                                            text = "$freshnessText · open-meteo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextMuted,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }

                "widget-today-summary" -> {
                    // Gemini AI Brief
                    val briefData = (briefState as? WidgetState.Ready)?.data ?:
                                    (briefState as? WidgetState.Stale)?.data

                    CollapsibleWidget(
                        title = "brief",
                        collapsedSummary = {
                            Text(
                                text = when (briefState) {
                                    is WidgetState.NeedsSetup -> "needs setup"
                                    is WidgetState.Loading -> "writing brief..."
                                    is WidgetState.Ready, is WidgetState.Stale -> "brief is on"
                                    is WidgetState.Error -> "offline"
                                    else -> "needs setup"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (briefState is WidgetState.Ready || briefState is WidgetState.Stale) MaterialTheme.colorScheme.primary else MeenaTextMuted
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            when (briefState) {
                                is WidgetState.NeedsSetup -> {
                                    Text(
                                        text = "add your gemini key to get a brief. tap to configure.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onOpenSettings()
                                            }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                                is WidgetState.Loading -> {
                                    Text(
                                        text = "writing your daily brief...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MeenaTextMuted
                                    )
                                }
                                is WidgetState.Error -> {
                                    val err = (briefState as WidgetState.Error).message
                                    Text(
                                        text = "$err. tap to retry.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                GeminiBriefRepository.refresh(context, settings, force = true)
                                            }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                                is WidgetState.Ready, is WidgetState.Stale -> {
                                    if (briefData != null) {
                                        Text(
                                            text = briefData.summaryText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MeenaTextWhite,
                                            lineHeight = 20.sp
                                        )
                                        val minutesAgo = ((System.currentTimeMillis() - briefData.generatedAt) / 60000L).coerceAtLeast(0)
                                        val freshnessText = if (minutesAgo == 0L) "updated just now" else "updated ${minutesAgo}m ago"
                                        Text(
                                            text = "$freshnessText · ${briefData.modelUsed}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextMuted,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }

                "widget-solat" -> {
                    // Waktu Solat JAKIM with dynamic celestial arc
                    CollapsibleWidget(
                        title = "waktu solat • jakim",
                        collapsedSummary = {
                            Text(
                                text = jakimSchedule.nextPrayerLabel.lowercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        DynamicSolatCelestialCard(
                            schedule = jakimSchedule,
                            onZoneChanged = onUpdateSolatZone
                        )
                    }
                }

                "widget-calendar-month" -> {
                    // Device calendar events & agenda
                    val calEvents = (calendarState as? CalendarState.Ready)?.events ?: emptyList()
                    val isNeedsPermission = calendarState is CalendarState.NeedsPermission
                    CollapsibleWidget(
                        title = "calendar",
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
                                        .clickable { onRequestCalendarPermission() }
                                        .padding(vertical = 4.dp),
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
                                        fontWeight = FontWeight.Bold
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
                                        text = "all quiet • no upcoming events",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                    Text(
                                        text = "+ event",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { DeviceCalendarRepository.openNewEvent(context) }
                                    )
                                }
                            }
                            else -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    calEvents.take(4).forEach { event ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MeenaSurface)
                                                .clickable { DeviceCalendarRepository.openEventDetail(context, event.id) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = event.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = buildString {
                                                        append(event.formattedDate)
                                                        append(" • ")
                                                        append(event.formattedTime)
                                                        if (event.location.isNotBlank()) {
                                                            append(" • ")
                                                            append(event.location)
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MeenaTextMuted,
                                                    fontSize = 11.sp
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
                                            modifier = Modifier.clickable { DeviceCalendarRepository.openNewEvent(context) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "widget-tasks" -> {
                    // Today's schedule / agenda
                    val calEvents = (calendarState as? CalendarState.Ready)?.events ?: emptyList()
                    val todayEvents = calEvents.filter { it.formattedDate == "today" }
                    CollapsibleWidget(
                        title = "today's events",
                        collapsedSummary = {
                            Text(
                                text = if (todayEvents.isEmpty()) "all quiet" else "${todayEvents.size} scheduled today",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (todayEvents.isEmpty()) MeenaTextMuted else MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        if (todayEvents.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "no more events today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted
                                )
                                Text(
                                    text = "+ event",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { DeviceCalendarRepository.openNewEvent(context) }
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                todayEvents.forEach { event ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MeenaSurface)
                                            .clickable { DeviceCalendarRepository.openEventDetail(context, event.id) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = event.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White
                                            )
                                            Text(
                                                text = event.formattedTime,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MeenaTextMuted
                                            )
                                        }
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
                        collapsedSummary = {
                            Text(
                                text = if (quickNotes.isEmpty()) "empty" else "${quickNotes.size} notes pinned",
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
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                quickNotes.forEachIndexed { index, note ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MeenaSurface)
                                            .border(1.dp, MeenaBorder)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MeenaTextWhite,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable { quickNotes.removeAt(index) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "x", fontSize = 11.sp, color = MeenaTextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "widget-health-device" -> {
                    // Connected device
                    CollapsibleWidget(
                        title = "connected device status",
                        collapsedSummary = {
                            Text(
                                text = "${deviceInfo.name.lowercase()} • ${deviceInfo.batteryPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaProfitGreen
                            )
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface)
                                    .border(1.dp, MeenaBorder)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        HealthRepository.openFitnessApp(context)
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = deviceInfo.name.lowercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "synced today at ${deviceInfo.lastSyncTime}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MeenaTextMuted
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF00A4EF), RoundedCornerShape(0.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = deviceInfo.connectionType.lowercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Color(0xFF141414))
                                                .border(1.dp, MeenaBorder)
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Column {
                                                Text("battery", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${deviceInfo.batteryPercent}%",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (deviceInfo.batteryPercent > 20) MeenaProfitGreen else Color(0xFFFF5252)
                                                    )
                                                    Text("normal", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Color(0xFF141414))
                                                .border(1.dp, MeenaBorder)
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Column {
                                                Text("skin temp", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "%.1f°c".format(deviceInfo.bodyTempCelsius),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text("wrist", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "open companion app",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "sync now",
                                            fontSize = 11.sp,
                                            color = MeenaTextMuted,
                                            modifier = Modifier.clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                HealthRepository.refreshTelemetry()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "widget-health-telemetry" -> {
                    // Fit telemetry
                    CollapsibleWidget(
                        title = "fit telemetry",
                        collapsedSummary = {
                            Text(
                                text = "${fitTelemetry.stepsToday} steps • ${fitTelemetry.heartRateBpm} bpm",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface)
                                    .border(1.dp, MeenaBorder)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("step target", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        Text(
                                            text = "${fitTelemetry.stepsToday} / ${fitTelemetry.stepGoal}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (fitTelemetry.stepsToday.toFloat() / fitTelemetry.stepGoal.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = MeenaProfitGreen,
                                        trackColor = MeenaBorder
                                    )
                                }
                            }
                        }
                    }
                }

                "widget-health-aqi" -> {
                    // Air quality index
                    CollapsibleWidget(
                        title = "air quality index",
                        collapsedSummary = {
                            Text(
                                text = "${airIndex.stationName.lowercase()} • aqi ${airIndex.aqiValue}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MeenaSurface)
                                .border(1.dp, MeenaBorder)
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(airIndex.stationName.lowercase(), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${airIndex.aqiValue}",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(android.graphics.Color.parseColor(airIndex.aqiColorHex))
                                    )
                                    Text(
                                        text = airIndex.aqiStatus.lowercase(),
                                        fontSize = 12.sp,
                                        color = Color(android.graphics.Color.parseColor(airIndex.aqiColorHex))
                                    )
                                }
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
                    // System telemetry
                    CollapsibleWidget(
                        title = "device status",
                        collapsedSummary = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 28.dp, height = 10.dp)
                                        .border(1.dp, MeenaBorder, RoundedCornerShape(0.dp))
                                        .padding(1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize((telemetry.batteryPercent / 100f).coerceIn(0.05f, 1f))
                                            .background(if (telemetry.batteryPercent <= 20) Color(0xFFE51400) else MeenaProfitGreen)
                                    )
                                }
                                Text("${telemetry.batteryPercent}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                if (telemetry.isCharging) {
                                    Text("charging", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Text("• memory ${telemetry.ramPercent}%", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface)
                                    .border(1.dp, MeenaBorder)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("battery:", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 12.dp)
                                            .border(1.dp, MeenaBorder)
                                            .padding(1.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize((telemetry.batteryPercent / 100f).coerceIn(0.05f, 1f))
                                                .background(if (telemetry.batteryPercent <= 20) Color(0xFFE51400) else MeenaProfitGreen)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${telemetry.batteryPercent}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                                Text(
                                    if (telemetry.isCharging) "fast charging" else "discharging • optimal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (telemetry.isCharging) MaterialTheme.colorScheme.primary else MeenaTextMuted
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MeenaSurface)
                                        .border(1.dp, MeenaBorder)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("memory", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        Text(
                                            String.format(Locale.US, "%.1f gb / %.1f gb", telemetry.usedRamMb / 1024.0, telemetry.totalRamMb / 1024.0),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        LinearProgressIndicator(
                                            progress = { (telemetry.ramPercent / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MeenaBorder
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MeenaSurface)
                                        .border(1.dp, MeenaBorder)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("storage", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        Text(
                                            String.format(Locale.US, "%.1f gb / %.1f gb", telemetry.usedStorageGb, telemetry.totalStorageGb),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        LinearProgressIndicator(
                                            progress = { (telemetry.storagePercent / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                                            color = Color.White,
                                            trackColor = MeenaBorder
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface)
                                    .border(1.dp, MeenaBorder)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("data traffic:", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Text(
                                        String.format(Locale.US, "↓ %.2f mb  ↑ %.2f mb", telemetry.trafficRxMb, telemetry.trafficTxMb),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("brightness:", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Text("${telemetry.screenBrightnessPercent}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}
