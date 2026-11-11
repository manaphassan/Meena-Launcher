package my.hana.meena.ui.components

import my.hana.meena.R
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
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val initialNow = remember { Calendar.getInstance() }
    var currentTimeStr by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(initialNow.time))
    }
    var currentMinutesOfDay by remember {
        mutableStateOf(initialNow.get(Calendar.HOUR_OF_DAY) * 60 + initialNow.get(Calendar.MINUTE))
    }

    var isZonePickerOpen by remember { mutableStateOf(false) }
    var isMonthlyScheduleOpen by remember { mutableStateOf(false) }

    // Live continuous time ticker updating time only when minute rolls over
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        var lastMinute = -1
        while (true) {
            val now = Calendar.getInstance()
            val minuteNow = now.get(Calendar.MINUTE)
            val minutesOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + minuteNow
            if (minuteNow != lastMinute) {
                lastMinute = minuteNow
                currentTimeStr = timeFormat.format(now.time)
                currentMinutesOfDay = minutesOfDay
            }
            delay(1000)
        }
    }

    // Dynamic sun glow & pulse animation
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // UPPER SECTION: SKY GRADIENT & CELESTIAL SUN ARC
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // Background celestial canvas with sky gradient, horizon line & lower dark zone
                CelestialSkyCanvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    schedule = schedule,
                    currentMinutesOfDay = currentMinutesOfDay,
                    sunPulseRadius = sunPulseRadius,
                    sunGlowAlpha = sunGlowAlpha
                )

                // Top Info Bar (Zone & Clock)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.m),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zone Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isZonePickerOpen = true
                            }
                            .padding(vertical = 0.dp)
                    ) {
                        Text(
                            text = schedule.zone,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MeenaTextWhite
                        )
                        Spacer(modifier = Modifier.width(my.hana.meena.theme.MeenaSpacing.s))
                        Text(
                            text = schedule.zoneName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Live Digital Clock & Timezone
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentTimeStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MeenaTextWhite
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "myt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Next Prayer Hero Summary (Top-Left overlay)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = my.hana.meena.theme.MeenaSpacing.m, top = my.hana.meena.theme.MeenaSpacing.l)
                ) {
                    Text(
                        text = "next",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 12.sp
                    )
                    Text(
                        text = schedule.nextPrayerName,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        color = MeenaTextWhite,
                        lineHeight = 34.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Text(
                            text = schedule.nextPrayerTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MeenaTextWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = schedule.nextPrayerRemaining,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // LOWER SECTION: 6-PRAYER TIMELINE BAR (Equal 1/6th width per prayer, guarantees 'isyak' is never squished)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = my.hana.meena.theme.MeenaSpacing.s, vertical = my.hana.meena.theme.MeenaSpacing.s),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                schedule.timelineItems.forEach { item ->
                    val isHighlighted = item.isNext
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isHighlighted) {
                                    Modifier
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
                                } else {
                                    Modifier.padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = item.name.lowercase(),
                                fontSize = 12.sp,
                                fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.time24,
                                fontSize = 13.sp,
                                fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MeenaTextWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Subtle divider line
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = 0.dp),
                color = MaterialTheme.colorScheme.primary,
                thickness = 1.dp
            )

            // FOOTER: DATE & INTERACTIVE ACTIONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.s),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Gregorian & Hijri Date
                Text(
                    text = "${schedule.formattedGregorianDate}  Â·  ${schedule.hijriDate}".lowercase(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Right: Interactive Action Buttons (Protected from line wrap, lowercase English, no symbols)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .metroClickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isMonthlyScheduleOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "monthly schedule",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .metroClickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isZonePickerOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "change zone",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // Zone Picker Modal Dialog
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

    // Monthly Timetable Modal Dialog
    if (isMonthlyScheduleOpen) {
        MonthlyScheduleDialog(
            zone = schedule.zone,
            zoneName = schedule.zoneName,
            onDismiss = { isMonthlyScheduleOpen = false }
        )
    }
}

@Composable
private fun CelestialSkyCanvas(
    modifier: Modifier,
    schedule: JakimSchedule,
    currentMinutesOfDay: Int,
    sunPulseRadius: Float,
    sunGlowAlpha: Float
) {
    val items = schedule.timelineItems
    if (items.size < 6) return

    val tSubuh = items[0].totalMinutes
    val tSyuruk = items[1].totalMinutes
    val tDhuhr = items[2].totalMinutes
    val tAsar = items[3].totalMinutes
    val tMaghrib = items[4].totalMinutes
    val tIsyak = items[5].totalMinutes

    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.65f
        val peakY = h * 0.28f
        val centerX = w * 0.50f

        // Draw Sky Gradient above Horizon
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor,
                    primaryColor,
                    primaryColor,
                    primaryColor
                ),
                startY = 0f,
                endY = horizonY
            ),
            topLeft = Offset.Zero,
            size = Size(w, horizonY)
        )

        // Draw Below Horizon Surface
        drawRect(
            color = primaryColor,
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )

        // Draw Horizon Separator Line
        drawLine(
            color = primaryColor,
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1.4.dp.toPx()
        )

        // Draw Soft Cloudy Shapes in the Sky (pure Metro geometric rectangles)
        val cloudColor = primaryColor
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(w * 0.10f, h * 0.12f),
            size = Size(85.dp.toPx(), 20.dp.toPx()),
            cornerRadius = CornerRadius.Zero
        )
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(w * 0.63f, h * 0.20f),
            size = Size(90.dp.toPx(), 22.dp.toPx()),
            cornerRadius = CornerRadius.Zero
        )
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(w * 0.65f, h * 0.38f),
            size = Size(70.dp.toPx(), 18.dp.toPx()),
            cornerRadius = CornerRadius.Zero
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

        // Target Focus Ring marker right next to sun (matches reference âŠ™)
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
            color = primaryColor,
            radius = 2.2.dp.toPx(),
            center = Offset(sunX, sunY)
        )

        // Target Focus Ring marker right next to sun (matches reference âŠ™)
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
        nativeCanvas.drawText("Syuruk", xSyuruk - 14.dp.toPx(), ySyuruk - 9.dp.toPx(), labelPaint)

        // Subuh label to the right of dot
        labelPaint.textAlign = Paint.Align.LEFT
        nativeCanvas.drawText("Subuh", xSubuh + 8.dp.toPx(), ySubuh + 4.dp.toPx(), labelPaint)

        // Active prayer name centered directly above sun & target marker
        val sunLabelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12.sp.toPx()
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val labelCenterX = (sunX + targetX) / 2f
        nativeCanvas.drawText(schedule.nextPrayerName, labelCenterX, sunY - 14.dp.toPx(), sunLabelPaint)

        // Asar label to the upper-right
        labelPaint.textAlign = Paint.Align.LEFT
        nativeCanvas.drawText("Asar", xAsar + my.hana.meena.theme.MeenaSpacing.s.toPx(), yAsar - 7.dp.toPx(), labelPaint)

        // Maghrib label to the upper-right
        nativeCanvas.drawText("Maghrib", xMaghrib + my.hana.meena.theme.MeenaSpacing.s.toPx(), yMaghrib - 9.dp.toPx(), labelPaint)

        // Isyak label to the left
        labelPaint.textAlign = Paint.Align.RIGHT
        nativeCanvas.drawText("Isyak", xIsyak - 8.dp.toPx(), yIsyak + 4.dp.toPx(), labelPaint)
    }
}

@Composable
private fun ZonePickerDialog(
    currentZone: String,
    onZoneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(my.hana.meena.theme.MeenaSpacing.m),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.primary,
            border = androidx.compose.foundation.BorderStroke(1.dp, MeenaBorder)
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
                    Text(
                        text = "Pilih Zon Waktu Solat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MeenaTextWhite
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .metroClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_thin_close), contentDescription = "close", tint = MeenaTextWhite, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.m))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    items(JakimSolatRepository.AVAILABLE_ZONES) { zone ->
                        val isSelected = zone.code.equals(currentZone, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface,
                                    RoundedCornerShape(0.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onZoneSelected(zone.code)
                                }
                                .padding(my.hana.meena.theme.MeenaSpacing.m),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${zone.code} Â· ${zone.state}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MeenaTextMuted
                                )
                                Text(
                                    text = zone.name,
                                    fontSize = 13.sp,
                                    color = MeenaTextWhite
                                )
                            }
                            if (isSelected) {
                                Text("âœ“", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyScheduleDialog(
    zone: String,
    zoneName: String,
    onDismiss: () -> Unit
) {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH)
    val year = now.get(Calendar.YEAR)
    val monthNames = arrayOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
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
            color = MaterialTheme.colorScheme.primary,
            border = androidx.compose.foundation.BorderStroke(1.dp, MeenaBorder)
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
                            text = "prayer timetable Â· ${monthNames[month]} $year",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MeenaTextWhite
                        )
                        Text(
                            text = "$zone Â· $zoneName",
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
                        Icon(painterResource(R.drawable.ic_thin_close), contentDescription = "close", tint = MeenaTextWhite, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s, horizontal = my.hana.meena.theme.MeenaSpacing.s),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("day", modifier = Modifier.weight(1.1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text("subuh", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text("syuruk", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text("zohor", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text("asar", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text("maghrib", modifier = Modifier.weight(1.25f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
                    Text("isyak", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
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
                                .background(
                                    if (row.isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(0.dp)
                                )
                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.s, horizontal = my.hana.meena.theme.MeenaSpacing.s),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${row.dayName.take(3)} ${row.day}",
                                modifier = Modifier.weight(1.1f),
                                fontSize = 12.sp,
                                fontWeight = if (row.isToday) FontWeight.Medium else FontWeight.Normal,
                                color = if (row.isToday) MaterialTheme.colorScheme.primary else MeenaTextWhite
                            )
                            Text(row.subuh, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(row.syuruk, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(row.dhuhr, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MeenaTextWhite)
                            Text(row.asar, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MeenaTextWhite)
                            Text(row.maghrib, modifier = Modifier.weight(1.25f), fontSize = 12.sp, color = MeenaTextWhite)
                            Text(row.isyak, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MeenaTextWhite)
                        }
                    }
                }
            }
        }
    }
}


