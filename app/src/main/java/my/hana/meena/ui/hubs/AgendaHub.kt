package my.hana.meena.ui.hubs

import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.size
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
import my.hana.meena.theme.MeenaBlack
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
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
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
                        title = "waktu solat, jakim",
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
                        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                            quickNotes.forEachIndexed { index, note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MeenaSurface)
                                        .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.s),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MeenaTextWhite,
                                        modifier = Modifier.weight(1f)
                                    )
                                    androidx.compose.material3.Icon(
                                        painter = androidx.compose.ui.res.painterResource(my.hana.meena.R.drawable.ic_thin_close),
                                        contentDescription = "remove note",
                                        tint = MeenaTextMuted,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                quickNotes.removeAt(index)
                                            }
                                            .padding(12.dp)
                                    )
                                }
                            }

                            // Add quick note input row
                            var newNoteText by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = my.hana.meena.theme.MeenaSpacing.s),
                                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s),
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
                                        focusedTextColor = MeenaTextWhite,
                                        unfocusedTextColor = MeenaTextWhite
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
                                    Text("+", fontSize = 16.sp, color = MeenaTextWhite)
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
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .background(MeenaSurface)
            .drawBehind {
                if (isCurrent) {
                    val h = 3.dp.toPx()
                    drawRect(accent, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                }
            }
            .padding(my.hana.meena.theme.MeenaSpacing.s),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSolar) MeenaSolarAmber else if (isCurrent) MaterialTheme.colorScheme.primary else MeenaTextMuted,
                fontSize = 12.sp
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
                fontSize = 12.sp
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
                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isToday -> MeenaBlack
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
            .drawBehind {
                val stroke = 3.dp.toPx()
                drawLine(
                    color = accent,
                    start = Offset(stroke / 2, 0f),
                    end = Offset(stroke / 2, size.height),
                    strokeWidth = stroke
                )
            }
            .padding(my.hana.meena.theme.MeenaSpacing.m),
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

