package com.example.meenalauncher.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.meenalauncher.data.system.JakimSchedule
import com.example.meenalauncher.data.system.JakimSolatRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DynamicSolatCelestialCard(
    modifier: Modifier = Modifier,
    initialZone: String = "WLY01"
) {
    val haptic = LocalHapticFeedback.current
    var selectedZone by remember { mutableStateOf(initialZone) }
    var schedule by remember(selectedZone) { mutableStateOf(JakimSolatRepository.getTodaySchedule(selectedZone)) }

    val initialNow = remember { Calendar.getInstance() }
    var currentTimeStr by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(initialNow.time))
    }
    var currentMinutesOfDay by remember {
        mutableStateOf(initialNow.get(Calendar.HOUR_OF_DAY) * 60 + initialNow.get(Calendar.MINUTE))
    }

    var isZonePickerOpen by remember { mutableStateOf(false) }
    var isMonthlyScheduleOpen by remember { mutableStateOf(false) }

    // Live continuous time ticker updating prayer schedule only when minute rolls over or zone changes
    LaunchedEffect(selectedZone) {
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
                schedule = JakimSolatRepository.getTodaySchedule(selectedZone)
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF222B35), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13171C)),
        shape = RoundedCornerShape(20.dp)
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = schedule.zone,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = schedule.zoneName,
                            fontSize = 11.sp,
                            color = Color(0xFFB0C4D0)
                        )
                    }

                    // Live Digital Clock & Timezone
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentTimeStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MYT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8AA3B5)
                        )
                    }
                }

                // Next Prayer Hero Summary (Top-Left overlay)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 38.dp)
                ) {
                    Text(
                        text = "SETERUSNYA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF8BA5B5),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = schedule.nextPrayerName,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 34.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = schedule.nextPrayerTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = schedule.nextPrayerRemaining,
                            fontSize = 12.sp,
                            color = Color(0xFF9FB6C5)
                        )
                    }
                }
            }

            // LOWER SECTION: 6-PRAYER TIMELINE BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                schedule.timelineItems.forEach { item ->
                    val isHighlighted = item.isNext
                    if (isHighlighted) {
                        // Highlighted Active/Upcoming Prayer Card (matches reference)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF162F38), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF224B57), RoundedCornerShape(10.dp))
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = item.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF26C6DA)
                                )
                                Text(
                                    text = item.time24,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF26C6DA)
                                )
                            }
                        }
                    } else {
                        // Regular Prayer Column
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 11.sp,
                                color = Color(0xFF7E92A2)
                            )
                            Text(
                                text = item.time24,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Subtle divider line
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                color = Color(0xFF1D242B),
                thickness = 1.dp
            )

            // FOOTER: DATE & INTERACTIVE ACTIONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Gregorian & Hijri Date
                Text(
                    text = "${schedule.formattedGregorianDate}  •  ${schedule.hijriDate}",
                    fontSize = 9.5.sp,
                    color = Color(0xFF8697A6),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Right: Interactive Action Buttons (Protected from line wrap)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jadual sebulan",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF26C6DA),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isMonthlyScheduleOpen = true
                        }
                    )
                    Text(
                        text = "Tukar zon →",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF26C6DA),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isZonePickerOpen = true
                        }
                    )
                }
            }
        }
    }

    // Zone Picker Modal Dialog
    if (isZonePickerOpen) {
        ZonePickerDialog(
            currentZone = selectedZone,
            onZoneSelected = { newZone ->
                selectedZone = newZone
                isZonePickerOpen = false
            },
            onDismiss = { isZonePickerOpen = false }
        )
    }

    // Monthly Timetable Modal Dialog
    if (isMonthlyScheduleOpen) {
        MonthlyScheduleDialog(
            zone = selectedZone,
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
                    Color(0xFF0D2535),
                    Color(0xFF143346),
                    Color(0xFF1D455C),
                    Color(0xFF244F67)
                ),
                startY = 0f,
                endY = horizonY
            ),
            topLeft = Offset.Zero,
            size = Size(w, horizonY)
        )

        // Draw Below Horizon Surface
        drawRect(
            color = Color(0xFF13171C),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )

        // Draw Horizon Separator Line
        drawLine(
            color = Color(0x7741637D),
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1.4.dp.toPx()
        )

        // Draw Soft Cloudy Shapes in the Sky
        val cloudColor = Color(0x22A3C4D8)
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(w * 0.10f, h * 0.12f),
            size = Size(85.dp.toPx(), 22.dp.toPx()),
            cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx())
        )
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(w * 0.63f, h * 0.20f),
            size = Size(90.dp.toPx(), 24.dp.toPx()),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(w * 0.65f, h * 0.38f),
            size = Size(70.dp.toPx(), 20.dp.toPx()),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
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
            color = Color(0xFF86B2D1),
            style = Stroke(width = 2.2.dp.toPx())
        )

        // Draw solid connector from Sun to Target Marker
        val connectorPath = Path()
        connectorPath.moveTo(sunX, sunY)
        connectorPath.lineTo(targetX, targetY)
        drawPath(
            path = connectorPath,
            color = Color(0xFF86B2D1),
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
            color = Color(0xFF516A7A),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        )

        // Draw Prayer Nodes
        // Subuh (below horizon, grey filled dot)
        drawCircle(
            color = Color(0xFF8A9EA8),
            radius = 3.5.dp.toPx(),
            center = Offset(xSubuh, ySubuh)
        )

        // Syuruk (at horizon, bright white dot)
        drawCircle(
            color = Color.White,
            radius = 4.2.dp.toPx(),
            center = Offset(xSyuruk, ySyuruk)
        )

        // Asar (on downward slope, white ring)
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(xAsar, yAsar),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Maghrib (at horizon, white ring)
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(xMaghrib, yMaghrib),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Isyak (below horizon, grey ring)
        drawCircle(
            color = Color(0xFF8A9EA8),
            radius = 3.5.dp.toPx(),
            center = Offset(xIsyak, yIsyak),
            style = Stroke(width = 1.3.dp.toPx())
        )

        // Draw the Animated Glowing Sun at (sunX, sunY)
        // Outer pulsing halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFB300).copy(alpha = sunGlowAlpha * 0.60f),
                    Color(0xFFFFB300).copy(alpha = sunGlowAlpha * 0.22f),
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
            color = Color(0xFFFFB300),
            radius = 6.5.dp.toPx(),
            center = Offset(sunX, sunY)
        )

        // Sun bright white center shine
        drawCircle(
            color = Color(0xFFFFFDE7),
            radius = 2.2.dp.toPx(),
            center = Offset(sunX, sunY)
        )

        // Target Focus Ring marker right next to sun (matches reference ⊙)
        drawCircle(
            color = Color.White,
            radius = 4.5.dp.toPx(),
            center = Offset(targetX, targetY),
            style = Stroke(width = 1.3.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = 1.5.dp.toPx(),
            center = Offset(targetX, targetY)
        )

        // Draw Node Labels onto Native Canvas
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val labelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
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
            textSize = 11.sp.toPx()
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val labelCenterX = (sunX + targetX) / 2f
        nativeCanvas.drawText(schedule.nextPrayerName, labelCenterX, sunY - 14.dp.toPx(), sunLabelPaint)

        // Asar label to the upper-right
        labelPaint.textAlign = Paint.Align.LEFT
        nativeCanvas.drawText("Asar", xAsar + 6.dp.toPx(), yAsar - 7.dp.toPx(), labelPaint)

        // Maghrib label to the upper-right
        nativeCanvas.drawText("Maghrib", xMaghrib + 6.dp.toPx(), yMaghrib - 9.dp.toPx(), labelPaint)

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
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF13171C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222B35))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Zon Waktu Solat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(JakimSolatRepository.AVAILABLE_ZONES) { zone ->
                        val isSelected = zone.code.equals(currentZone, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFF1B3846) else MeenaSurface,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF26C6DA) else MeenaBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onZoneSelected(zone.code)
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${zone.code} • ${zone.state}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF26C6DA) else MeenaTextMuted
                                )
                                Text(
                                    text = zone.name,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                            if (isSelected) {
                                Text("✓", fontSize = 16.sp, color = Color(0xFF26C6DA), fontWeight = FontWeight.Bold)
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
    val monthNames = arrayOf("Januari", "Februari", "Mac", "April", "Mei", "Jun", "Julai", "Ogos", "September", "Oktober", "November", "Disember")
    val scheduleRows = remember(zone) { JakimSolatRepository.getMonthlySchedule(zone, month, year) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF13171C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222B35))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Jadual Solat • ${monthNames[month]} $year",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$zone • $zoneName",
                            fontSize = 11.sp,
                            color = Color(0xFF8AA3B5)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B232C), RoundedCornerShape(6.dp))
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hari", modifier = Modifier.weight(1.1f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8AA3B5))
                    Text("Subuh", modifier = Modifier.weight(1f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8AA3B5))
                    Text("Syuruk", modifier = Modifier.weight(1f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8AA3B5))
                    Text("Zohor", modifier = Modifier.weight(1f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8AA3B5))
                    Text("Asar", modifier = Modifier.weight(1f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8AA3B5))
                    Text("Maghrib", modifier = Modifier.weight(1.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8AA3B5), maxLines = 1, softWrap = false)
                    Text("Isyak", modifier = Modifier.weight(1f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8AA3B5))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(scheduleRows) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (row.isToday) Color(0xFF1B3D4F) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(vertical = 5.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${row.dayName.take(3)} ${row.day}",
                                modifier = Modifier.weight(1.1f),
                                fontSize = 10.sp,
                                fontWeight = if (row.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (row.isToday) Color(0xFF26C6DA) else Color.White
                            )
                            Text(row.subuh, modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color(0xFFB0C0CC))
                            Text(row.syuruk, modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color(0xFF8899A6))
                            Text(row.dhuhr, modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.White)
                            Text(row.asar, modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.White)
                            Text(row.maghrib, modifier = Modifier.weight(1.25f), fontSize = 10.sp, color = Color.White)
                            Text(row.isyak, modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
