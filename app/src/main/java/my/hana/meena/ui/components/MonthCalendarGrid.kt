package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.hana.meena.R
import my.hana.meena.data.system.MonthGridBuilder
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * This month as a plain type grid: ISO week numbers, Monday first, today in the accent colour,
 * a small square under days with events. No cells, boxes or borders.
 */
@Composable
fun MonthCalendarGrid(
    nowMillis: Long,
    eventStartTimes: List<Long>,
    modifier: Modifier = Modifier
) {
    val now = remember(nowMillis / 86_400_000L) { Calendar.getInstance().apply { timeInMillis = nowMillis } }
    val year = now.get(Calendar.YEAR)
    val month = now.get(Calendar.MONTH)
    val today = now.get(Calendar.DAY_OF_MONTH)
    val grid = remember(year, month) { MonthGridBuilder.build(year, month) }
    val eventDays = remember(eventStartTimes, year, month) { MonthGridBuilder.eventDays(eventStartTimes, year, month) }
    val monthLabel = remember(year, month) { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time).lowercase() }
    val dayLabels = listOf("mo", "tu", "we", "th", "fr", "sa", "su")
    val accent = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        Text(text = monthLabel, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.calendar_week_short),
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            dayLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index >= 5) MeenaTextMuted else MeenaTextWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        grid.weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    text = week.weekNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                week.days.forEachIndexed { index, day ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (day != null) {
                            val isToday = day == today
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal,
                                color = when {
                                    isToday -> accent
                                    index >= 5 -> MeenaTextMuted
                                    else -> MeenaTextWhite
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .then(if (day in eventDays) Modifier.background(accent) else Modifier)
                            )
                        }
                    }
                }
            }
        }
    }
}
