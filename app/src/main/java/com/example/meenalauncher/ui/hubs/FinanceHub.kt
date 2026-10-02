package com.example.meenalauncher.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.finance.FinanceTicker
import com.example.meenalauncher.data.finance.GoogleFinanceRepository
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.data.system.SpendingRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.ui.components.AdvancedCandlestickChart
import com.example.meenalauncher.ui.components.CollapsibleWidget
import com.example.meenalauncher.ui.components.GoogleFinanceCard

@Composable
fun FinanceHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val liveTransactions by SpendingRepository.transactionsFlow.collectAsState()
    val totalSpentToday = liveTransactions.sumOf { it.amount }

    val watchlist by GoogleFinanceRepository.watchlist.collectAsState()
    var selectedTicker by remember {
        mutableStateOf(
            GoogleFinanceRepository.getTicker("6888.KL") ?: watchlist.firstOrNull() ?: FinanceTicker(
                symbol = "6888.KL",
                tickerDisplay = "AXIATA",
                name = "Axiata Group Bhd",
                exchange = "KLSE",
                currency = "MYR",
                price = 1.65,
                change = 0.03,
                changePercent = 1.85,
                dayHigh = 1.67,
                dayLow = 1.62,
                sparkline = listOf(1.62, 1.63, 1.63, 1.66, 1.64, 1.65, 1.65),
                googleFinanceUrl = "https://www.google.com/finance/quote/AXIATA:KLSE"
            )
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. CANDLESTICK CHART WITH DATE & PRICE AXES
        item {
            CollapsibleWidget(
                title = "candlestick chart • ${selectedTicker.tickerDisplay.lowercase()}",
                collapsedSummary = {
                    val isBull = selectedTicker.changePercent >= 0
                    Text(
                        "${if (isBull) "+" else ""}${selectedTicker.change} (${if (isBull) "+" else ""}${selectedTicker.changePercent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBull) MeenaProfitGreen else Color(0xFFFF3B30)
                    )
                }
            ) {
                AdvancedCandlestickChart(
                    selectedTicker = selectedTicker,
                    onSelectTicker = { selectedTicker = it }
                )
            }
        }

        // 2. GOOGLE FINANCE (PORTFOLIOS & WATCHLIST)
        item {
            CollapsibleWidget(
                title = "google finance • portfolio & watchlist",
                collapsedSummary = {
                    Text(
                        "RM 375.51 • ${watchlist.size} Watchlist",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaProfitGreen
                    )
                }
            ) {
                GoogleFinanceCard(
                    selectedTickerSymbol = selectedTicker.symbol,
                    onSelectTicker = { selectedTicker = it }
                )
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
