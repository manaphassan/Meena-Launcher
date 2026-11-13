package my.hana.meena.ui.components

import my.hana.meena.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import my.hana.meena.ui.components.metroClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import my.hana.meena.data.system.JakimSchedule
import my.hana.meena.data.system.JakimSolatRepository
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DynamicSolatCelestialCard(
    schedule: JakimSchedule,
    onZoneChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSunPath: Boolean = false,
    showSyuruk: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    var currentMinutesOfDay by remember {
        val now = Calendar.getInstance()
        mutableStateOf(now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE))
    }
    var isZonePickerOpen by remember { mutableStateOf(false) }
    var isMonthlyScheduleOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            currentMinutesOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            delay(30_000)
        }
    }

    // Imsak marks the start of the fast, not a prayer, so the row shows the six JAKIM times.
    val shown = schedule.timelineItems.filter { it.name != "imsak" && (showSyuruk || it.name != "syuruk") }
    val nextId = shown.firstOrNull { it.totalMinutes > currentMinutesOfDay }?.id ?: shown.firstOrNull()?.id

    Column(modifier = modifier.fillMaxWidth()) {
        if (showSunPath) {
            AnimatedSunPath(schedule = schedule, currentMinutesOfDay = currentMinutesOfDay, nextName = nextId ?: "")
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            shown.forEach { item ->
                val isNext = item.id == nextId
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    Text(
                        text = item.name.lowercase(),
                        fontSize = 12.sp,
                        color = if (isNext) MaterialTheme.colorScheme.primary else MeenaTextMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = item.time24,
                        fontSize = 16.sp,
                        fontWeight = if (isNext) FontWeight.Normal else FontWeight.Light,
                        color = if (isNext) MaterialTheme.colorScheme.primary else MeenaTextWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = schedule.zoneName.lowercase(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 48.dp)
                    .metroClickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isZonePickerOpen = true
                    }
                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
            )
            Text(
                text = stringResource(R.string.solat_this_month),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .metroClickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isMonthlyScheduleOpen = true
                    }
                    .padding(
                        start = my.hana.meena.theme.MeenaSpacing.m,
                        top = my.hana.meena.theme.MeenaSpacing.m,
                        bottom = my.hana.meena.theme.MeenaSpacing.m
                    )
            )
        }
    }

    if (isZonePickerOpen) {
        ZonePickerDialog(
            currentZone = schedule.zone,
            onZoneSelected = { newZone ->
                onZoneChanged(newZone)
                isZonePickerOpen = false
            },
            onDismiss = { isZonePickerOpen = false }
        )
    }

    if (isMonthlyScheduleOpen) {
        MonthlyScheduleDialog(
            zone = schedule.zone,
            zoneName = schedule.zoneName,
            onDismiss = { isMonthlyScheduleOpen = false }
        )
    }
}

/** Optional sun path (off by default). The pulse only runs while it is on screen. */
@Composable
private fun AnimatedSunPath(schedule: JakimSchedule, currentMinutesOfDay: Int, nextName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "celestialAnim")
    val sunPulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunPulse"
    )
    val sunGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunAlpha"
    )
    CelestialSkyCanvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clipToBounds(),
        schedule = schedule,
        currentMinutesOfDay = currentMinutesOfDay,
        sunPulseRadius = sunPulseRadius,
        sunGlowAlpha = sunGlowAlpha,
        nextName = nextName
    )
}

@Composable
private fun CelestialSkyCanvas(
    modifier: Modifier,
    schedule: JakimSchedule,
    currentMinutesOfDay: Int,
    sunPulseRadius: Float,
    sunGlowAlpha: Float,
    nextName: String
) {
    val items = schedule.timelineItems.filter { it.name != "imsak" }
    if (items.size < 6) return

    val tSubuh = items[0].totalMinutes
    val tSyuruk = items[1].totalMinutes
    val tDhuhr = items[2].totalMinutes
    val tAsar = items[3].totalMinutes
    val tMaghrib = items[4].totalMinutes
    val tIsyak = items[5].totalMinutes

    val subuhLabel = stringResource(R.string.solat_name_subuh)
    val syurukLabel = stringResource(R.string.solat_name_syuruk)
    val asarLabel = stringResource(R.string.solat_name_asar)
    val maghribLabel = stringResource(R.string.solat_name_maghrib)
    val isyakLabel = stringResource(R.string.solat_name_isyak)
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.65f
        val peakY = h * 0.28f
        val centerX = w * 0.50f

        // Horizon line only: no filled sky, ground or cloud blocks (no boxes, Meena UI).
        drawLine(
            color = primaryColor,
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1.4.dp.toPx()
        )

        // Parabola equation: y = peakY + A * (x - centerX)^2
        // At Syuruk (x = 0.16 * w), y = horizonY
        val syurukX = w * 0.16f
        val dxSyuruk = syurukX - centerX
        val aCoeff = if (dxSyuruk != 0f) (horizonY - peakY) / (dxSyuruk * dxSyuruk) else 0f

        fun getY(x: Float): Float {
            val dx = x - centerX
            return peakY + aCoeff * dx * dx
        }

        // Node Coordinates
        val xSubuh = w * 0.08f
        val xSyuruk = syurukX
        val xDhuhr = centerX
        val xAsar = w * 0.67f
        val xMaghrib = w * 0.84f
        val xIsyak = w * 0.92f

        val ySubuh = getY(xSubuh)
        val ySyuruk = horizonY
        val yDhuhr = peakY
        val yAsar = getY(xAsar)
        val yMaghrib = horizonY
        val yIsyak = getY(xIsyak)

        // Calculate Sun Position along the curve based on current time
        val sunX = when {
            currentMinutesOfDay <= tSubuh -> xSubuh
            currentMinutesOfDay <= tSyuruk -> {
                val f = (currentMinutesOfDay - tSubuh).toFloat() / (tSyuruk - tSubuh).coerceAtLeast(1)
                xSubuh + f * (xSyuruk - xSubuh)
            }
            currentMinutesOfDay <= tDhuhr -> {
                val f = (currentMinutesOfDay - tSyuruk).toFloat() / (tDhuhr - tSyuruk).coerceAtLeast(1)
                xSyuruk + f * (xDhuhr - xSyuruk)
            }
            currentMinutesOfDay <= tAsar -> {
                val f = (currentMinutesOfDay - tDhuhr).toFloat() / (tAsar - tDhuhr).coerceAtLeast(1)
                xDhuhr + f * (xAsar - xDhuhr)
            }
            currentMinutesOfDay <= tMaghrib -> {
                val f = (currentMinutesOfDay - tAsar).toFloat() / (tMaghrib - tAsar).coerceAtLeast(1)
                xAsar + f * (xMaghrib - xAsar)
            }
            currentMinutesOfDay <= tIsyak -> {
                val f = (currentMinutesOfDay - tMaghrib).toFloat() / (tIsyak - tMaghrib).coerceAtLeast(1)
                xMaghrib + f * (xIsyak - xMaghrib)
            }
            else -> xIsyak
        }
        val sunY = getY(sunX)

        // Target Focus Ring marker right next to sun (matches reference ⊙)
        val targetX = (sunX + 13.dp.toPx()).coerceAtMost(w * 0.94f)
        val targetY = getY(targetX)

        // Draw Solid Trajectory (Start -> Current Sun)
        val solidPath = Path()
        val startX = w * 0.05f
        solidPath.moveTo(startX, getY(startX))
        val solidSteps = 30
        val solidStepX = (sunX - startX) / solidSteps.coerceAtLeast(1)
        for (i in 1..solidSteps) {
            val px = startX + i * solidStepX
            solidPath.lineTo(px, getY(px))
        }
        drawPath(
            path = solidPath,
            color = primaryColor,
            style = Stroke(width = 2.2.dp.toPx())
        )

        // Draw solid connector from Sun to Target Marker
        val connectorPath = Path()
        connectorPath.moveTo(sunX, sunY)
        connectorPath.lineTo(targetX, targetY)
        drawPath(
            path = connectorPath,
            color = primaryColor,
            style = Stroke(width = 2.0.dp.toPx())
        )

        // Draw Dashed Trajectory (Target Marker -> End)
        val dashedPath = Path()
        dashedPath.moveTo(targetX, targetY)
        val endX = w * 0.95f
        val dashedSteps = 30
        val dashedStepX = (endX - targetX) / dashedSteps.coerceAtLeast(1)
        for (i in 1..dashedSteps) {
            val px = targetX + i * dashedStepX
            dashedPath.lineTo(px, getY(px))
        }
        drawPath(
            path = dashedPath,
            color = primaryColor,
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        )

        // Draw Prayer Nodes
        // Subuh (below horizon, grey filled dot)
        drawCircle(
            color = primaryColor,
            radius = 3.5.dp.toPx(),
            center = Offset(xSubuh, ySubuh)
        )

        // Syuruk (at horizon, bright white dot)
        drawCircle(
            color = MeenaTextWhite,
            radius = 4.2.dp.toPx(),
            center = Offset(xSyuruk, ySyuruk)
        )

        // Asar (on downward slope, white ring)
        drawCircle(
            color = MeenaTextWhite,
            radius = 3.5.dp.toPx(),
            center = Offset(xAsar, yAsar),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Maghrib (at horizon, white ring)
        drawCircle(
            color = MeenaTextWhite,
            radius = 4.dp.toPx(),
            center = Offset(xMaghrib, yMaghrib),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Isyak (below horizon, grey ring)
        drawCircle(
            color = primaryColor,
            radius = 3.5.dp.toPx(),
            center = Offset(xIsyak, yIsyak),
            style = Stroke(width = 1.3.dp.toPx())
        )

        // Draw the Animated Glowing Sun at (sunX, sunY)
        // Outer pulsing halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = sunGlowAlpha * 0.60f),
                    primaryColor.copy(alpha = sunGlowAlpha * 0.22f),
                    Color.Transparent
                ),
                center = Offset(sunX, sunY),
                radius = sunPulseRadius.dp.toPx()
            ),
            radius = sunPulseRadius.dp.toPx(),
            center = Offset(sunX, sunY)
        )

        // Inner solid sun disk
        drawCircle(
            color = primaryColor,
            radius = 6.5.dp.toPx(),
            center = Offset(sunX, sunY)
        )

        // Sun bright white center shine
        drawCircle(
            color = MeenaTextWhite,
            radius = 2.2.dp.toPx(),
            center = Offset(sunX, sunY)
        )

        // Target Focus Ring marker right next to sun (matches reference ⊙)
        drawCircle(
            color = MeenaTextWhite,
            radius = 4.5.dp.toPx(),
            center = Offset(targetX, targetY),
            style = Stroke(width = 1.3.dp.toPx())
        )
        drawCircle(
            color = MeenaTextWhite,
            radius = 1.5.dp.toPx(),
            center = Offset(targetX, targetY)
        )

        // Draw Node Labels onto Native Canvas
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val labelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12.sp.toPx()
            color = android.graphics.Color.parseColor("#9DB2C0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // Syuruk label directly above-left of dot
        nativeCanvas.drawText(syurukLabel, xSyuruk - 14.dp.toPx(), ySyuruk - 9.dp.toPx(), labelPaint)

        // Subuh label to the right of dot
        labelPaint.textAlign = Paint.Align.LEFT
        nativeCanvas.drawText(subuhLabel, xSubuh + 8.dp.toPx(), ySubuh + 4.dp.toPx(), labelPaint)

        // Active prayer name centered directly above sun & target marker
        val sunLabelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12.sp.toPx()
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val labelCenterX = (sunX + targetX) / 2f
        nativeCanvas.drawText(nextName, labelCenterX, sunY - 14.dp.toPx(), sunLabelPaint)

        // Asar label to the upper-right
        labelPaint.textAlign = Paint.Align.LEFT
        nativeCanvas.drawText(asarLabel, xAsar + my.hana.meena.theme.MeenaSpacing.s.toPx(), yAsar - 7.dp.toPx(), labelPaint)

        // Maghrib label to the upper-right
        nativeCanvas.drawText(maghribLabel, xMaghrib + my.hana.meena.theme.MeenaSpacing.s.toPx(), yMaghrib - 9.dp.toPx(), labelPaint)

        // Isyak label to the left
        labelPaint.textAlign = Paint.Align.RIGHT
        nativeCanvas.drawText(isyakLabel, xIsyak - 8.dp.toPx(), yIsyak + 4.dp.toPx(), labelPaint)
    }
}

@Composable
private fun ZonePickerDialog(
    currentZone: String,
    onZoneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val zones = JakimSolatRepository.AVAILABLE_ZONES
    MetroListPicker(
        title = stringResource(R.string.solat_zone_title),
        items = zones.map {
            MetroPickerItem(
                key = it.code,
                title = stringResource(R.string.settings_status_pair, it.code.lowercase(), it.name.lowercase()),
                subtitle = it.state.lowercase()
            )
        },
        selectedKey = zones.firstOrNull { it.code.equals(currentZone, ignoreCase = true) }?.code ?: currentZone,
        onSelect = onZoneSelected,
        onDismiss = onDismiss
    )
}

@Composable
private fun MonthlyScheduleDialog(
    zone: String,
    zoneName: String,
    onDismiss: () -> Unit
) {
    val now = Calendar.getInstance()
    val monthLabel = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.UK).format(now.time).lowercase()
    var scheduleRows by remember(zone) { mutableStateOf<List<my.hana.meena.data.system.DailyPrayerRow>>(emptyList()) }
    
    LaunchedEffect(zone) {
        scheduleRows = JakimSolatRepository.fetchMonthlySchedule(zone)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(my.hana.meena.theme.MeenaSpacing.s),
            shape = RoundedCornerShape(0.dp),
            color = MeenaBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(my.hana.meena.theme.MeenaSpacing.m)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.solat_month_title, monthLabel),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MeenaTextWhite
                        )
                        Text(
                            text = stringResource(R.string.settings_status_pair, zone.lowercase(), zoneName.lowercase()),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .metroClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_thin_close), contentDescription = stringResource(R.string.action_close), tint = MeenaTextWhite, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s, horizontal = my.hana.meena.theme.MeenaSpacing.s),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.solat_table_day), modifier = Modifier.weight(1.1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.solat_name_subuh), modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.solat_name_syuruk), modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.solat_name_zohor), modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.solat_name_asar), modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.solat_name_maghrib), modifier = Modifier.weight(1.25f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
                    Text(stringResource(R.string.solat_name_isyak), modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(scheduleRows) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.s, horizontal = my.hana.meena.theme.MeenaSpacing.s),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${row.dayName.take(3).lowercase()} ${row.day}",
                                modifier = Modifier.weight(1.1f),
                                fontSize = 12.sp,
                                fontWeight = if (row.isToday) FontWeight.Medium else FontWeight.Normal,
                                color = if (row.isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite
                            )
                            Text(row.subuh, modifier = Modifier.weight(1f), fontSize = 12.sp, color = if (row.isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite)
                            Text(row.syuruk, modifier = Modifier.weight(1f), fontSize = 12.sp, color = if (row.isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite)
                            Text(row.dhuhr, modifier = Modifier.weight(1f), fontSize = 12.sp, color = if (row.isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite)
                            Text(row.asar, modifier = Modifier.weight(1f), fontSize = 12.sp, color = if (row.isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite)
                            Text(row.maghrib, modifier = Modifier.weight(1.25f), fontSize = 12.sp, color = if (row.isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite)
                            Text(row.isyak, modifier = Modifier.weight(1f), fontSize = 12.sp, color = if (row.isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite)
                        }
                    }
                }
            }
        }
    }
}


