package com.example.meenalauncher.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.finance.FinanceTicker
import com.example.meenalauncher.data.finance.GoogleFinanceRepository
import com.example.meenalauncher.data.finance.PortfolioAccount
import java.util.Locale

/**
 * Google Finance integration component matching user's Google Finance
 * Portfolios and Watchlist (AXIATA, BURSA, FFB, HLAL, SENHENG, Mplus+, Luno).
 */
@Composable
fun GoogleFinanceCard(
    selectedTickerSymbol: String,
    onSelectTicker: (FinanceTicker) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val portfolios by GoogleFinanceRepository.portfolios.collectAsState()
    val watchlist by GoogleFinanceRepository.watchlist.collectAsState()

    val totalValue = GoogleFinanceRepository.getTotalPortfolioValue()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF222B35), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13171C)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Google Finance Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Finance",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF8AA3B5)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        GoogleFinanceRepository.openGoogleFinance(context)
                    }
                ) {
                    Text(
                        text = "Buka Google Finance ↗",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF26C6DA)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1F242B), thickness = 1.dp)

            // 2. Portfolios Section (Total Value + Accounts)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PORTFOLIOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF8BA5B5)
                    )
                    Text(
                        text = "Total value",
                        fontSize = 11.sp,
                        color = Color(0xFF7E92A2)
                    )
                }

                Text(
                    text = "RM %.2f".format(totalValue),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Portfolio cards row (Mplus+ and Luno)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    portfolios.forEach { acc ->
                        PortfolioAccountPill(
                            account = acc,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1F242B), thickness = 1.dp)

            // 3. Watchlist Section with Sparklines & Ticker Selection
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WATCHLIST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF8BA5B5)
                    )
                    Text(
                        text = "Tap to view chart",
                        fontSize = 10.sp,
                        color = Color(0xFF7E92A2)
                    )
                }

                watchlist.forEach { item ->
                    val isSelected = item.symbol == selectedTickerSymbol
                    WatchlistItemRow(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTicker(item)
                        },
                        onOpenWeb = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            GoogleFinanceRepository.openGoogleFinance(context, item.googleFinanceUrl)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioAccountPill(
    account: PortfolioAccount,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF222B35), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = account.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "RM %.2f".format(account.value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            val formattedChange = String.format(Locale.US, "%.2f", Math.abs(account.change))
            val formattedPercent = String.format(Locale.US, "%.2f", account.changePercent)
            val changeSign = if (account.change >= 0) "+RM" else "-RM"
            val percentSign = if (account.changePercent >= 0) "+" else ""
            Text(
                text = "$changeSign$formattedChange ($percentSign$formattedPercent%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (account.change >= 0) Color(0xFF00CC6A) else Color(0xFFFF3B30)
            )
        }
    }
}

@Composable
private fun WatchlistItemRow(
    item: FinanceTicker,
    isSelected: Boolean,
    onClick: () -> Unit,
    onOpenWeb: () -> Unit
) {
    val isPositive = item.changePercent >= 0
    val isNeutral = item.changePercent == 0.0
    val color = when {
        isNeutral -> Color(0xFF8AA3B5)
        isPositive -> Color(0xFF00CC6A)
        else -> Color(0xFFFF3B30)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0xFF162F38) else Color(0xFF0D1117),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF26C6DA) else Color(0xFF1D242B),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Ticker & Name
        Column(modifier = Modifier.weight(1.3f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.tickerDisplay,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF26C6DA) else Color.White
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• CHART",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF26C6DA)
                    )
                }
            }
            Text(
                text = item.name,
                fontSize = 11.sp,
                color = Color(0xFF7E92A2),
                maxLines = 1
            )
        }

        // Center Column: Sparkline mini chart
        Box(
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            SparklineCanvas(points = item.sparkline, lineColor = color)
        }

        // Right Column: Price & Change Badge
        Column(
            modifier = Modifier.weight(1.1f),
            horizontalAlignment = Alignment.End
        ) {
            val currencySymbol = if (item.currency == "USD") "$" else "RM"
            val priceStr = if (item.price < 10.0) "%.2f".format(item.price) else "%.2f".format(item.price)
            Text(
                text = "$currencySymbol$priceStr",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", item.changePercent)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (isPositive) "▲" else if (isNeutral) "•" else "▼",
                    fontSize = 9.sp,
                    color = color
                )
            }
        }
    }
}

/**
 * Clean vector sparkline chart matching Google Finance.
 */
@Composable
private fun SparklineCanvas(
    points: List<Double>,
    lineColor: Color
) {
    if (points.size < 2) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val range = (maxVal - minVal).coerceAtLeast(0.001)

        val stepX = w / (points.size - 1)
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val yNorm = ((maxVal - pt) / range).toFloat()
            val y = 2.dp.toPx() + yNorm * (h - 4.dp.toPx())

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        // Gradient area under sparkline
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.22f),
                    Color.Transparent
                )
            )
        )

        // Stroke line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 1.6.dp.toPx())
        )

        // End circle dot
        val lastY = 2.dp.toPx() + (((maxVal - points.last()) / range).toFloat() * (h - 4.dp.toPx()))
        drawCircle(
            color = lineColor,
            radius = 2.5.dp.toPx(),
            center = Offset(w, lastY)
        )
    }
}
