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
import com.example.meenalauncher.ui.components.CollapsibleWidget
import com.example.meenalauncher.ui.components.LiveTile
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
    var currentTime by remember { mutableStateOf("07:42") }
    var currentDate by remember { mutableStateOf("FRIDAY, OCTOBER 2") }
    var telemetry by remember { mutableStateOf(DeviceTelemetryHelper.getTelemetry(context)) }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        var pollCounter = 0
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).uppercase()
            pollCounter++
            if (pollCounter >= 3) {
                pollCounter = 0
                telemetry = DeviceTelemetryHelper.getTelemetry(context)
            }
            delay(1000)
        }
    }

    // Dynamic Weather Icon Animation
    val infiniteTransition = rememberInfiniteTransition(label = "weatherAnimation")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunRotation"
    )
    val sunScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunScale"
    )

    var isMessagesFlipped by remember { mutableStateOf(false) }
    var isOutlookFlipped by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            isMessagesFlipped = !isMessagesFlipped
            delay(1500)
            isOutlookFlipped = !isOutlookFlipped
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
                                        rotationZ = sunRotation
                                        scaleX = sunScale
                                        scaleY = sunScale
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

        // 4. WIDGET: LIVE TILES GRID
        if (settings.enabledWidgets["widget-my-apps"] != false) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "live tiles",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Auto-Flipping 3D",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }

                    // 2x2 Grid (Phone & Messaging)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Phone (Medium 2x2)
                        LiveTile(
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            onClick = onOpenDialer,
                            frontContent = {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.Phone, "Phone", tint = Color.White, modifier = Modifier.size(28.dp))
                                        Box(modifier = Modifier.background(Color.White).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                            Text("2 missed", fontSize = 10.sp, color = Color.Black)
                                        }
                                    }
                                    Column {
                                        Text("Phone", style = MaterialTheme.typography.headlineMedium)
                                        Text("Ali • 12m ago", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        )

                        // Messaging (Medium 2x2, Live Flipping)
                        LiveTile(
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            backgroundColor = Color(0xFF107C10),
                            isFlipped = isMessagesFlipped,
                            onClick = onOpenMessages,
                            frontContent = {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Message, "Messaging", tint = Color.White, modifier = Modifier.size(28.dp))
                                        Box(modifier = Modifier.background(Color.White).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text("3", fontSize = 11.sp, color = Color.Black)
                                        }
                                    }
                                    Column {
                                        Text("Messaging", style = MaterialTheme.typography.headlineMedium)
                                        Text("John: Free tonight?", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            },
                            backContent = {
                                Column(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFF0E630E)).padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("3", style = MaterialTheme.typography.displayMedium, color = Color.White)
                                    Text("UNREAD TEXTS", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    Text("2 min ago", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wide 4x2 Tile (Outlook Mail)
                    LiveTile(
                        modifier = Modifier.fillMaxWidth().height(88.dp),
                        backgroundColor = Color(0xFF0078D7),
                        isFlipped = isOutlookFlipped,
                        onClick = onOpenEmail,
                        frontContent = {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Email, "Outlook", tint = Color.White, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Outlook Mail", style = MaterialTheme.typography.bodyLarge)
                                        Text("Erik: Best mic for hollowbody guitar?", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                                Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("12", fontSize = 18.sp, color = Color.White)
                                }
                            }
                        },
                        backContent = {
                            Row(
                                modifier = Modifier.fillMaxSize().background(Color(0xFF005A9E)).padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LATEST FROM SARAH LIN", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text("Q3 Product Review Presentation Slides", style = MaterialTheme.typography.bodyLarge)
                                }
                                Text("09:12 AM", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    )

                    // Pinned Apps Live Tiles (Multi-Size: 1x1, 2x2, 4x2)
                    if (settings.pinnedAppIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PINNED APPS (${settings.pinnedAppIds.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val wideApps = settings.pinnedAppIds.filter { settings.pinnedAppSizes[it] == "4x2" }
                        val standardApps = settings.pinnedAppIds.filter { settings.pinnedAppSizes[it] != "4x2" }

                        // 4x2 Wide Tiles
                        wideApps.forEach { appId ->
                            val appMeta = getPinnedAppMeta(appId)
                            LiveTile(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(88.dp)
                                    .padding(bottom = 8.dp),
                                backgroundColor = appMeta.color,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onLaunchPackage(appId)
                                },
                                frontContent = {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(appMeta.emoji, fontSize = 28.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(appMeta.name, style = MaterialTheme.typography.headlineMedium, fontSize = 18.sp)
                                                Text("${appMeta.category} • Wide Live Tile", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.25f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("4×2", fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            )
                        }

                        // 2x2 and 1x1 standard tiles in 2-column rows
                        val chunked = standardApps.chunked(2)
                        chunked.forEach { rowIds ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIds.forEach { appId ->
                                    val appMeta = getPinnedAppMeta(appId)
                                    val isSmall = settings.pinnedAppSizes[appId] == "1x1"
                                    LiveTile(
                                        modifier = Modifier.weight(1f).aspectRatio(1f),
                                        backgroundColor = appMeta.color,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onLaunchPackage(appId)
                                        },
                                        frontContent = {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(appMeta.emoji, fontSize = if (isSmall) 20.sp else 24.sp)
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color.White.copy(alpha = 0.25f))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(if (isSmall) "1×1" else "PINNED", fontSize = 8.sp, color = Color.White)
                                                    }
                                                }
                                                Column {
                                                    Text(appMeta.name, style = MaterialTheme.typography.headlineMedium, fontSize = if (isSmall) 14.sp else 16.sp, maxLines = 1)
                                                    Text(appMeta.category, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                                }
                                            }
                                        }
                                    )
                                }
                                if (rowIds.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
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

        // 6. WIDGET: NOTIFICATION STREAM
        if (settings.enabledWidgets["widget-notifications"] != false) {
            item {
                CollapsibleWidget(title = "notification stream") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NotificationRow(MaterialTheme.colorScheme.primary, "Telegram", "Ahmad deployed backend v2", "Just now")
                        NotificationRow(Color(0xFFEA4335), "Gmail", "Build passes for Meena 1.0", "8m ago")
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

        // 8. WIDGET: MAP RADAR
        if (settings.enabledWidgets["widget-map-radar"] != false) {
            item {
                CollapsibleWidget(
                    title = "current map location",
                    collapsedSummary = {
                        Text("Bukit Bintang, KL • Live", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color(0xFF080808))
                            .border(1.dp, MeenaBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Bukit Bintang, Kuala Lumpur", style = MaterialTheme.typography.bodyLarge)
                            Text("3.1466° N, 101.7112° E • Accuracy 4m", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
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
private fun NotificationRow(badgeColor: Color, app: String, text: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(6.dp).background(badgeColor))
            Spacer(modifier = Modifier.width(8.dp))
            Text("$app: $text", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
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
