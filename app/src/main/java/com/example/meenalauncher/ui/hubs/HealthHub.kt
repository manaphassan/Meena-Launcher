package com.example.meenalauncher.ui.hubs

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.health.HealthRepository
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSolarAmber
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextSecondary
import com.example.meenalauncher.theme.MeenaTextWhite
import com.example.meenalauncher.ui.components.CollapsibleWidget

@Composable
fun HealthHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val deviceInfo by HealthRepository.deviceInfo.collectAsState()
    val fitTelemetry by HealthRepository.fitTelemetry.collectAsState()
    val airIndex by HealthRepository.airIndex.collectAsState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. CONNECTED DEVICE STATUS
        if (settings.enabledWidgets["widget-health-device"] != false) {
            item {
                CollapsibleWidget(
                title = "connected device status",
                collapsedSummary = {
                    Text(
                        text = "${deviceInfo.name} • ${deviceInfo.batteryPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaProfitGreen
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Device Primary Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MeenaSurface)
                            .border(1.dp, MeenaBorder)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                HealthRepository.openFitnessApp(context)
                            }
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "⌚",
                                        fontSize = 20.sp
                                    )
                                    Column {
                                        Text(
                                            text = deviceInfo.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Synced today at ${deviceInfo.lastSyncTime}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextMuted
                                        )
                                    }
                                }

                                // BT Badge
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF00A4EF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = deviceInfo.connectionType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Battery & Temperature telemetry pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Battery
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFF141414))
                                        .border(1.dp, MeenaBorder)
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Column {
                                        Text("BATTERY", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${deviceInfo.batteryPercent}%",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (deviceInfo.batteryPercent > 20) MeenaProfitGreen else Color(0xFFFF5252)
                                            )
                                            Text("Normal", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        }
                                    }
                                }

                                // Body Temperature
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFF141414))
                                        .border(1.dp, MeenaBorder)
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Column {
                                        Text("SKIN TEMP", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "%.1f°C".format(deviceInfo.bodyTempCelsius),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text("Wrist", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                        }
                                    }
                                }
                            }

                            // Action footer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TAP TO OPEN COMPANION APP →",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "SYNC NOW",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MeenaTextMuted,
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        HealthRepository.refreshTelemetry()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // 2. FIT TELEMETRY (STEPS, CALORIES, HEART RATE, SLEEP)
        if (settings.enabledWidgets["widget-health-telemetry"] != false) {
            item {
                CollapsibleWidget(
                title = "fit telemetry",
                collapsedSummary = {
                    Text(
                        text = "${fitTelemetry.stepsToday} steps • ${fitTelemetry.heartRateBpm} BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Step Goal Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MeenaSurface)
                            .border(1.dp, MeenaBorder)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text("DAILY STEP COUNTER", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Text(
                                        text = "${fitTelemetry.stepsToday}",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Goal: ${fitTelemetry.stepGoal}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                    val pct = ((fitTelemetry.stepsToday.toFloat() / fitTelemetry.stepGoal) * 100).toInt()
                                    Text(
                                        text = "$pct% Completed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaProfitGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { (fitTelemetry.stepsToday.toFloat() / fitTelemetry.stepGoal).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MeenaProfitGreen,
                                trackColor = Color(0xFF1E2820)
                            )
                        }
                    }

                    // 4-Quadrant Metric Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HealthMetricTile(
                            title = "CALORIES",
                            value = "${fitTelemetry.activeCaloriesKcal} kcal",
                            subtitle = "Target: 600 kcal",
                            accentColor = Color(0xFFFF8C00),
                            modifier = Modifier.weight(1f)
                        )
                        HealthMetricTile(
                            title = "DISTANCE",
                            value = "%.2f km".format(fitTelemetry.distanceKm),
                            subtitle = "${fitTelemetry.activeMinutes}m active",
                            accentColor = Color(0xFF00A4EF),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HealthMetricTile(
                            title = "HEART RATE",
                            value = "${fitTelemetry.heartRateBpm} BPM",
                            subtitle = "Resting: ${fitTelemetry.restingHeartRateBpm} • Max: ${fitTelemetry.maxHeartRateBpm}",
                            accentColor = Color(0xFFFF3B30),
                            modifier = Modifier.weight(1f)
                        )
                        HealthMetricTile(
                            title = "SLEEP TRACK",
                            value = fitTelemetry.sleepHoursFormatted,
                            subtitle = "Deep: ${fitTelemetry.deepSleepFormatted} (${fitTelemetry.sleepScore} score)",
                            accentColor = Color(0xFF9E86FF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

        // 3. AIR QUALITY INDEX (AQI)
        if (settings.enabledWidgets["widget-health-aqi"] != false) {
            item {
                CollapsibleWidget(
                title = "air index • kuala lumpur",
                collapsedSummary = {
                    Text(
                        text = "AQI ${airIndex.aqiValue} • ${airIndex.aqiStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(android.graphics.Color.parseColor(airIndex.aqiColorHex))
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Big Air Quality Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MeenaSurface)
                            .border(1.dp, MeenaBorder)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(airIndex.stationName.uppercase(), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${airIndex.aqiValue}",
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(android.graphics.Color.parseColor(airIndex.aqiColorHex))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color(android.graphics.Color.parseColor(airIndex.aqiColorHex)).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .border(1.dp, Color(android.graphics.Color.parseColor(airIndex.aqiColorHex)).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = airIndex.aqiStatus,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(android.graphics.Color.parseColor(airIndex.aqiColorHex))
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "US AQI STANDARD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MeenaTextMuted
                                )
                            }

                            Text(
                                text = airIndex.advisory,
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextSecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Pollutants & Weather Breakdown Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PollutantCard("PM2.5", "%.1f µg/m³".format(airIndex.pm25), "Primary", Modifier.weight(1f))
                        PollutantCard("PM10", "%.1f µg/m³".format(airIndex.pm10), "Dust/Pollen", Modifier.weight(1f))
                        PollutantCard("HUMIDITY", "${airIndex.humidityPercent}%", "Tropical", Modifier.weight(1f))
                        PollutantCard("UV INDEX", "${airIndex.uvIndex} (${airIndex.uvCategory})", "Midday Peak", Modifier.weight(1f))
                    }

                    // External Station Deep-link
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141414))
                            .border(1.dp, MeenaBorder)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                HealthRepository.openAirQualityDetails(context)
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VIEW LIVE AQM RADAR & SATELLITE MAP ↗",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
private fun HealthMetricTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 18.sp
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PollutantCard(
    param: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF141414))
            .border(1.dp, MeenaBorder)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(param, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted, fontSize = 8.5.sp)
        }
    }
}
