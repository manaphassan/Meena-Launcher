package my.hana.meena.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            title = "weather",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (weatherData != null) {
                // Today's details
                val current = weatherData.current
                val todayDaily = weatherData.daily.firstOrNull()
                val rainProb = todayDaily?.precipitationProbability ?: 0

                val temp = if (isFahrenheit) (current.temperature * 9f / 5f + 32f).roundToInt() else current.temperature.roundToInt()

                Text(
                    text = "${current.locationName.lowercase()}",
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
                    Text(
                        text = current.condition,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MeenaTextWhite
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("rain chance", "$rainProb%")
                DetailRow("humidity", "${current.humidity}%")
                DetailRow("wind", "${current.windSpeed.toInt()} km/h")
                if (aqiData != null) {
                    DetailRow("air quality", "${aqiData.aqiValue} aqi")
                }

                Spacer(modifier = Modifier.weight(1f))

                val minutesAgo = ((System.currentTimeMillis() - weatherData.fetchedAt) / 60000L).coerceAtLeast(0)
                val freshnessText = if (minutesAgo == 0L) "updated just now" else "updated ${minutesAgo}m ago"
                Text(
                    text = "$freshnessText - open-meteo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            } else {
                Text(
                    text = "no weather data available.",
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
            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "←",
            style = MaterialTheme.typography.displayMedium,
            color = MeenaTextWhite,
            modifier = Modifier.metroClickable { onNavigateBack() }.padding(end = 12.dp)
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
            .padding(vertical = 6.dp),
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
