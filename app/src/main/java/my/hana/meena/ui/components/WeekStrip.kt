package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.hana.meena.R
import my.hana.meena.data.system.StripDay
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Seven days in one row (AIO content, Metro layout): big date, small weekday, up to three
 * squares for events. Today in the accent colour, weekends muted. No boxes or cells.
 */
@Composable
fun WeekStrip(
    days: List<StripDay>,
    showEvents: Boolean,
    onDayClick: (StripDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val weekday = SimpleDateFormat("EEE", Locale.getDefault())
    val longDate = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())
    Row(modifier = modifier.fillMaxWidth()) {
        days.forEach { day ->
            val time = Calendar.getInstance().apply { timeInMillis = day.startMillis }.time
            val label = weekday.format(time).lowercase().take(3)
            val color = when {
                day.isToday -> accent
                day.isWeekend -> MeenaTextMuted
                else -> MeenaTextWhite
            }
            val dateText = longDate.format(time).lowercase()
            val dayShowsEvents = showEvents && day.hasEventInfo
            val description = if (dayShowsEvents) {
                pluralStringResource(R.plurals.week_strip_day_events, day.eventCount, dateText, day.eventCount)
            } else {
                dateText
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = description }
                    .metroClickable { onDayClick(day) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = if (day.isToday) FontWeight.Normal else FontWeight.Light,
                    color = color
                )
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.height(4.dp)) {
                    if (dayShowsEvents) {
                        repeat(day.eventCount.coerceAtMost(3)) { index ->
                            if (index > 0) Spacer(modifier = Modifier.width(3.dp))
                            Box(modifier = Modifier.size(4.dp).background(accent))
                        }
                    }
                }
            }
        }
    }
}
