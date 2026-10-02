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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.data.system.DeviceTelemetryHelper
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaSurfaceElevated
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextSecondary
import com.example.meenalauncher.theme.MeenaTextWhite
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.meenalauncher.data.system.DeviceAppInfo
import com.example.meenalauncher.data.system.InstalledAppsRepository
import com.example.meenalauncher.data.system.NotificationRepository
import com.example.meenalauncher.ui.components.CollapsibleWidget
import com.example.meenalauncher.ui.components.ConversationsWidget
import com.example.meenalauncher.ui.components.LiveTile
import com.example.meenalauncher.ui.components.NewsFeedWidget
import com.example.meenalauncher.ui.components.OpenMapsRadarWidget
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StartHub(
    settings: MeenaUserSettings,
    listState: LazyListState,
    onLaunchPackage: (String) -> Unit = {},
    onOpenDialer: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenEmail: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenCalculator: () -> Unit
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
    var installedApps by remember { mutableStateOf<List<DeviceAppInfo>>(emptyList()) }
    var isNotificationAccessGranted by remember {
        mutableStateOf(NotificationRepository.isNotificationAccessGranted(context))
    }

    LaunchedEffect(isListenerConnected) {
        isNotificationAccessGranted = isListenerConnected || NotificationRepository.isNotificationAccessGranted(context)
    }

    LaunchedEffect(Unit) {
        installedApps = InstalledAppsRepository.loadInstalledApps(context)
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

        // 4. WIDGET: LIVE TILES LIST (Latest 6 Data Items)
        if (settings.enabledWidgets["widget-my-apps"] != false) {
            item {
                CollapsibleWidget(
                    title = "live tiles (6)",
                    collapsedSummary = {
                        Text(
                            text = "6 active live tiles • Tap to open",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Phone
                        val phoneNotif = notifications.firstOrNull { 
                            it.packageName.contains("dialer", ignoreCase = true) || 
                            it.packageName.contains("telecom", ignoreCase = true) ||
                            it.appName.contains("phone", ignoreCase = true)
                        }
                        LiveTileListRow(
                            title = "Phone",
                            subtitle = phoneNotif?.let { "${it.title}: ${it.text}" } ?: "Dialer & Recent Calls",
                            badgeText = phoneNotif?.formattedTime,
                            accentColor = MaterialTheme.colorScheme.primary,
                            iconVector = Icons.Default.Phone,
                            onClick = onOpenDialer
                        )

                        // 2. Messaging
                        val msgNotif = notifications.firstOrNull { 
                            it.packageName.contains("messaging", ignoreCase = true) || 
                            it.packageName.contains("mms", ignoreCase = true) ||
                            it.appName.contains("message", ignoreCase = true)
                        }
                        LiveTileListRow(
                            title = "Messaging",
                            subtitle = msgNotif?.let { "${it.title}: ${it.text}" } ?: "SMS • Chat & Conversations",
                            badgeText = msgNotif?.formattedTime ?: "3",
                            accentColor = Color(0xFF107C10),
                            iconVector = Icons.AutoMirrored.Filled.Message,
                            onClick = onOpenMessages
                        )

                        // 3. Outlook / Mail
                        val mailNotif = notifications.firstOrNull { 
                            it.packageName.contains("gmail", ignoreCase = true) || 
                            it.packageName.contains("outlook", ignoreCase = true) ||
                            it.packageName.contains("email", ignoreCase = true)
                        }
                        LiveTileListRow(
                            title = "Outlook Mail",
                            subtitle = mailNotif?.let { "${it.title}: ${it.text}" } ?: "Inbox • Synced",
                            badgeText = mailNotif?.formattedTime ?: "12",
                            accentColor = Color(0xFF0078D7),
                            iconVector = Icons.Default.Email,
                            onClick = onOpenEmail
                        )

                        // 4. Camera
                        LiveTileListRow(
                            title = "Camera",
                            subtitle = "Quick Capture • 4K HDR",
                            badgeText = null,
                            accentColor = Color(0xFFD83B01),
                            iconVector = Icons.Default.CameraAlt,
                            onClick = onOpenCamera
                        )

                        // Items 5 & 6: Pinned apps or top installed apps
                        val customAppIds = (settings.pinnedAppIds + installedApps.map { it.packageName })
                            .distinct()
                            .filterNot { it.contains("dialer") || it.contains("camera") }
                            .take(2)

                        customAppIds.forEach { pkg ->
                            val appInfo = installedApps.firstOrNull { it.packageName == pkg }
                            val meta = getPinnedAppMeta(pkg)
                            val appNotif = notifications.firstOrNull { it.packageName == pkg }
                            val appLabel = appInfo?.label ?: meta.name
                            val appSubtitle = appNotif?.let { "${it.title}: ${it.text}" } ?: (meta.category + " • Live")
                            val appColor = if (appInfo != null) Color(0xFF1F1F1F) else meta.color

                            LiveTileListRow(
                                title = appLabel,
                                subtitle = appSubtitle,
                                badgeText = appNotif?.formattedTime,
                                accentColor = appColor,
                                iconBitmap = appInfo?.iconBitmap,
                                iconGlyph = if (appInfo?.iconBitmap == null) meta.emoji else null,
                                onClick = {
                                    onLaunchPackage(pkg)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 5. WIDGET: MAILBOX
        if (settings.enabledWidgets["widget-mailbox"] != false) {
            item {
                CollapsibleWidget(title = "mailbox • outlook") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MailboxRow("Erik Hudgens: Best mic for hollowbody guitar?", "Re: Telecaster & Shure Beta 58A shootout...", "12:49 PM", Color(0xFF0078D7))
                        MailboxRow("Sarah Lin: Q3 Product Review Slides", "Attached final sprint deck for leadership...", "09:12 AM", Color(0xFF555555))
                    }
                }
            }
        }

        // 6. WIDGET: REALTIME CONVERSATIONS (WhatsApp, Telegram, SMS, etc.)
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

        // 8. WIDGET: MAP RADAR (OpenMaps Dark Theme with 3km Radar Wave & North Bearing)
        if (settings.enabledWidgets["widget-map-radar"] != false) {
            item {
                OpenMapsRadarWidget()
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
private fun MailboxRow(subject: String, preview: String, time: String, borderAccent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .padding(start = 4.dp)
            .drawBehindBorderLeft(borderAccent, 3.dp)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(subject, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(preview, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted, maxLines = 1)
        }
        Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
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

private fun Modifier.drawBehindBorderLeft(color: Color, width: Dp) = this.drawBehind {
    val strokeWidthPx = width.toPx()
    drawLine(
        color = color,
        start = Offset(strokeWidthPx / 2, 0f),
        end = Offset(strokeWidthPx / 2, this.size.height),
        strokeWidth = strokeWidthPx
    )
}

data class PinnedAppMeta(
    val name: String,
    val emoji: String,
    val color: Color,
    val category: String
)

fun getPinnedAppMeta(appId: String): PinnedAppMeta {
    return when (appId) {
        "org.telegram.messenger" -> PinnedAppMeta("Telegram", "✈️", Color(0xFF229ED9), "Messaging")
        "com.google.android.gm" -> PinnedAppMeta("Gmail", "✉️", Color(0xFFEA4335), "Google")
        "com.whatsapp" -> PinnedAppMeta("WhatsApp", "💬", Color(0xFF25D366), "Social")
        "com.android.calculator2" -> PinnedAppMeta("Calculator", "🔢", Color(0xFF0078D7), "Tools")
        "com.android.deskclock" -> PinnedAppMeta("Clock", "⏰", Color(0xFF107C10), "Alarms")
        "com.android.camera" -> PinnedAppMeta("Camera", "📷", Color(0xFF333333), "Media")
        else -> {
            val simpleName = appId.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            PinnedAppMeta(simpleName, "📱", Color(0xFF1F1F1F), "Application")
        }
    }
}

@Composable
private fun LiveTileListRow(
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = MeenaProfitGreen,
    accentColor: Color,
    iconGlyph: String? = null,
    iconBitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = iconBitmap,
                        contentDescription = title,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconGlyph != null) {
                    Text(
                        text = iconGlyph,
                        fontSize = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextWhite,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    maxLines = 1
                )
            }
        }
        if (!badgeText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(badgeColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
