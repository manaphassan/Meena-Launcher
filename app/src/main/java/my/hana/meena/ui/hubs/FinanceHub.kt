package my.hana.meena.ui.hubs

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
import my.hana.meena.data.finance.FinanceTicker
import my.hana.meena.data.finance.GoogleFinanceRepository
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.SpendingRepository
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.ui.components.AdvancedCandlestickChart
import my.hana.meena.ui.components.CollapsibleWidget
import my.hana.meena.ui.components.GoogleFinanceCard

import my.hana.meena.ui.widgets.WidgetState

@Composable
fun FinanceHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val spendingState by SpendingRepository.state.collectAsState()
    val liveTransactions = (spendingState as? WidgetState.Ready)?.data ?: emptyList()
    val totalSpentToday = liveTransactions.sumOf { it.amount }

    val financeState by GoogleFinanceRepository.state.collectAsState()
    val financeData = (financeState as? WidgetState.Ready)?.data
    val watchlist = financeData?.watchlist ?: emptyList()

    var selectedTicker: FinanceTicker? by remember(watchlist) {
        mutableStateOf(watchlist.firstOrNull())
    }

    val financeWidgets = settings.widgetConfigs.entries
        .filter { it.value.hubId == "finance" && it.value.isEnabled }
        .sortedBy { it.value.position }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        items(financeWidgets.size, key = { financeWidgets[it].key }) { index ->
            val (widgetId, config) = financeWidgets[index]
            when (widgetId) {
                "widget-finance-charts" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CollapsibleWidget(
                            title = selectedTicker?.let { "candlestick chart • ${it.tickerDisplay.lowercase()}" } ?: "candlestick chart",
                            collapsedSummary = {
                                if (selectedTicker != null) {
                                    val isBull = selectedTicker!!.changePercent >= 0
                                    Text(
                                        "${if (isBull) "+" else ""}${selectedTicker!!.change} (${if (isBull) "+" else ""}${selectedTicker!!.changePercent}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isBull) MeenaProfitGreen else Color(0xFFFF3B30)
                                    )
                                } else {
                                    Text(
                                        "add your first holding",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                            }
                        ) {
                            if (selectedTicker != null) {
                                AdvancedCandlestickChart(
                                    selectedTicker = selectedTicker!!,
                                    onSelectTicker = { selectedTicker = it }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("add your first holding", color = MeenaTextMuted)
                                }
                            }
                        }

                        CollapsibleWidget(
                            title = "google finance • portfolio & watchlist",
                            collapsedSummary = {
                                val portValue = GoogleFinanceRepository.getTotalPortfolioValue()
                                Text(
                                    if (watchlist.isNotEmpty()) "RM ${"%.2f".format(portValue)} • ${watchlist.size} Watchlist" else "add your first holding",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (watchlist.isNotEmpty()) MeenaProfitGreen else MeenaTextMuted
                                )
                            }
                        ) {
                            if (selectedTicker != null) {
                                GoogleFinanceCard(
                                    selectedTickerSymbol = selectedTicker!!.symbol,
                                    onSelectTicker = { selectedTicker = it }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                    Text("add your first holding", color = MeenaTextMuted)
                                }
                            }
                        }
                    }
                }
                "widget-exchange-rates" -> {
                    CollapsibleWidget(
                        title = "exchange rates (myr)",
                        collapsedSummary = {
                            Text(
                                "pick a currency",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    ) {
                        Text(
                            text = "pick a currency in settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                }
                "widget-spending-summary" -> {
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
                                liveTransactions.take(6).forEach { tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MeenaSurface, RoundedCornerShape(0.dp))
                                            .border(1.dp, MeenaBorder, RoundedCornerShape(0.dp))
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
                                                        RoundedCornerShape(0.dp)
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
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun RateCard(currency: String, rate: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MeenaSurface, RoundedCornerShape(0.dp))
            .border(1.dp, MeenaBorder, RoundedCornerShape(0.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(currency, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        Text(rate, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}

