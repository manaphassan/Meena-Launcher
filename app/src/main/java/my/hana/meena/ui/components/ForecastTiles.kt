package my.hana.meena.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.hana.meena.R
import my.hana.meena.data.system.WeatherData
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.widgets.WidgetState
import kotlin.math.roundToInt

@Composable
fun ForecastTiles(
    state: WidgetState<WeatherData>,
    isFahrenheit: Boolean,
    onNavigateToDetail: () -> Unit,
    onRequestLocation: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weatherData = (state as? WidgetState.Ready)?.data
        ?: (state as? WidgetState.Stale)?.data

    when (state) {
        is WidgetState.NeedsSetup -> {
            Text(
                text = "allow location",
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted,
                modifier = modifier
                    .fillMaxWidth()
                    .metroClickable(onClick = onRequestLocation)
                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
            )
        }
        is WidgetState.Loading -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.m),
                verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
            ) {
                MetroDottedProgress()
            }
        }
        is WidgetState.Error -> {
            Text(
                text = "couldn't reach the weather. tap to retry.",
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted,
                modifier = modifier
                    .fillMaxWidth()
                    .metroClickable(onClick = onRetry)
                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
            )
        }
        is WidgetState.Empty -> {
            // Hide the block per requirement
        }
        is WidgetState.Ready, is WidgetState.Stale -> {
            if (weatherData != null && weatherData.daily.isNotEmpty()) {
                // One row per day, whole words ("today", "tomorrow", "sunday"): no boxes, nothing cropped.
                val forecastItems = weatherData.daily.take(5)
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .metroClickable(onClick = onNavigateToDetail)
                ) {
                    forecastItems.forEachIndexed { index, day ->
                        val isToday = index == 0
                        val high = if (isFahrenheit) (day.maxTemp * 9f / 5f + 32f).roundToInt() else day.maxTemp.roundToInt()
                        val low = if (isFahrenheit) (day.minTemp * 9f / 5f + 32f).roundToInt() else day.minTemp.roundToInt()
                        val semanticDesc = "${day.dayLabel}, ${day.condition}, $high to $low degrees"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .semantics(mergeDescendants = true) { contentDescription = semanticDesc },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.dayLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite,
                                maxLines = 1,
                                modifier = Modifier.width(104.dp)
                            )
                            Icon(
                                painter = painterResource(id = weatherCodeToDrawableRes(day.weatherCode)),
                                contentDescription = null,
                                tint = MeenaTextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = day.condition,
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m)
                            )
                            Text(
                                text = "$high°",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MeenaTextWhite,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(40.dp)
                            )
                            Text(
                                text = "$low°",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MeenaTextMuted,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun weatherCodeToDrawableRes(code: Int): Int {
    return when (code) {
        0 -> R.drawable.ic_weather_clear
        1, 2 -> R.drawable.ic_weather_partly_cloudy
        3 -> R.drawable.ic_weather_cloudy
        45, 48 -> R.drawable.ic_weather_fog
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_weather_rain
        95, 96, 99 -> R.drawable.ic_weather_storm
        71, 73, 75, 77, 85, 86 -> R.drawable.ic_weather_cloudy
        else -> R.drawable.ic_weather_cloudy
    }
}
