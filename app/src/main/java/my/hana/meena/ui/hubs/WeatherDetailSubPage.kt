package my.hana.meena.ui.hubs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.hana.meena.R
import my.hana.meena.data.health.AirQualityIndexInfo
import my.hana.meena.data.system.WeatherRepository
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.Sky
import my.hana.meena.ui.components.WeatherSky
import my.hana.meena.ui.components.WindArrow
import my.hana.meena.ui.components.airBandColor
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.widgets.WidgetState
import kotlin.math.roundToInt

/** The eight compass points, from where the wind comes (degrees from north). */
internal fun compassIndex(degrees: Int): Int = (((degrees % 360) + 360 + 22) % 360) / 45

/**
 * Weather in full: the sky behind it moves with the real weather (see [WeatherSky]). Rain falls
 * as hard as it's raining and leans with the wind, clouds drift, storms flash, and the page
 * tints toward the air quality. Tap the temperature to switch units; touch the sky to play.
 */
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
    val aqi = aqiData?.aqiValue?.takeIf { it > 0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeenaSurface)
    ) {
        if (weatherData != null) {
            val c = weatherData.current
            val recipe = remember(c.weatherCode, c.isDay, c.precipitationMm, c.windSpeed, c.windDirection, c.cloudCover, aqi) {
                Sky.recipe(c.weatherCode, c.isDay, c.precipitationMm, c.windSpeed, c.windDirection, c.cloudCover, aqi)
            }
            WeatherSky(recipe = recipe, animate = animate)
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            MetroAppBar(title = stringResource(R.string.weather_title), onNavigateBack = onNavigateBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(my.hana.meena.theme.MeenaSpacing.l),
                verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
            ) {
                if (weatherData == null) {
                    Text(
                        text = stringResource(R.string.weather_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeenaTextMuted
                    )
                    return@Column
                }
                val current = weatherData.current
                val todayDaily = weatherData.daily.firstOrNull()
                var showFahrenheit by remember(isFahrenheit) { mutableStateOf(isFahrenheit) }
                fun shown(celsius: Float) = if (showFahrenheit) celsius * 9f / 5f + 32f else celsius

                Text(
                    // The weather keeps a placeholder until a real place is known; don't show it as a place.
                    text = my.hana.meena.data.system.ClockInfo.placeName(current.locationName, isRealFix = true)
                        ?: stringResource(R.string.weather_near_you),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MeenaTextWhite,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Counts up to the temperature when the page opens and when units switch.
                    val target = shown(current.temperature)
                    val counted = remember { Animatable(if (animate) target - 12f else target) }
                    LaunchedEffect(target, animate) {
                        if (animate) counted.animateTo(target, tween(700, easing = FastOutSlowInEasing)) else counted.snapTo(target)
                    }
                    Text(
                        text = "${counted.value.roundToInt()}\u00b0",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.metroClickable(onClickLabel = stringResource(R.string.weather_switch_units)) {
                            showFahrenheit = !showFahrenheit
                        }
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

                DetailRow(stringResource(R.string.weather_feels_like), "${shown(current.apparentTemperature).roundToInt()}\u00b0")
                DetailRow(stringResource(R.string.weather_rain_chance), stringResource(R.string.percent_value, todayDaily?.precipitationProbability ?: 0))
                if (current.precipitationMm > 0f) {
                    DetailRow(stringResource(R.string.weather_rain_now), stringResource(R.string.weather_rain_mm, "%.1f".format(current.precipitationMm)))
                }
                DetailRow(stringResource(R.string.weather_humidity), stringResource(R.string.percent_value, current.humidity))
                DetailRow(stringResource(R.string.weather_clouds), stringResource(R.string.percent_value, current.cloudCover))

                val directions = stringArrayResource(R.array.wind_directions)
                DetailRow(
                    label = stringResource(R.string.weather_wind),
                    value = stringResource(R.string.weather_wind_from, current.windSpeed.toInt(), directions[compassIndex(current.windDirection)])
                ) {
                    WindArrow(
                        fromDegrees = current.windDirection,
                        strength = Sky.windStrength(current.windSpeed),
                        color = MeenaTextWhite,
                        animate = animate,
                        modifier = Modifier.padding(end = my.hana.meena.theme.MeenaSpacing.s)
                    )
                }

                if (aqi != null) {
                    val band = Sky.airBand(aqi)
                    val bandName = stringResource(
                        when (band) {
                            0 -> R.string.weather_air_good
                            1 -> R.string.weather_air_moderate
                            2 -> R.string.weather_air_sensitive
                            3 -> R.string.weather_air_unhealthy
                            4 -> R.string.weather_air_very_unhealthy
                            else -> R.string.weather_air_hazardous
                        }
                    )
                    DetailRow(stringResource(R.string.air_quality_title), stringResource(R.string.weather_aqi_band, aqi, bandName)) {
                        // The 6dp square mark, in the band's colour.
                        Box(
                            modifier = Modifier
                                .padding(end = my.hana.meena.theme.MeenaSpacing.s)
                                .size(my.hana.meena.theme.MeenaSpacing.s)
                                .background(airBandColor(band))
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (animate) {
                    Text(
                        text = stringResource(R.string.weather_sky_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )
                }
                val minutesAgo = ((System.currentTimeMillis() - weatherData.fetchedAt) / 60000L).coerceAtLeast(0)
                val freshnessText = if (minutesAgo == 0L) stringResource(R.string.state_updated_just_now) else stringResource(R.string.state_updated_ago, minutesAgo.toInt())
                Text(
                    text = stringResource(R.string.weather_source, freshnessText),
                    style = MaterialTheme.typography.labelSmall,
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
private fun DetailRow(label: String, value: String, lead: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MeenaTextMuted
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            lead?.invoke()
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MeenaTextWhite
            )
        }
    }
}
