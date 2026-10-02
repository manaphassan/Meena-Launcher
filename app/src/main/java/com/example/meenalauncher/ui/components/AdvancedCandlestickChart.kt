package com.example.meenalauncher.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.meenalauncher.data.finance.FinanceCandle
import com.example.meenalauncher.data.finance.FinanceTicker
import com.example.meenalauncher.data.finance.GoogleFinanceRepository
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced Candlestick Chart with interactive Date and Price axes,
 * crosshair scrubber, timeframe switching, and Google Finance integration.
 */
@Composable
fun AdvancedCandlestickChart(
    selectedTicker: FinanceTicker,
    onSelectTicker: (FinanceTicker) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedTimeframe by remember { mutableStateOf("1D") }
    var selectedCandleIndex by remember { mutableIntStateOf(-1) }

    // Fetch candles for the current symbol and timeframe
    val candles = remember(selectedTicker.symbol, selectedTimeframe) {
        GoogleFinanceRepository.getCandles(selectedTicker.symbol, selectedTimeframe)
    }

    val activeCandle: FinanceCandle = if (selectedCandleIndex in candles.indices) {
        candles[selectedCandleIndex]
    } else {
        candles.lastOrNull() ?: FinanceCandle("17:00", "02 Oct 2026", selectedTicker.price, selectedTicker.price, selectedTicker.price, selectedTicker.price)
    }

    val candleChange = activeCandle.close - activeCandle.open
    val isBullish = candleChange >= 0
    val changePercent = if (activeCandle.open > 0) (candleChange / activeCandle.open) * 100.0 else 0.0

    val isPennyStock = activeCandle.close < 10.0
    val priceFormat = if (isPennyStock) "%.2f" else "%.1f"
    val fullPriceFormat = if (isPennyStock) "%.2f" else "%.2f"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Ticker Selector Chip Carousel
        val allWatchlist by remember { mutableStateOf(GoogleFinanceRepository.watchlist.value) }
        val chipScrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allWatchlist.forEach { ticker ->
                val isSelected = ticker.symbol == selectedTicker.symbol
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) Color(0xFF162F38) else Color(0xFF13171C),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF26C6DA) else Color(0xFF222B35),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTicker(ticker)
                            selectedCandleIndex = -1
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ticker.tickerDisplay,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF26C6DA) else Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (ticker.changePercent >= 0) "+" else ""}${ticker.changePercent}%",
                            fontSize = 10.sp,
                            color = if (ticker.changePercent >= 0) Color(0xFF00CC6A) else Color(0xFFFF3B30)
                        )
                    }
                }
            }
        }

        // 2. Header Bar: Security Name, Price, Date & Live Change
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${selectedTicker.tickerDisplay} • ${selectedTicker.exchange}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8AA3B5),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google Finance ↗",
                        fontSize = 10.sp,
                        color = Color(0xFF26C6DA),
                        modifier = Modifier.clickable {
                            GoogleFinanceRepository.openGoogleFinance(context, selectedTicker.googleFinanceUrl)
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = if (selectedTicker.currency == "USD") "$" else "RM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0C4D0),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = fullPriceFormat.format(activeCandle.close),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isBullish) "+" else ""}${fullPriceFormat.format(candleChange)} (${if (isBullish) "+" else ""}${String.format(Locale.US, "%.2f", changePercent)}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBullish) Color(0xFF00CC6A) else Color(0xFFFF3B30)
                )
                Text(
                    text = if (activeCandle.dateLabel.isNotEmpty()) "${activeCandle.timeLabel} • ${activeCandle.dateLabel}" else activeCandle.timeLabel,
                    fontSize = 11.sp,
                    color = Color(0xFF8AA3B5)
                )
            }
        }

        // 3. OHLC Detail Pill (Live Scrubber Values)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13171C), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF222B35), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("O: ${priceFormat.format(activeCandle.open)}", fontSize = 11.sp, color = Color(0xFF8AA3B5))
            Text("H: ${priceFormat.format(activeCandle.high)}", fontSize = 11.sp, color = Color(0xFF00CC6A))
            Text("L: ${priceFormat.format(activeCandle.low)}", fontSize = 11.sp, color = Color(0xFFFF3B30))
            Text("C: ${priceFormat.format(activeCandle.close)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // 4. Timeframe Switcher Tabs (1H, 1D, 1W, 1M)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("1H", "1D", "1W", "1M").forEach { tf ->
                val isSelected = selectedTimeframe == tf
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF13171C),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF222B35),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTimeframe = tf
                            selectedCandleIndex = -1
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF8AA3B5)
                    )
                }
            }
        }

        // 5. High-Fidelity Candlestick Canvas with Integrated Date & Price Axes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF222B35), RoundedCornerShape(8.dp))
                .pointerInput(candles) {
                    detectTapGestures { offset ->
                        val priceAxisW = 54.dp.toPx()
                        val plotW = size.width - priceAxisW
                        if (candles.isNotEmpty() && offset.x <= plotW) {
                            val slotW = plotW / candles.size
                            val idx = (offset.x / slotW).toInt().coerceIn(0, candles.size - 1)
                            selectedCandleIndex = idx
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
                .pointerInput(candles) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val priceAxisW = 54.dp.toPx()
                        val plotW = size.width - priceAxisW
                        if (candles.isNotEmpty() && change.position.x <= plotW) {
                            val slotW = plotW / candles.size
                            val idx = (change.position.x / slotW).toInt().coerceIn(0, candles.size - 1)
                            if (selectedCandleIndex != idx) {
                                selectedCandleIndex = idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
                .clipToBounds()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val priceAxisWidth = 54.dp.toPx()
                val dateAxisHeight = 22.dp.toPx()

                val plotWidth = w - priceAxisWidth
                val plotHeight = h - dateAxisHeight

                if (candles.isEmpty()) return@Canvas

                val allLows = candles.map { it.low }
                val allHighs = candles.map { it.high }
                val margin = ((allHighs.maxOrNull() ?: 10.0) - (allLows.minOrNull() ?: 0.0)) * 0.08
                val minVal = (allLows.minOrNull() ?: 1.0) - margin.coerceAtLeast(0.01)
                val maxVal = (allHighs.maxOrNull() ?: 2.0) + margin.coerceAtLeast(0.01)
                val valRange = max(0.001, maxVal - minVal)

                val nativeCanvas = drawContext.canvas.nativeCanvas
                val axisTextPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 9.sp.toPx()
                    color = android.graphics.Color.parseColor("#7E92A2")
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

                // A. Price Axis: Horizontal Grid Lines & Y-Axis Labels
                val gridSteps = 4
                for (step in 0..gridSteps) {
                    val yNorm = step.toFloat() / gridSteps
                    val y = plotHeight * yNorm
                    val priceAtY = maxVal - (yNorm * valRange)

                    // Grid line across plot
                    drawLine(
                        color = Color(0xFF1B222B),
                        start = Offset(0f, y),
                        end = Offset(plotWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Price label on right axis
                    axisTextPaint.textAlign = Paint.Align.LEFT
                    val formattedPrice = if (isPennyStock) "%.2f".format(priceAtY) else "%.1f".format(priceAtY)
                    nativeCanvas.drawText(formattedPrice, plotWidth + 6.dp.toPx(), y + 3.dp.toPx(), axisTextPaint)
                }

                // Vertical boundary line separating chart from price axis
                drawLine(
                    color = Color(0xFF222B35),
                    start = Offset(plotWidth, 0f),
                    end = Offset(plotWidth, h),
                    strokeWidth = 1.dp.toPx()
                )

                // Horizontal boundary line separating chart from date axis
                drawLine(
                    color = Color(0xFF222B35),
                    start = Offset(0f, plotHeight),
                    end = Offset(w, plotHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // B. Draw Candlesticks
                val count = candles.size
                val slotWidth = plotWidth / count
                val candleWidth = (slotWidth * 0.64f).coerceAtLeast(3.dp.toPx())

                // Determine date label display stride
                val dateStride = when {
                    count <= 6 -> 1
                    count <= 10 -> 2
                    count <= 16 -> 3
                    else -> 4
                }

                candles.forEachIndexed { i, candle ->
                    val centerX = i * slotWidth + (slotWidth / 2f)

                    val highY = ((maxVal - candle.high) / valRange * plotHeight).toFloat()
                    val lowY = ((maxVal - candle.low) / valRange * plotHeight).toFloat()
                    val openY = ((maxVal - candle.open) / valRange * plotHeight).toFloat()
                    val closeY = ((maxVal - candle.close) / valRange * plotHeight).toFloat()

                    val isBull = candle.close >= candle.open
                    val candleColor = if (isBull) Color(0xFF00CC6A) else Color(0xFFFF3B30)

                    // Draw Wick (High to Low)
                    drawLine(
                        color = candleColor,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Draw Candle Body (Open to Close)
                    val bodyTop = min(openY, closeY)
                    val bodyHeight = max(2.dp.toPx(), abs(closeY - openY))
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(centerX - (candleWidth / 2f), bodyTop),
                        size = Size(candleWidth, bodyHeight)
                    )

                    // X-Axis Date / Time Labels
                    if (i % dateStride == 0 || i == count - 1) {
                        axisTextPaint.textAlign = Paint.Align.CENTER
                        nativeCanvas.drawText(
                            candle.timeLabel,
                            centerX,
                            plotHeight + 15.dp.toPx(),
                            axisTextPaint
                        )
                    }
                }

                // C. Active Scrubber Marker & Crosshair
                if (selectedCandleIndex in candles.indices) {
                    val candle = candles[selectedCandleIndex]
                    val centerX = selectedCandleIndex * slotWidth + (slotWidth / 2f)
                    val closeY = ((maxVal - candle.close) / valRange * plotHeight).toFloat()

                    // Vertical Crosshair Line
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, plotHeight),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Horizontal Crosshair Line
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(0f, closeY),
                        end = Offset(plotWidth, closeY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Right Y-Axis Floating Price Badge
                    drawRoundRect(
                        color = Color(0xFF26C6DA),
                        topLeft = Offset(plotWidth + 2.dp.toPx(), closeY - 8.dp.toPx()),
                        size = Size(priceAxisWidth - 4.dp.toPx(), 16.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    val badgePaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 9.sp.toPx()
                        color = android.graphics.Color.BLACK
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val badgeFormattedPrice = if (isPennyStock) "%.2f".format(candle.close) else "%.1f".format(candle.close)
                    nativeCanvas.drawText(
                        badgeFormattedPrice,
                        plotWidth + (priceAxisWidth / 2f),
                        closeY + 3.dp.toPx(),
                        badgePaint
                    )

                    // Bottom X-Axis Floating Date Badge
                    drawRoundRect(
                        color = Color(0xFF162F38),
                        topLeft = Offset(centerX - 22.dp.toPx(), plotHeight + 2.dp.toPx()),
                        size = Size(44.dp.toPx(), 18.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    val dateBadgePaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 9.sp.toPx()
                        color = android.graphics.Color.parseColor("#26C6DA")
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    nativeCanvas.drawText(
                        candle.timeLabel,
                        centerX,
                        plotHeight + 14.dp.toPx(),
                        dateBadgePaint
                    )
                }
            }
        }
    }
}
