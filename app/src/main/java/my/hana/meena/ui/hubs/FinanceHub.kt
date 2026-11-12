package my.hana.meena.ui.hubs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import my.hana.meena.ui.components.metroPressEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.components.metroFeatherIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.R
import my.hana.meena.data.finance.ExchangeRateRepository
import my.hana.meena.data.finance.FinanceHolding
import my.hana.meena.data.finance.FinanceRepository
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.SpendingRepository
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.MetroDialog
import my.hana.meena.ui.widgets.WidgetState

/**
 * Authentic Meena UI Money & Finance Hub.
 * Design language: Clean, light, open, fast (inspired by Windows Phone Metro UI & AIO Launcher).
 * Rules:
 * - 100% lowercase text, no ALL CAPS, no emojis.
 * - Zero cards, zero borders, zero boxes, zero elevation.
 * - Hierarchy comes strictly from type scale and generous 24dp margins.
 * - Real live sources: Frankfurter ECB exchange rates, Google Sheets bridge, bank SMS spending.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinanceHub(
    settings: MeenaUserSettings,
    listState: LazyListState,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var localShowAmounts by remember(settings.showFinanceAmounts) { mutableStateOf(settings.showFinanceAmounts) }
    var holdingToDelete by remember { mutableStateOf<FinanceHolding?>(null) }
    var showResetPortfolioDialog by remember { mutableStateOf(false) }

    // Initialize repositories on entry
    LaunchedEffect(Unit) {
        ExchangeRateRepository.initialize(context, baseCurrency = "MYR")
        FinanceRepository.initialize(context)
    }

    LaunchedEffect(settings) {
        val isSpendingEnabled = settings.widgetConfigs["widget-spending-summary"]?.isEnabled == true
        SpendingRepository.isOptedIn = isSpendingEnabled
    }

    val financeState by FinanceRepository.state.collectAsState()
    val exchangeRateState by ExchangeRateRepository.state.collectAsState()
    val spendingState by SpendingRepository.state.collectAsState()

    val liveTransactions = (spendingState as? WidgetState.Ready)?.data ?: emptyList()
    val totalSpentToday = liveTransactions.sumOf { it.amount }

    val financeWidgets = settings.widgetConfigs.entries
        .filter { (it.value.hubId == "money" || it.value.hubId == "finance") && it.value.isEnabled }
        .sortedBy { it.value.position }

    if (showResetPortfolioDialog) {
        MetroDialog(
            title = stringResource(R.string.dialog_reset_portfolio_title),
            message = stringResource(R.string.dialog_reset_portfolio_question),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
            onConfirm = {
                FinanceRepository.resetPortfolioStorage(context)
                showResetPortfolioDialog = false
            },
            onDismiss = { showResetPortfolioDialog = false }
        )
    }

    if (holdingToDelete != null) {
        val holding = holdingToDelete!!
        MetroDialog(
            title = "delete holding",
            message = "delete this holding (${holding.symbol.lowercase()})?",
            confirmText = "yes",
            dismissText = "no",
            onConfirm = {
                FinanceRepository.removeHolding(context, holding.id)
                holdingToDelete = null
            },
            onDismiss = { holdingToDelete = null }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.l)
    ) {
        item { Spacer(modifier = Modifier.height(my.hana.meena.theme.MeenaSpacing.m)) }

        items(financeWidgets.size, key = { financeWidgets[it].key }) { index ->
            val (widgetId, _) = financeWidgets[index]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .metroFeatherIn(index = index, reducedMotion = settings.reducedMotion)
            ) {
                when (widgetId) {
                    "widget-finance-charts" -> {
                        PortfolioSection(
                            state = financeState,
                            showAmounts = localShowAmounts,
                            onToggleShowAmounts = { localShowAmounts = !localShowAmounts },
                            onDeleteHolding = { holding -> holdingToDelete = holding },
                            onOpenSettings = onOpenSettings,
                            errorHint = if (FinanceRepository.isStorageLocked) stringResource(R.string.finance_unlock_failed_hint) else "tap to retry",
                            onRefresh = {
                                if (FinanceRepository.isStorageLocked) {
                                    showResetPortfolioDialog = true
                                } else {
                                    FinanceRepository.refreshSheetData(context, (financeState as? WidgetState.Ready)?.data?.sheetUrl ?: "")
                                }
                            }
                        )
                    }

                    "widget-exchange-rates" -> {
                        ExchangeRatesSection(
                            state = exchangeRateState,
                            onRefresh = { ExchangeRateRepository.refresh(context, "MYR", force = true) },
                            onOpenSettings = onOpenSettings
                        )
                    }

                    "widget-spending-summary" -> {
                        SpendingSection(
                            state = spendingState,
                            transactions = liveTransactions,
                            totalSpentToday = totalSpentToday,
                            onOpenSettings = onOpenSettings
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * Portfolio and Watchlist Section.
 * Strictly free-tier, Google Sheets bridge or local holdings.
 * Follows Meena UI: no cards, no borders, clean type hierarchy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortfolioSection(
    state: WidgetState<my.hana.meena.data.finance.FinanceData>,
    showAmounts: Boolean,
    onToggleShowAmounts: () -> Unit,
    onDeleteHolding: (FinanceHolding) -> Unit,
    onOpenSettings: () -> Unit,
    errorHint: String,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        Text(
            text = "portfolio",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        when (state) {
            is WidgetState.NeedsSetup -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onOpenSettings() }
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                    verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    Text(
                        text = "needs setup",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextWhite
                    )
                    Text(
                        text = "add your holdings or connect a google sheet in settings. tap to configure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }

            is WidgetState.Loading -> {
                Text(
                    text = "updating portfolio...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }

            is WidgetState.Ready, is WidgetState.Stale -> {
                val data = if (state is WidgetState.Ready) state.data else (state as WidgetState.Stale).data
                val totalValue = data.holdings.sumOf { it.currentValue }
                val totalCost = data.holdings.sumOf { it.totalCost }
                val totalChange = totalValue - totalCost
                val changePct = if (totalCost > 0) (totalChange / totalCost) * 100.0 else 0.0
                val isProfit = totalChange >= 0

                val context = LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onToggleShowAmounts() },
                    verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (showAmounts) "rm %.2f".format(totalValue) else "rm ·····",
                            fontSize = 32.sp,
                            style = MaterialTheme.typography.displaySmall,
                            color = MeenaTextWhite
                        )
                        if (data.holdings.isNotEmpty()) {
                            val changeText = if (showAmounts) {
                                "${if (isProfit) "+" else ""}rm %.2f (${if (isProfit) "+" else ""}%.1f%%)".format(totalChange, changePct)
                            } else {
                                "${if (isProfit) "+" else ""}··.·· (${if (isProfit) "+" else ""}%.1f%%)".format(changePct)
                            }
                            Text(
                                text = changeText,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isProfit) MeenaProfitGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Text(
                        text = if (data.sheetConnected) "updated just now · google sheets bridge" else "updated just now · local portfolio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )
                }

                // Holdings List
                if (data.holdings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                        data.holdings.forEach { holding ->
                            val hProfit = holding.profitLoss >= 0
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroPressEffect(isPressed)
                                    .combinedClickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = {
                                            FinanceRepository.openQuoteUrl(context, "https://www.google.com/finance/quote/${holding.symbol}")
                                        },
                                        onLongClick = {
                                            onDeleteHolding(holding)
                                        }
                                    )
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = holding.symbol.lowercase(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MeenaTextWhite
                                    )
                                    val sharesText = if (showAmounts) {
                                        "${holding.shares.toInt()} shares · avg rm %.2f".format(holding.averageCost)
                                    } else {
                                        "··· shares · avg rm ··.··"
                                    }
                                    Text(
                                        text = sharesText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val valText = if (showAmounts) "rm %.2f".format(holding.currentValue) else "rm ··.··"
                                    Text(
                                        text = valText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MeenaTextWhite
                                    )
                                    val profitText = if (showAmounts) {
                                        "${if (hProfit) "+" else ""}rm %.2f (%.1f%%)".format(holding.profitLoss, holding.profitLossPercent)
                                    } else {
                                        "${if (hProfit) "+" else ""}··.·· (%.1f%%)".format(holding.profitLossPercent)
                                    }
                                    Text(
                                        text = profitText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hProfit) MeenaProfitGreen else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Watchlist Items
                if (data.watchlist.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "watchlist",
                        style = MaterialTheme.typography.titleSmall,
                        color = MeenaTextMuted
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                        data.watchlist.forEach { item ->
                            val isBull = item.change >= 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable {
                                        FinanceRepository.openQuote(context, item)
                                    }
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.tickerDisplay.lowercase(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MeenaTextWhite
                                    )
                                    if (item.name.isNotBlank() && item.name != item.symbol) {
                                        Text(
                                            text = item.name.lowercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MeenaTextMuted
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "%.2f".format(item.price),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MeenaTextWhite
                                    )
                                    Text(
                                        text = "${if (isBull) "+" else ""}%.2f (%.1f%%)".format(item.change, item.changePercent),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isBull) MeenaProfitGreen else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is WidgetState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onRefresh() }
                ) {
                    Text(
                        text = state.message.lowercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = errorHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )
                }
            }

            is WidgetState.Empty -> {
                Text(
                    text = "no portfolio items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
        }
    }
}

/**
 * Exchange Rates Section backed by Frankfurter ECB reference rates.
 * Follows Meena UI: no cards, no borders, clean type hierarchy.
 */
@Composable
private fun ExchangeRatesSection(
    state: WidgetState<my.hana.meena.data.finance.ExchangeRatesData>,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        Text(
            text = "exchange rates",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        when (state) {
            is WidgetState.NeedsSetup -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onOpenSettings() }
                ) {
                    Text(
                        text = "needs setup",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeenaTextWhite
                    )
                    Text(
                        text = "pick currencies in settings. tap to configure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }

            is WidgetState.Loading -> {
                Text(
                    text = "fetching exchange rates...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }

            is WidgetState.Ready, is WidgetState.Stale -> {
                val data = if (state is WidgetState.Ready) state.data else (state as WidgetState.Stale).data
                val base = data.baseCurrency.lowercase()
                val isStale = state is WidgetState.Stale

                Text(
                    text = if (isStale) "updated earlier · offline cache" else "updated today · european central bank",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Rate rows
                Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                    data.rates.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            pair.forEach { item ->
                                val target = item.currency.lowercase()
                                val unit = if (target == "jpy" || target == "thb" || target == "idr") 100 else 1
                                val price = if (unit == 100) item.rateToBase * 100 else item.rateToBase

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .metroClickable { onRefresh() }
                                ) {
                                    Text(
                                        text = "$unit $target",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                    Text(
                                        text = "%.2f $base".format(price),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MeenaTextWhite
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            is WidgetState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onRefresh() }
                ) {
                    Text(
                        text = state.message.lowercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "tap to retry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )
                }
            }

            is WidgetState.Empty -> {
                Text(
                    text = "no exchange rates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
        }
    }
}

/**
 * Spending Section derived from parsed banking notifications.
 * Follows Meena UI: no cards, no borders, clean typography.
 */
@Composable
private fun SpendingSection(
    state: WidgetState<List<my.hana.meena.data.system.SpendingTransaction>>,
    transactions: List<my.hana.meena.data.system.SpendingTransaction>,
    totalSpentToday: Double,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "spending",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (totalSpentToday > 0 && state is WidgetState.Ready) {
                Text(
                    text = "rm %.2f today".format(totalSpentToday),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextWhite
                )
            }
        }

        when {
            state is WidgetState.NeedsSetup -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onOpenSettings() },
                    verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    Text(
                        text = "turn on spending",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextWhite
                    )
                    Text(
                        text = "tap to opt in and track payments from allowlisted banking apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }
            transactions.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onOpenSettings() },
                    verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    Text(
                        text = "all quiet today",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextWhite
                    )
                    Text(
                        text = "banking notifications from allowlisted payment apps will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }
            else -> {
            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                transactions.take(6).forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.merchant.lowercase(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MeenaTextWhite
                            )
                            Text(
                                text = "${tx.bank.lowercase()} · ${tx.formattedTime}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                        Text(
                            text = "-rm %.2f".format(tx.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
}



