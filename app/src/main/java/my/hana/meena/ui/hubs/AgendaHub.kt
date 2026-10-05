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
import androidx.compose.runtime.collectAsState
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
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.JakimSchedule
import my.hana.meena.data.system.JakimSolatRepository
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaSolarAmber
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.CollapsibleWidget
import my.hana.meena.ui.components.DynamicSolatCelestialCard

@Composable
fun AgendaHub(
    settings: MeenaUserSettings,
    listState: LazyListState,
    onUpdateSolatZone: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    
    val solatState by JakimSolatRepository.state.collectAsState()
    val jakimSchedule = (solatState as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data ?:
                       (solatState as? my.hana.meena.ui.widgets.WidgetState.Stale)?.data ?:
                       JakimSchedule()

    val quickNotes = remember {
        mutableStateListOf<String>()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        val agendaWidgets = settings.widgetConfigs.entries
            .filter { it.value.hubId == "agenda" && it.value.isEnabled }
            .sortedBy { it.value.position }

        items(agendaWidgets.size, key = { agendaWidgets[it].key }) { index ->
            val widgetId = agendaWidgets[index].key
            when (widgetId) {
                "widget-solat" -> {
                    // 1. WAKTU SOLAT JAKIM TIMELINE (With Celestial Animated Sun Arc)
                    CollapsibleWidget(
                        title = "waktu solat • jakim",
                        collapsedSummary = {
                            Text(jakimSchedule.nextPrayerLabel, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                        }
                    ) {
                        DynamicSolatCelestialCard(
                            schedule = jakimSchedule,
                            onZoneChanged = onUpdateSolatZone
                        )
                    }
                }
                "widget-calendar-month" -> {
                    // 2. MONTHLY CALENDAR GRID
                    CollapsibleWidget(
                        title = "calendar",
                        collapsedSummary = {
                            Text("needs setup", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                        }
                    ) {
                        Text(
                            text = "allow calendar access in settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                }
                "widget-tasks" -> {
                    // 3. TODAY'S EVENTS / TASKS
                    CollapsibleWidget(
                        title = "today's events",
                        collapsedSummary = {
                            Text("needs setup", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                        }
                    ) {
                        Text(
                            text = "add a task or event",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                }
                "widget-notes" -> {
                    // 4. QUICK NOTES (Modular Widget Moved Below Tasks)
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

