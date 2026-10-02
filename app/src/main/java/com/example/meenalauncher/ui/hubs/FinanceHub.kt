package com.example.meenalauncher.ui.hubs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun FinanceHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val haptic = LocalHapticFeedback.current
    var selectedTimeframe by remember { mutableStateOf("Today") }

    val liveTransactions by SpendingRepository.transactionsFlow.collectAsState()
    val totalSpentToday = liveTransactions.sumOf { it.amount }

    var simulatedAlertMessage by remember { mutableStateOf<String?>(null) }

    val onSimulateTransaction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        SpendingRepository.addTransaction(
            bank = "Maybank MAE",
            bankCode = "M",
            merchant = "Jaya Grocer The Gardens",
            channel = "Maybank2u DuitNow QR",
            amount = 38.00
        )
        simulatedAlertMessage = "Maybank2u Alert: RM 38.00 spent at Jaya Grocer"
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. DUAL-LINE MARKET CHART (BTC-USD vs. FBM KLCI)
        if (settings.enabledWidgets["widget-finance-charts"] != false) {
            item {
                CollapsibleWidget(
                    title = "market overview",
                    collapsedSummary = {
                        val gain = when (selectedTimeframe) {
                            "Weekly" -> "+3.85% 7D"
                            "Monthly" -> "+7.92% 30D"
                            "Yearly" -> "+98.4% 1Y"
                            else -> "+2.84% Total"
                        }
                        Text(gain, style = MaterialTheme.typography.labelSmall, color = MeenaProfitGreen)
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("BTC-USD vs. FBM KLCI ($selectedTimeframe)", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                            val gain = when (selectedTimeframe) {
                                "Weekly" -> "+3.85% 7D"
                                "Monthly" -> "+7.92% 30D"
                                "Yearly" -> "+98.4% 1Y"
                                else -> "+2.84% Total"
                            }
                            Text(gain, style = MaterialTheme.typography.labelSmall, color = MeenaProfitGreen)
                        }

                        // Timeframe Switcher Tabs with Haptics and Touch Isolation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectDragGestures { _, _ -> /* isolate drag */ }
                                },
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Today", "Weekly", "Monthly", "Yearly").forEach { tf ->
                                val isSelected = selectedTimeframe == tf
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF141414))
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MeenaBorder)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedTimeframe = tf
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tf.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White else MeenaTextMuted
                                    )
                                }
                            }
                        }

                        // Canvas Vector Chart with Touch Isolation
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color(0xFF080808))
                                .border(1.dp, MeenaBorder)
                                .pointerInput(Unit) {
                                    detectDragGestures { _, _ -> /* isolate touch gestures */ }
                                }
                                .padding(8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Grid horizontal guidelines
                                drawLine(Color(0xFF1C1C1C), Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 1f)
                                drawLine(Color(0xFF1C1C1C), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
                                drawLine(Color(0xFF1C1C1C), Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 1f)

                                // BTC Line (Primary Accent) - using quadraticTo
                                val btcPath = Path().apply {
                                    when (selectedTimeframe) {
                                        "Weekly" -> {
                                            moveTo(0f, h * 0.65f)
                                            quadraticTo(w * 0.25f, h * 0.75f, w * 0.5f, h * 0.4f)
                                            quadraticTo(w * 0.75f, h * 0.15f, w, h * 0.25f)
                                        }
                                        "Monthly" -> {
                                            moveTo(0f, h * 0.85f)
                                            quadraticTo(w * 0.3f, h * 0.4f, w * 0.6f, h * 0.6f)
                                            quadraticTo(w * 0.8f, h * 0.2f, w, h * 0.15f)
                                        }
                                        "Yearly" -> {
                                            moveTo(0f, h * 0.92f)
                                            quadraticTo(w * 0.35f, h * 0.8f, w * 0.65f, h * 0.45f)
                                            quadraticTo(w * 0.85f, h * 0.25f, w, h * 0.08f)
                                        }
                                        else -> {
                                            moveTo(0f, h * 0.8f)
                                            quadraticTo(w * 0.25f, h * 0.65f, w * 0.5f, h * 0.45f)
                                            quadraticTo(w * 0.75f, h * 0.3f, w, h * 0.2f)
                                        }
                                    }
                                }
                                drawPath(btcPath, color = primaryColor, style = Stroke(width = 4f))

                                // KLCI Line (Profit Green) - using quadraticTo
                                val klciPath = Path().apply {
                                    when (selectedTimeframe) {
                                        "Weekly" -> {
                                            moveTo(0f, h * 0.55f)
                                            quadraticTo(w * 0.35f, h * 0.6f, w * 0.6f, h * 0.35f)
                                            quadraticTo(w * 0.85f, h * 0.45f, w, h * 0.3f)
                                        }
                                        "Monthly" -> {
                                            moveTo(0f, h * 0.6f)
                                            quadraticTo(w * 0.25f, h * 0.65f, w * 0.55f, h * 0.38f)
                                            quadraticTo(w * 0.85f, h * 0.3f, w, h * 0.28f)
                                        }
                                        "Yearly" -> {
                                            moveTo(0f, h * 0.75f)
                                            quadraticTo(w * 0.3f, h * 0.65f, w * 0.6f, h * 0.5f)
                                            quadraticTo(w * 0.85f, h * 0.38f, w, h * 0.25f)
                                        }
                                        else -> {
                                            moveTo(0f, h * 0.6f)
                                            quadraticTo(w * 0.35f, h * 0.7f, w * 0.6f, h * 0.45f)
                                            quadraticTo(w * 0.85f, h * 0.4f, w, h * 0.35f)
                                        }
                                    }
                                }
                                drawPath(klciPath, color = MeenaProfitGreen, style = Stroke(width = 3f))
                            }
                        }

                        val (btcText, klciText) = when (selectedTimeframe) {
                            "Weekly" -> "BTC: $65,420 (+4.69%)" to "KLCI: 1,684.10 (+1.02%)"
                            "Monthly" -> "BTC: $68,110 (+10.45%)" to "KLCI: 1,692.50 (+1.75%)"
                            "Yearly" -> "BTC: $64,280 (+134.6%)" to "KLCI: 1,678.90 (+16.5%)"
                            else -> "BTC: $64,280 (+3.1%)" to "KLCI: 1,678.90 (+0.42%)"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(btcText, style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(MeenaProfitGreen))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(klciText, style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                            }
                        }
                    }
                }
            }
        }

        // 2. WATCHLIST
        if (settings.enabledWidgets["widget-news-feed"] != false) {
            item {
                CollapsibleWidget(title = "watchlist") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        WatchlistRow("1818.KLSE", "Bursa Malaysia", "MYR 8.85", "+1.26%", MeenaProfitGreen)
                        WatchlistRow("HLAL.US", "Wahed FTSE USA", "$49.20", "+0.68%", MeenaProfitGreen)
                        WatchlistRow("ETH-USD", "Ethereum", "$2,640.10", "+2.15%", MeenaProfitGreen)
                    }
                }
            }
        }

        // 3. EXCHANGE RATES (MYR)
        if (settings.enabledWidgets["widget-exchange-rates"] != false) {
            item {
                CollapsibleWidget(title = "exchange rates (myr)") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RateCard("1 USD", "4.22 MYR", Modifier.weight(1f))
                            RateCard("1 GBP", "5.61 MYR", Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RateCard("100 JPY", "2.91 MYR", Modifier.weight(1f))
                            RateCard("1 EUR", "4.68 MYR", Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 4. SPENDING SUMMARY (CAPTURED FROM NOTIFICATIONS)
        if (settings.enabledWidgets["widget-spending-summary"] != false) {
            item {
                CollapsibleWidget(
                    title = "spending summary (captured)",
                    collapsedSummary = {
                        Text(
                            text = "MYR %.2f spent today".format(totalSpentToday),
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaProfitGreen
                        )
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Simulation alert notification banner if triggered
                        if (simulatedAlertMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F2B1D))
                                    .border(1.dp, MeenaProfitGreen)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔔 $simulatedAlertMessage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaProfitGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "✕",
                                    color = MeenaTextMuted,
                                    modifier = Modifier
                                        .clickable { simulatedAlertMessage = null }
                                        .padding(start = 6.dp)
                                )
                            }
                        }

                        // Summary Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SPENT TODAY", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Text("MYR %.2f".format(totalSpentToday), style = MaterialTheme.typography.headlineMedium, color = MeenaTextWhite)
                            }
                            Button(
                                onClick = { onSimulateTransaction() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(0.dp)
                            ) {
                                Text("⚡ Bank Alert", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Monthly Budget bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("MONTHLY BUDGET", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Text("MYR 1,420 / MYR 3,500 (40.5%)", style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
                            }
                            LinearProgressIndicator(
                                progress = { 0.405f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color(0xFF222222)
                            )
                        }

                        Text(
                            text = "TRANSACTION FEED (${liveTransactions.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Transactions Feed
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            liveTransactions.forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MeenaSurface)
                                        .border(1.dp, MeenaBorder)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.merchant, style = MaterialTheme.typography.bodyLarge)
                                        Text("${tx.channel} • ${tx.bank} • ${tx.formattedTime}", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                    }
                                    Text("-MYR %.2f".format(tx.amount), style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFF5252))
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
private fun WatchlistRow(symbol: String, name: String, price: String, change: String, changeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(price, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Text(change, style = MaterialTheme.typography.labelSmall, color = changeColor)
        }
    }
}

@Composable
private fun RateCard(currency: String, rate: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(currency, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        Text(rate, style = MaterialTheme.typography.bodyLarge)
    }
}
