package my.hana.meena.ui.hubs

import androidx.compose.ui.res.stringResource
import my.hana.meena.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import my.hana.meena.data.system.WeatherRepository
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.widgets.WidgetState
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.data.health.AirQualityIndexInfo
import kotlin.math.roundToInt

@Composable
fun WeatherDetailSubPage(
    isFahrenheit: Boolean,
    aqiData: AirQualityIndexInfo?,
    animate: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val weatherState by WeatherRepository.state.collectAsState()
    val weatherData = (weatherState as? WidgetState.Ready)?.data
        ?: (weatherState as? WidgetState.Stale)?.data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeenaSurface)
    ) {
        MetroAppBar(
            title = stringResource(R.string.weather_title),
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(my.hana.meena.theme.MeenaSpacing.l),
            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
        ) {
            if (weatherData != null) {
                // Today's details
                val current = weatherData.current
                val todayDaily = weatherData.daily.firstOrNull()
                val rainProb = todayDaily?.precipitationProbability ?: 0

                val temp = if (isFahrenheit) (current.temperature * 9f / 5f + 32f).roundToInt() else current.temperature.roundToInt()

                Text(
                    // The weather keeps a placeholder until a real place is known; don't show it as a place.
                    text = my.hana.meena.data.system.ClockInfo.placeName(current.locationName, isRealFix = true)
                        ?: stringResource(R.string.weather_near_you),
                    style = MaterialTheme.typography.displaySmall,
                    color = MeenaTextWhite
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$temp°",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        my.hana.meena.ui.components.AnimatedWeatherIcon(
                            code = current.weatherCode,
                            isDay = current.isDay,
                            color = MeenaTextWhite,
                            size = 56.dp,
                            animate = animate
                        )
                        Text(
                            text = current.condition,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MeenaTextWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.m))

                DetailRow(stringResource(R.string.weather_rain_chance), stringResource(R.string.percent_value, rainProb))
                DetailRow(stringResource(R.string.weather_humidity), stringResource(R.string.percent_value, current.humidity))
                DetailRow(stringResource(R.string.weather_wind), stringResource(R.string.weather_wind_kmh, current.windSpeed.toInt()))
                if (aqiData != null) {
                    DetailRow(stringResource(R.string.air_quality_title), stringResource(R.string.weather_aqi, aqiData.aqiValue))
                }

                Spacer(modifier = Modifier.weight(1f))

                val minutesAgo = ((System.currentTimeMillis() - weatherData.fetchedAt) / 60000L).coerceAtLeast(0)
                val freshnessText = if (minutesAgo == 0L) stringResource(R.string.state_updated_just_now) else stringResource(R.string.state_updated_ago, minutesAgo.toInt())
                Text(
                    text = stringResource(R.string.weather_source, freshnessText),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            } else {
                Text(
                    text = stringResource(R.string.weather_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
            }
        }
    }
}

@Composable
private fun MetroAppBar(title: String, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = my.hana.meena.theme.MeenaSpacing.l, start = my.hana.meena.theme.MeenaSpacing.l, end = my.hana.meena.theme.MeenaSpacing.l, bottom = my.hana.meena.theme.MeenaSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_thin_back),
            contentDescription = stringResource(R.string.settings_back),
            tint = MeenaTextWhite,
            modifier = Modifier
                .size(48.dp)
                .metroClickable(onClick = onNavigateBack)
                .padding(my.hana.meena.theme.MeenaSpacing.m)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = MeenaTextWhite
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextWhite
        )
    }
}
