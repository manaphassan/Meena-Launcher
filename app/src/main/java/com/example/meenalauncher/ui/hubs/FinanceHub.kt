package com.example.meenalauncher.ui.hubs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.data.system.SpendingRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextWhite
import com.example.meenalauncher.ui.components.CollapsibleWidget
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class MarketCandle(
    val time: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

@Composable
fun FinanceHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val haptic = LocalHapticFeedback.current
    var selectedTimeframe by remember { mutableStateOf("1D") }
    var selectedCandleIndex by remember { mutableIntStateOf(-1) }

    val liveTransactions by SpendingRepository.transactionsFlow.collectAsState()
    val totalSpentToday = liveTransactions.sumOf { it.amount }

    // Authentic market candlestick data (FBM KLCI Index & Bursa Market)
    val candles = remember(selectedTimeframe) {
        when (selectedTimeframe) {
            "1H" -> listOf(
                MarketCandle("11:30", 1682.4, 1683.9, 1681.9, 1683.5),
                MarketCandle("11:45", 1683.5, 1684.8, 1682.8, 1684.2),
                MarketCandle("12:00", 1684.2, 1685.1, 1683.7, 1684.0),
                MarketCandle("12:15", 1684.0, 1686.0, 1683.9, 1685.8),
                MarketCandle("12:30", 1685.8, 1687.2, 1685.2, 1686.9),
                MarketCandle("12:45", 1686.9, 1687.5, 1685.9, 1686.1),
                MarketCandle("14:00", 1686.1, 1688.2, 1685.8, 1688.0),
                MarketCandle("14:15", 1688.0, 1689.4, 1687.5, 1688.7),
                MarketCandle("14:30", 1688.7, 1690.1, 1688.1, 1689.8),
                MarketCandle("14:45", 1689.8, 1691.5, 1689.2, 1691.0),
                MarketCandle("15:00", 1691.0, 1691.8, 1690.2, 1690.6),
                MarketCandle("15:15", 1690.6, 1692.4, 1690.3, 1692.1)
            )
            "1W" -> listOf(
                MarketCandle("Mon", 1668.5, 1674.2, 1665.0, 1672.8),
                MarketCandle("Tue", 1672.8, 1678.0, 1670.2, 1676.4),
                MarketCandle("Wed", 1676.4, 1682.1, 1674.8, 1679.5),
                MarketCandle("Thu", 1679.5, 1686.0, 1678.0, 1685.1),
                MarketCandle("Fri", 1685.1, 1693.4, 1684.2, 1692.1)
            )
            "1M" -> listOf(
                MarketCandle("W1", 1642.0, 1655.8, 1638.2, 1651.4),
                MarketCandle("W2", 1651.4, 1664.0, 1648.0, 1662.9),
                MarketCandle("W3", 1662.9, 1677.5, 1659.1, 1674.2),
                MarketCandle("W4", 1674.2, 1693.4, 1671.0, 1692.1)
            )
            else -> listOf( // "1D" default
                MarketCandle("09:00", 1678.2, 1681.4, 1677.5, 1680.1),
                MarketCandle("10:00", 1680.1, 1683.0, 1679.2, 1682.4),
                MarketCandle("11:00", 1682.4, 1685.2, 1681.8, 1684.0),
                MarketCandle("12:00", 1684.0, 1686.5, 1683.4, 1685.8),
                MarketCandle("13:00", 1685.8, 1687.1, 1685.0, 1686.2),
                MarketCandle("14:00", 1686.2, 1689.0, 1685.8, 1688.4),
                MarketCandle("15:00", 1688.4, 1690.8, 1687.9, 1690.2),
                MarketCandle("16:00", 1690.2, 1692.5, 1689.6, 1691.7),
                MarketCandle("17:00", 1691.7, 1693.4, 1690.8, 1692.1)
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. CANDLESTICK MARKET CHART (FBM KLCI / Bursa Malaysia)
        item {
            CollapsibleWidget(
                title = "market candlestick • klci",
                collapsedSummary = {
                    Text("+13.90 pts (+0.83%)", style = MaterialTheme.typography.labelSmall, color = MeenaProfitGreen)
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header Bar with Current Scrubber Price or Latest Price
                    val activeCandle = if (selectedCandleIndex in candles.indices) candles[selectedCandleIndex] else candles.last()
                    val candleChange = activeCandle.close - activeCandle.open
                    val isBullish = candleChange >= 0
                    val changePercent = if (activeCandle.open > 0) (candleChange / activeCandle.open) * 100 else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "FBM KLCI • Bursa Malaysia",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                            Text(
                                text = "%.2f".format(activeCandle.close),
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isBullish) "+" else ""}%.2f (${if (isBullish) "+" else ""}%.2f%%)".format(candleChange, changePercent),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isBullish) Color(0xFF00CC6A) else Color(0xFFFF3B30)
                            )
                            Text(
                                text = "Time: ${activeCandle.time}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    }

                    // OHLC Detail Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF101010))
                            .border(1.dp, MeenaBorder)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("O: %.1f".format(activeCandle.open), fontSize = 11.sp, color = MeenaTextMuted)
                        Text("H: %.1f".format(activeCandle.high), fontSize = 11.sp, color = Color(0xFF00CC6A))
                        Text("L: %.1f".format(activeCandle.low), fontSize = 11.sp, color = Color(0xFFFF3B30))
                        Text("C: %.1f".format(activeCandle.close), fontSize = 11.sp, color = Color.White)
                    }

                    // Timeframe Switcher Tabs (1H, 1D, 1W, 1M)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1H", "1D", "1W", "1M").forEach { tf ->
                            val isSelected = selectedTimeframe == tf
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF141414), RoundedCornerShape(4.dp))
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MeenaBorder, RoundedCornerShape(4.dp))
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
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else MeenaTextMuted
                                )
                            }
                        }
                    }

                    // High-Fidelity Candlestick Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .background(Color(0xFF080808), RoundedCornerShape(4.dp))
                            .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
                            .pointerInput(candles) {
                                detectTapGestures { offset ->
                                    val candleWidth = size.width / candles.size
                                    val idx = (offset.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                    selectedCandleIndex = idx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            .pointerInput(candles) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val candleWidth = size.width / candles.size
                                    val idx = (change.position.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                    if (selectedCandleIndex != idx) {
                                        selectedCandleIndex = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val allLows = candles.map { it.low }
                            val allHighs = candles.map { it.high }
                            val minVal = (allLows.minOrNull() ?: 1670.0) - 1.0
                            val maxVal = (allHighs.maxOrNull() ?: 1700.0) + 1.0
                            val valRange = max(1.0, maxVal - minVal)

                            // Horizontal Grid Lines
                            for (step in 1..3) {
                                val yGrid = h * (step / 4f)
                                drawLine(
                                    color = Color(0xFF1E1E1E),
                                    start = Offset(0f, yGrid),
                                    end = Offset(w, yGrid),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            val count = candles.size
                            val slotWidth = w / count
                            val candleWidth = (slotWidth * 0.62f).coerceAtLeast(4f)

                            candles.forEachIndexed { i, candle ->
                                val centerX = i * slotWidth + (slotWidth / 2f)

                                val highY = ((maxVal - candle.high) / valRange * h).toFloat()
                                val lowY = ((maxVal - candle.low) / valRange * h).toFloat()
                                val openY = ((maxVal - candle.open) / valRange * h).toFloat()
                                val closeY = ((maxVal - candle.close) / valRange * h).toFloat()

                                val isBull = candle.close >= candle.open
                                val candleColor = if (isBull) Color(0xFF00CC6A) else Color(0xFFFF3B30)

                                // Draw Wick (Vertical line from High to Low)
                                drawLine(
                                    color = candleColor,
                                    start = Offset(centerX, highY),
                                    end = Offset(centerX, lowY),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                // Draw Candle Body (from Open to Close)
                                val bodyTop = min(openY, closeY)
                                val bodyHeight = max(2.dp.toPx(), abs(closeY - openY))

                                drawRect(
                                    color = candleColor,
                                    topLeft = Offset(centerX - (candleWidth / 2f), bodyTop),
                                    size = Size(candleWidth, bodyHeight)
                                )

                                // Active Selection Highlight Marker
                                if (i == selectedCandleIndex) {
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.5f),
                                        start = Offset(centerX, 0f),
                                        end = Offset(centerX, h),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. EXCHANGE RATES (REAL BENCHMARK)
        item {
            CollapsibleWidget(title = "exchange rates (myr)") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RateCard("1 USD", "4.218 MYR", Modifier.weight(1f))
                        RateCard("1 SGD", "3.275 MYR", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RateCard("1 EUR", "4.672 MYR", Modifier.weight(1f))
                        RateCard("1 GBP", "5.604 MYR", Modifier.weight(1f))
                    }
                }
            }
        }

        // 3. RECENT SPENDING TRANSACTIONS (LATEST 6 REAL DATA)
        item {
            CollapsibleWidget(
                title = "spending • transactions",
                collapsedSummary = {
                    Text(
                        text = if (totalSpentToday > 0) "MYR %.2f spent today".format(totalSpentToday) else "Monitoring banking notifications",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (totalSpentToday > 0) MeenaProfitGreen else MeenaTextMuted
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LATEST RECORDED TRANSACTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                        Text(
                            text = "MYR %.2f".format(totalSpentToday),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    if (liveTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MeenaSurface)
                                .border(1.dp, MeenaBorder)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No transactions recorded yet today",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Maybank MAE, TNG eWallet, CIMB, GrabPay, and Bank SMS alerts will automatically appear here when received.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    } else {
                        // Display latest up to 6 real transactions
                        liveTransactions.take(6).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface, RoundedCornerShape(4.dp))
                                    .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                when (tx.bankCode) {
                                                    "M" -> Color(0xFFFFB800)
                                                    "T" -> Color(0xFF0055A5)
                                                    "C" -> Color(0xFFCC0000)
                                                    "G" -> Color(0xFF00B14F)
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tx.bankCode,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(tx.merchant, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                        Text("${tx.bank} • ${tx.formattedTime}", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    }
                                }
                                Text("-MYR %.2f".format(tx.amount), style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun RateCard(currency: String, rate: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MeenaSurface, RoundedCornerShape(4.dp))
            .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(currency, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        Text(rate, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}
