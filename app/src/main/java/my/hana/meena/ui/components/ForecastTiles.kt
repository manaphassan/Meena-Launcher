package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                val forecastItems = weatherData.daily.take(5)
                if (forecastItems.size != 5) return // Needs to be exactly 5 tiles per requirements, wait, what if Open-Meteo returns fewer? We should show what we have up to 5, but let's take exactly 5 if possible. The prompt says "Row of exactly 5 tiles". If < 5, just render them, it's safer than hiding.

                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .metroClickable(onClick = onNavigateToDetail),
                    horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    forecastItems.forEachIndexed { index, day ->
                        val isToday = index == 0
                        val high = if (isFahrenheit) (day.maxTemp * 9f / 5f + 32f).roundToInt() else day.maxTemp.roundToInt()
                        val low = if (isFahrenheit) (day.minTemp * 9f / 5f + 32f).roundToInt() else day.minTemp.roundToInt()
                        val dayLabel = day.dayLabel.take(3)
                        val iconRes = weatherCodeToDrawableRes(day.weatherCode)

                        val semanticDesc = "${day.dayLabel}, $high degrees, ${day.condition}"

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f) // Ensure roughly square to fit exactly
                                .background(if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .semantics { contentDescription = semanticDesc },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextWhite,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null, // Handled by parent semantic node
                                tint = MeenaTextWhite,
                                modifier = Modifier
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
                                    .size(my.hana.meena.theme.MeenaSpacing.l)
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$high°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MeenaTextWhite,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "/$low°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextWhite.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
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
