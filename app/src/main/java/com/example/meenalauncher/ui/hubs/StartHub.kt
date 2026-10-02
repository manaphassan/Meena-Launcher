package com.example.meenalauncher.ui.hubs

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.data.system.DeviceTelemetryHelper
import com.example.meenalauncher.data.system.NotificationRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextSecondary
import com.example.meenalauncher.theme.MeenaTextWhite
import com.example.meenalauncher.ui.components.CollapsibleWidget
import com.example.meenalauncher.ui.components.ConversationsWidget
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StartHub(
    settings: MeenaUserSettings,
    listState: LazyListState,
    onLaunchPackage: (String) -> Unit = {},
    onOpenDialer: () -> Unit = {},
    onOpenMessages: () -> Unit = {},
    onOpenEmail: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onOpenCalculator: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val initialDate = remember { Date() }
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(initialDate))
    }
    var currentDate by remember {
        mutableStateOf(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(initialDate).uppercase())
    }
    var telemetry by remember { mutableStateOf(DeviceTelemetryHelper.getTelemetry(context)) }
    val notifications by NotificationRepository.notificationsFlow.collectAsState()
    val isListenerConnected by NotificationRepository.isListenerConnectedFlow.collectAsState()
    var isNotificationAccessGranted by remember {
        mutableStateOf(NotificationRepository.isNotificationAccessGranted(context))
    }

    LaunchedEffect(isListenerConnected) {
        isNotificationAccessGranted = isListenerConnected || NotificationRepository.isNotificationAccessGranted(context)
    }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        var pollCounter = 0
        while (true) {
            val now = Date()
            val newTime = timeFormat.format(now)
            if (newTime != currentTime) {
                currentTime = newTime
                currentDate = dateFormat.format(now).uppercase()
            }
            pollCounter++
            if (pollCounter >= 3) {
                pollCounter = 0
                telemetry = DeviceTelemetryHelper.getTelemetry(context)
                if (!isListenerConnected) {
                    isNotificationAccessGranted = NotificationRepository.isNotificationAccessGranted(context)
                }
            }
            delay(1000)
        }
    }

    // Dynamic Weather Icon Animation - optimized for draw-phase execution
    val infiniteTransition = rememberInfiniteTransition(label = "weatherAnimation")
    val sunRotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunRotation"
    )
    val sunScale = infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunScale"
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. WIDGET: HERO CLOCK & CURRENT WEATHER
        if (settings.enabledWidgets["widget-clock-weather"] != false) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = sunRotation.value
                                        scaleX = sunScale.value
                                        scaleY = sunScale.value
                                    }
                                ) {
                                    Text(text = "☀️", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "31°",
                                    fontSize = 28.sp,
                                    color = MeenaTextWhite,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            Text(
                                text = "Kuala Lumpur • Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    }
                    Text(
                        text = "$currentDate • 19 RABI' AL-AWWAL 1448H",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 2. WIDGET: 5-DAY WEATHER FORECAST
        if (settings.enabledWidgets["widget-weather-forecast"] != false) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "5-DAY FORECAST • HUMIDITY 72% • UV 8",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ForecastCard("FRI", "☀️", "31°", Modifier.weight(1f))
                        ForecastCard("SAT", "⛅", "32°", Modifier.weight(1f))
                        ForecastCard("SUN", "🌧️", "28°", Modifier.weight(1f))
                        ForecastCard("MON", "⛈️", "27°", Modifier.weight(1f))
                        ForecastCard("TUE", "🌤️", "30°", Modifier.weight(1f))
                    }
                }
            }
        }

        // 3. WIDGET: TODAY SUMMARY (Gemini AI Brief)
        if (settings.enabledWidgets["widget-today-summary"] != false) {
            item {
                CollapsibleWidget(
                    title = "today • summary",
                    collapsedSummary = {
                        Text(
                            text = "☀️ 31°C • 3 tasks • KLCI +0.42%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                ) {
                    Text(
                        text = "Good morning. Clear skies in Kuala Lumpur at 31°C. You have 3 calendar events today including the AI Pipeline Review at 10:00 AM. Markets are positive with KLCI trading up +0.42%.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextSecondary,
                        lineHeight = 22.sp
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MeenaProfitGreen, RoundedCornerShape(0.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gemini Flash Proactive Context Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                }
            }
        }

        // 4. WIDGET: REALTIME CONVERSATIONS (WhatsApp, Telegram, SMS, etc.)
        if (settings.enabledWidgets["widget-conversations"] != false) {
            item {
                ConversationsWidget()
            }
        }

        // 7. WIDGET: NOTIFICATION STREAM
        if (settings.enabledWidgets["widget-notifications"] != false) {
            item {
                CollapsibleWidget(
                    title = "notification stream",
                    collapsedSummary = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isNotificationAccessGranted) MeenaProfitGreen else Color(0xFFE51400),
                                        RoundedCornerShape(0.dp)
                                    )
                            )
                            Text(
                                text = when {
                                    !isNotificationAccessGranted -> "Permission Required"
                                    notifications.isNotEmpty() -> "${notifications.size} unread notifications"
                                    else -> "Realtime Sync Active"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!isNotificationAccessGranted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2A1010))
                                    .border(1.dp, Color(0xFFE51400))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        NotificationRepository.openNotificationSettings(context)
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFE51400),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "NOTIFICATION ACCESS REQUIRED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE51400)
                                        )
                                        Text(
                                            text = "Tap to grant permission so real-time notifications show up.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else if (notifications.isNotEmpty()) {
                            notifications.take(6).forEach { notif ->
                                val notifColor = when {
                                    notif.packageName.contains("whatsapp", ignoreCase = true) -> Color(0xFF25D366)
                                    notif.packageName.contains("telegram", ignoreCase = true) -> Color(0xFF0088CC)
                                    notif.packageName.contains("gmail", ignoreCase = true) -> Color(0xFFEA4335)
                                    notif.packageName.contains("messaging", ignoreCase = true) -> Color(0xFF00A4EF)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                NotificationRow(
                                    badgeColor = notifColor,
                                    app = notif.appName,
                                    title = notif.title,
                                    text = notif.text,
                                    time = notif.formattedTime,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        NotificationRepository.openNotification(context, notif)
                                    },
                                    onDismiss = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        NotificationRepository.dismissNotification(notif.key)
                                    }
                                )
                            }
                            // Clear All Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "CLEAR ALL NOTIFICATIONS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MeenaTextMuted,
                                    modifier = Modifier
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            NotificationRepository.clearAll()
                                        }
                                        .padding(4.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface)
                                    .border(1.dp, MeenaBorder)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(MeenaProfitGreen, RoundedCornerShape(0.dp))
                                    )
                                    Text(
                                        text = "All caught up • Real-time notifications active",
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


        // 7. WIDGET: SYSTEM TELEMETRY (With Minimized Battery Gauge & Collapsible Breakdown)
        if (settings.enabledWidgets["widget-device-telemetry"] != false) {
            item {
                CollapsibleWidget(
                    title = "system telemetry",
                    collapsedSummary = {
                        // Minimized Battery Gauge Bar Status (Visible when folded!)
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
                                Text("⚡Charging", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("• RAM ${telemetry.ramPercent}%", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Battery Bar Full
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
                                Text("BATTERY:", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
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
                                if (telemetry.isCharging) "⚡ Fast Charging" else "Discharging • Optimal",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (telemetry.isCharging) MaterialTheme.colorScheme.primary else MeenaTextMuted
                            )
                        }

                        // RAM & UFS Storage 2-Column
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
                                    Text("MEMORY (RAM)", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Text(
                                        String.format(Locale.US, "%.1f GB / %.1f GB", telemetry.usedRamMb / 1024.0, telemetry.totalRamMb / 1024.0),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    LinearProgressIndicator(
                                        progress = { (telemetry.ramPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MeenaBorder
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            String.format(Locale.US, "Avail: %.1f GB", (telemetry.totalRamMb - telemetry.usedRamMb).coerceAtLeast(0) / 1024.0),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextMuted
                                        )
                                        Text("${telemetry.ramPercent}%", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    }
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
                                    Text("STORAGE", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Text(
                                        String.format(Locale.US, "%.1f GB / %.1f GB", telemetry.usedStorageGb, telemetry.totalStorageGb),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    LinearProgressIndicator(
                                        progress = { (telemetry.storagePercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                                        color = Color.White,
                                        trackColor = MeenaBorder
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            String.format(Locale.US, "Free: %.1f GB", telemetry.freeStorageGb),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextMuted
                                        )
                                        Text("${telemetry.storagePercent}%", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    }
                                }
                            }
                        }

                        // Traffic & Screen
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
                                Text("DATA TRAFFIC:", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Text(
                                    String.format(Locale.US, "↓ %.2f MB  ↑ %.2f MB", telemetry.trafficRxMb, telemetry.trafficTxMb),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("DISPLAY & BRIGHTNESS:", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Text("Level ${telemetry.screenBrightnessPercent}% • 120Hz LTPO", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
private fun ForecastCard(day: String, glyph: String, temp: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
            Text(glyph, fontSize = 16.sp, modifier = Modifier.padding(vertical = 2.dp))
            Text(temp, style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
        }
    }
}


@Composable
private fun NotificationRow(
    badgeColor: Color,
    app: String,
    title: String,
    text: String,
    time: String,
    onClick: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .background(badgeColor)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MeenaTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onDismiss != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 11.sp, color = MeenaTextMuted)
            }
        }
    }
}

