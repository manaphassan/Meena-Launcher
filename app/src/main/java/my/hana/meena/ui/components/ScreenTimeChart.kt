package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import my.hana.meena.R
import my.hana.meena.theme.MeenaSpacing
import my.hana.meena.theme.MeenaTextMuted

/** Hour label in the person's clock style: "06" or "6am". */
@Composable
fun screenTimeHourLabel(hour: Int, is24Hour: Boolean): String = when {
    is24Hour -> "%02d".format(hour)
    hour == 0 -> stringResource(R.string.screen_time_hour_am, 12)
    hour < 12 -> stringResource(R.string.screen_time_hour_am, hour)
    hour == 12 -> stringResource(R.string.screen_time_hour_pm, 12)
    else -> stringResource(R.string.screen_time_hour_pm, hour - 12)
}

/**
 * Today's screen time as 24 flat bars, one per hour since midnight. A full bar is a whole hour
 * on screen. Labels mark midnight, 6, noon and 6. Read aloud as the busiest hour.
 */
@Composable
fun ScreenTimeHourChart(
    minutesByHour: List<Int>,
    currentHour: Int,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MeenaTextMuted
    val busiest = minutesByHour.indices.maxByOrNull { minutesByHour[it] } ?: 0
    val busiestMinutes = minutesByHour.getOrElse(busiest) { 0 }
    val description = androidx.compose.ui.res.pluralStringResource(
        R.plurals.screen_time_chart_description,
        busiestMinutes,
        screenTimeHourLabel(busiest, is24Hour),
        busiestMinutes
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(MeenaSpacing.s)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .drawBehind {
                    val slots = 24
                    val gap = 2.dp.toPx()
                    val slotWidth = size.width / slots
                    val barWidth = (slotWidth - gap).coerceAtLeast(1f)
                    val baseline = 1.dp.toPx()
                    val chartHeight = size.height - baseline
                    for (hour in 0 until slots) {
                        val minutes = minutesByHour.getOrElse(hour) { 0 }
                        if (minutes <= 0) continue
                        val barHeight = (chartHeight * minutes / 60f).coerceAtLeast(2.dp.toPx())
                        drawRect(
                            color = if (hour == currentHour) accent else accent.copy(alpha = 0.7f),
                            topLeft = Offset(hour * slotWidth, chartHeight - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    }
                    // Baseline across the day; the part still to come is fainter.
                    val nowX = ((currentHour + 1) * slotWidth).coerceAtMost(size.width)
                    drawRect(muted, Offset(0f, chartHeight), Size(nowX, baseline))
                    drawRect(muted.copy(alpha = 0.35f), Offset(nowX, chartHeight), Size(size.width - nowX, baseline))
                }
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            for (hour in listOf(0, 6, 12, 18)) {
                Text(
                    text = screenTimeHourLabel(hour, is24Hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** A thin bar under an app's name: how its time compares with the most-used app. */
@Composable
fun ScreenTimeShareBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
            .height(4.dp)
            .background(MaterialTheme.colorScheme.primary)
    )
}
