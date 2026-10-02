package com.example.meenalauncher.ui.hubs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.data.system.JakimSolatRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSolarAmber
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextSecondary
import com.example.meenalauncher.theme.MeenaTextWhite
import com.example.meenalauncher.ui.components.CollapsibleWidget

@Composable
fun AgendaHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val haptic = LocalHapticFeedback.current
    val jakimSchedule = remember { JakimSolatRepository.getTodaySchedule("WLY01") }
    val quickNotes = remember {
        mutableStateListOf(
            "Prepare Jetpack Compose architecture proposal",
            "Verify JAKIM API e-Solat solar calculation endpoints"
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. WAKTU SOLAT JAKIM TIMELINE (With 5 Prayers + 3 Solar Markers)
        item {
            CollapsibleWidget(
                title = "waktu solat • jakim",
                collapsedSummary = {
                    Text(jakimSchedule.nextPrayerLabel, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Zone ${jakimSchedule.zone} (${jakimSchedule.zoneName})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )

                    // 6-Slot Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        jakimSchedule.slots.take(3).forEach { slot ->
                            PrayerSlot(
                                name = slot.name,
                                time = slot.time,
                                sub = slot.sub,
                                modifier = Modifier.weight(1f),
                                isSolar = slot.isSolar,
                                isCurrent = slot.isCurrent
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        jakimSchedule.slots.drop(3).take(3).forEach { slot ->
                            PrayerSlot(
                                name = slot.name,
                                time = slot.time,
                                sub = slot.sub,
                                modifier = Modifier.weight(1f),
                                isSolar = slot.isSolar,
                                isCurrent = slot.isCurrent
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Islamic Date: 19 Rabi' al-Awwal 1448H", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                        Text("Qibla: 292° WNW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 2. MONTHLY CALENDAR GRID
        if (settings.enabledWidgets["widget-calendar-month"] != false) {
            item {
                CollapsibleWidget(
                    title = "calendar • october 2026",
                    collapsedSummary = {
                        Text("Fri, Oct 2", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // 7-day header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted, modifier = Modifier.weight(1f))
                            }
                        }

                        // Weeks
                        CalendarWeekRow(listOf("28" to false, "29" to false, "30" to false, "1" to true, "2" to true, "3" to true, "4" to true), today = "2")
                        CalendarWeekRow(listOf("5" to true, "6" to true, "7" to true, "8" to true, "9" to true, "10" to true, "11" to true))
                        CalendarWeekRow(listOf("12" to true, "13" to true, "14" to true, "15" to true, "16" to true, "17" to true, "18" to true))
                    }
                }
            }
        }

        // 3. TODAY'S EVENTS / TASKS
        if (settings.enabledWidgets["widget-tasks"] != false) {
            item {
                CollapsibleWidget(title = "today's events") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventRow(
                            title = "Sprint Planning & Meena Review",
                            location = "Google Meet • Hana Innovation",
                            time = "10:00 AM",
                            duration = "45m",
                            accent = MaterialTheme.colorScheme.primary
                        )
                        EventRow(
                            title = "Solat Jumaat & Lunch",
                            location = "Masjid Wilayah Persekutuan",
                            time = "01:00 PM",
                            duration = "1h 30m",
                            accent = MeenaProfitGreen
                        )
                        EventRow(
                            title = "Architecture Sync with Core Team",
                            location = "Online • Jetpack Compose 1.7",
                            time = "03:30 PM",
                            duration = "30m",
                            accent = MeenaBorder
                        )
                    }
                }
            }
        }

        // 4. QUICK NOTES (Modular Widget Moved Below Tasks)
        if (settings.enabledWidgets["widget-notes"] != false) {
            item {
                CollapsibleWidget(
                    title = "quick notes",
                    collapsedSummary = {
                        Text("${quickNotes.size} notes pinned", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                    }
                ) {
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
                                Text(
                                    text = "✕",
                                    fontSize = 12.sp,
                                    color = MeenaTextMuted,
                                    modifier = Modifier
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            quickNotes.removeAt(index)
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }

                        // Add quick note input row
                        var newNoteText by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newNoteText,
                                onValueChange = { newNoteText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Add quick note...", color = MeenaTextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(0.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MeenaSurface,
                                    unfocusedContainerColor = MeenaSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MeenaBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Button(
                                onClick = {
                                    if (newNoteText.isNotBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        quickNotes.add(newNoteText.trim())
                                        newNoteText = ""
                                    }
                                },
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("+", fontSize = 16.sp, color = Color.White)
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
private fun PrayerSlot(
    name: String,
    time: String,
    sub: String,
    modifier: Modifier = Modifier,
    isSolar: Boolean = false,
    isCurrent: Boolean = false
) {
    Box(
        modifier = modifier
            .background(MeenaSurface)
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isSolar -> MeenaSolarAmber.copy(alpha = 0.5f)
                    else -> MeenaBorder
                }
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSolar) MeenaSolarAmber else if (isCurrent) MaterialTheme.colorScheme.primary else MeenaTextMuted,
                fontSize = 9.sp
            )
            Text(
                time,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSolar) MeenaSolarAmber else MeenaTextWhite,
                fontSize = 13.sp
            )
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextMuted,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun CalendarWeekRow(days: List<Pair<String, Boolean>>, today: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (d, inMonth) ->
            val isToday = d == today
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isToday -> Color.Black
                        inMonth -> MeenaTextWhite
                        else -> MeenaTextMuted
                    }
                )
            }
        }
    }
}

@Composable
private fun EventRow(title: String, location: String, time: String, duration: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .drawBehind {
                val stroke = 3.dp.toPx()
                drawLine(
                    color = accent,
                    start = Offset(stroke / 2, 0f),
                    end = Offset(stroke / 2, size.height),
                    strokeWidth = stroke
                )
            }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(location, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
            Text(duration, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        }
    }
}
