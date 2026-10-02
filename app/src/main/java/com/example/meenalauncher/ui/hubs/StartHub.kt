package com.example.meenalauncher.ui.hubs

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.data.system.DeviceAppInfo
import com.example.meenalauncher.data.system.DeviceTelemetryHelper
import com.example.meenalauncher.data.system.InstalledAppsRepository
import com.example.meenalauncher.data.system.JakimSolatRepository
import com.example.meenalauncher.data.system.NotificationRepository
import com.example.meenalauncher.data.system.SpendingRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextWhite
import com.example.meenalauncher.ui.components.CollapsibleWidget
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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var currentTime by remember { mutableStateOf("00:00") }
    var currentDate by remember { mutableStateOf("") }
    var telemetry by remember { mutableStateOf(DeviceTelemetryHelper.getTelemetry(context)) }
    var installedApps by remember { mutableStateOf<List<DeviceAppInfo>>(emptyList()) }

    val liveNotifications by NotificationRepository.notificationsFlow.collectAsState()
    val liveTransactions by SpendingRepository.transactionsFlow.collectAsState()
    val prayerSchedule = remember { JakimSolatRepository.getTodaySchedule() }

    // Load real installed apps
    LaunchedEffect(Unit) {
        installedApps = InstalledAppsRepository.loadInstalledApps(context)
    }

    // Live clock and telemetry updates
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

    // Compute top 6 quick apps (pinned apps first, supplemented by installed apps)
    val quickApps = remember(installedApps, settings.pinnedAppIds) {
        val pinned = settings.pinnedAppIds.mapNotNull { id -> installedApps.firstOrNull { it.packageName == id } }
        val rest = installedApps.filterNot { it.packageName in settings.pinnedAppIds }
        (pinned + rest).take(6)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. HERO CLOCK & REAL SYSTEM DATE
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Light,
                        color = MeenaTextWhite
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${telemetry.batteryPercent}% ⚡",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (telemetry.isCharging) MeenaProfitGreen else Color.White
                        )
                        Text(
                            text = if (telemetry.isCharging) "Charging" else "Battery Optimal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                }
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // 2. LIST WIDGET: PINNED & QUICK APPS (LATEST 6 DATA)
        item {
            CollapsibleWidget(
                title = "pinned apps • quick launch (6)",
                collapsedSummary = {
                    Text("${quickApps.size} apps ready", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (quickApps.isEmpty()) {
                        Text("Loading installed applications...", color = MeenaTextMuted, fontSize = 12.sp)
                    } else {
                        quickApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface, RoundedCornerShape(4.dp))
                                    .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
                                    .clickable { onLaunchPackage(app.packageName) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (app.iconBitmap != null) {
                                            Image(
                                                bitmap = app.iconBitmap,
                                                contentDescription = app.label,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Launch",
                                    tint = MeenaTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. LIST WIDGET: LIVE NOTIFICATIONS & MESSAGES (LATEST 6 REAL DATA)
        item {
            CollapsibleWidget(
                title = "notifications • alerts (6)",
                collapsedSummary = {
                    Text(
                        text = if (liveNotifications.isNotEmpty()) "${liveNotifications.size} new alerts" else "No active alerts",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (liveNotifications.isNotEmpty()) MaterialTheme.colorScheme.primary else MeenaTextMuted
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (liveNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MeenaSurface, RoundedCornerShape(4.dp))
                                .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No active notifications",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Incoming notifications from WhatsApp, Telegram, Gmail, and banking apps will appear here in real-time.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    } else {
                        liveNotifications.take(6).forEach { notif ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface, RoundedCornerShape(4.dp))
                                    .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
                                    .clickable { onLaunchPackage(notif.packageName) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = notif.appName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = notif.formattedTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MeenaTextMuted
                                            )
                                        }
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        if (notif.text.isNotBlank() && notif.text != notif.title) {
                                            Text(
                                                text = notif.text,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MeenaTextMuted,
                                                maxLines = 2,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. LIST WIDGET: SPENDING TRANSACTIONS (LATEST 6 DATA)
        item {
            CollapsibleWidget(
                title = "spending • transactions (6)",
                collapsedSummary = {
                    val spentToday = liveTransactions.sumOf { it.amount }
                    Text(
                        text = if (spentToday > 0) "MYR %.2f spent today".format(spentToday) else "Monitoring banking notifications",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (spentToday > 0) MeenaProfitGreen else MeenaTextMuted
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (liveTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MeenaSurface, RoundedCornerShape(4.dp))
                                .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No spending recorded today",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Maybank MAE, TNG eWallet, CIMB Octo, and GrabPay transactions will be parsed here automatically.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted
                                )
                            }
                        }
                    } else {
                        liveTransactions.take(6).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface, RoundedCornerShape(4.dp))
                                    .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                when (tx.bankCode) {
                                                    "M" -> Color(0xFFFFB800)
                                                    "T" -> Color(0xFF0055A5)
                                                    "C" -> Color(0xFFCC0000)
                                                    "G" -> Color(0xFF00B14F)
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tx.bankCode,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(tx.merchant, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                        Text("${tx.bank} • ${tx.formattedTime}", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    }
                                }
                                Text(
                                    text = "-MYR %.2f".format(tx.amount),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. LIST WIDGET: WAKTU SOLAT JAKIM (6 SLOTS FOR TODAY)
        item {
            CollapsibleWidget(
                title = "waktu solat • jakim (6)",
                collapsedSummary = {
                    Text(prayerSchedule.nextPrayerLabel, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Zone ${prayerSchedule.zone} (${prayerSchedule.zoneName}) • ${prayerSchedule.nextPrayerLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    prayerSchedule.slots.forEach { slot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (slot.isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MeenaSurface,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (slot.isCurrent) MaterialTheme.colorScheme.primary else MeenaBorder,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (slot.isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column {
                                    Text(
                                        text = slot.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (slot.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (slot.isCurrent) Color.White else MeenaTextWhite
                                    )
                                    Text(
                                        text = slot.sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                            }
                            Text(
                                text = slot.time,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (slot.isCurrent) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                }
            }
        }

        // 6. LIST WIDGET: HARDWARE TELEMETRY (6 REAL METRICS)
        item {
            CollapsibleWidget(
                title = "system telemetry (6)",
                collapsedSummary = {
                    Text("RAM ${telemetry.ramPercent}% • Storage ${telemetry.storagePercent}%", style = MaterialTheme.typography.labelSmall, color = MeenaProfitGreen)
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TelemetryRow(label = "1. BATTERY", value = "${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "Optimal"})")
                    TelemetryRow(label = "2. MEMORY (RAM)", value = "${"%.1f".format(telemetry.usedRamMb / 1024.0)} GB / ${"%.1f".format(telemetry.totalRamMb / 1024.0)} GB (${telemetry.ramPercent}%)")
                    TelemetryRow(label = "3. STORAGE", value = "${"%.1f".format(telemetry.usedStorageGb)} GB / ${"%.1f".format(telemetry.totalStorageGb)} GB (${telemetry.storagePercent}%)")
                    TelemetryRow(label = "4. DATA TRAFFIC (RX)", value = "${telemetry.trafficRxMb} MB (Received)")
                    TelemetryRow(label = "5. DATA TRAFFIC (TX)", value = "${telemetry.trafficTxMb} MB (Transmitted)")
                    TelemetryRow(label = "6. DISPLAY & BRIGHTNESS", value = "Level ${telemetry.screenBrightnessPercent}% • 120Hz LTPO")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface, RoundedCornerShape(4.dp))
            .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}
