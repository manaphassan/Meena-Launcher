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
import androidx.compose.foundation.layout.widthIn
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.components.metroFeatherIn
import my.hana.meena.ui.components.sensitive
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
import my.hana.meena.data.finance.KijangEmas
import my.hana.meena.data.finance.KijangEmasData
import my.hana.meena.data.finance.KijangEmasRepository
import my.hana.meena.data.finance.MarketQuote
import my.hana.meena.data.finance.Markets
import my.hana.meena.data.finance.MarketsData
import my.hana.meena.data.finance.MarketsRepository
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.SpendingRepository
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.MetroDialog
import my.hana.meena.ui.widgets.WidgetState
import my.hana.meena.ui.widgets.updatedAgoText

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
        MarketsRepository.initialize(context)
        KijangEmasRepository.initialize(context)
    }

    LaunchedEffect(settings) {
        val isSpendingEnabled = settings.widgetConfigs["widget-spending-summary"]?.isEnabled == true
        SpendingRepository.isOptedIn = isSpendingEnabled
    }

    val financeState by FinanceRepository.state.collectAsState()
    val exchangeRateState by ExchangeRateRepository.state.collectAsState()
    val marketsState by MarketsRepository.state.collectAsState()
    val kijangState by KijangEmasRepository.state.collectAsState()
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
            title = stringResource(R.string.finance_delete_title),
            message = stringResource(R.string.finance_delete_question, holding.symbol.lowercase()),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
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
                    "widget-markets" -> {
                        MarketsSection(
                            state = marketsState,
                            maxItems = settings.widgetLines,
                            onRefresh = { MarketsRepository.refresh(context, force = true) }
                        )
                    }

                    "widget-kijang-emas" -> {
                        KijangEmasSection(
                            state = kijangState,
                            maxItems = settings.widgetLines,
                            onRefresh = { KijangEmasRepository.refresh(context, force = true) }
                        )
                    }

                    "widget-finance-charts" -> androidx.compose.foundation.layout.Box(Modifier.sensitive()) {
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
                            maxPairs = settings.widgetLines * 2,
                            onRefresh = { ExchangeRateRepository.refresh(context, "MYR", force = true) },
                            onOpenSettings = onOpenSettings
                        )
                    }

                    "widget-spending-summary" -> androidx.compose.foundation.layout.Box(Modifier.sensitive()) {
                        SpendingSection(
                            state = spendingState,
                            transactions = liveTransactions,
                            totalSpentToday = totalSpentToday,
                            maxItems = settings.widgetLines,
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
            text = stringResource(R.string.widget_name_portfolio),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
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
                        text = stringResource(R.string.state_needs_setup),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextWhite
                    )
                    Text(
                        text = stringResource(R.string.finance_portfolio_setup),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }

            is WidgetState.Loading -> {
                Text(
                    text = stringResource(R.string.finance_portfolio_loading),
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
                            text = if (showAmounts) stringResource(R.string.finance_amount, "%.2f".format(totalValue)) else stringResource(R.string.finance_amount_hidden),
                            fontSize = 32.sp,
                            style = MaterialTheme.typography.displaySmall,
                            color = MeenaTextWhite
                        )
                        if (data.holdings.isNotEmpty()) {
                            val changeText = if (showAmounts) {
                                stringResource(R.string.finance_change, signedRm(totalChange), signedPercent(changePct))
                            } else {
                                stringResource(R.string.finance_change, stringResource(R.string.finance_hidden_short), signedPercent(changePct))
                            }
                            Text(
                                text = changeText,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isProfit) MeenaProfitGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    val syncedAt = data.sheetSyncedAt
                    Text(
                        text = when {
                            !data.sheetConnected -> stringResource(R.string.finance_source_local)
                            syncedAt == null -> stringResource(R.string.finance_sheet_not_loaded)
                            state is WidgetState.Stale -> stringResource(R.string.finance_source_sheet_saved, updatedAgoText(syncedAt))
                            else -> stringResource(R.string.finance_source_sheet, updatedAgoText(syncedAt))
                        },
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
                                        stringResource(R.string.finance_units_avg, java.math.BigDecimal.valueOf(holding.shares).stripTrailingZeros().toPlainString(), "%.2f".format(holding.averageCost))
                                    } else {
                                        stringResource(R.string.finance_units_hidden)
                                    }
                                    Text(
                                        text = sharesText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val valText = if (showAmounts) stringResource(R.string.finance_amount, "%.2f".format(holding.currentValue)) else stringResource(R.string.finance_amount_hidden)
                                    Text(
                                        text = valText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MeenaTextWhite
                                    )
                                    val profitText = if (showAmounts) {
                                        stringResource(R.string.finance_change, signedRm(holding.profitLoss), signedPercent(holding.profitLossPercent))
                                    } else {
                                        stringResource(R.string.finance_change, stringResource(R.string.finance_hidden_short), signedPercent(holding.profitLossPercent))
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
                        text = stringResource(R.string.finance_watchlist_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MeenaTextMuted
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                        data.watchlist.forEach { item ->
                            val isBull = item.change >= 0
                            val hasPrice = item.price > 0.0
                            val ticker = item.tickerDisplay.lowercase()
                            val name = item.name.lowercase().takeIf { item.name.isNotBlank() && item.name != item.symbol }
                            val muted = MeenaTextMuted
                            // AIO's one-line quote: "ticker name   price  change", or a dash when the sheet has no price.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable {
                                        FinanceRepository.openQuote(context, item)
                                    },
                                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = androidx.compose.ui.text.buildAnnotatedString {
                                        append(ticker)
                                        if (name != null) {
                                            pushStyle(androidx.compose.ui.text.SpanStyle(color = muted))
                                            append("  ")
                                            append(name)
                                            pop()
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MeenaTextWhite,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (hasPrice) {
                                    Text(
                                        text = "%.2f".format(item.price),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MeenaTextWhite
                                    )
                                    Text(
                                        text = signedPercent(item.changePercent),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isBull) MeenaProfitGreen else MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.finance_no_price),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MeenaTextMuted
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
                    text = stringResource(R.string.finance_portfolio_empty),
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
    maxPairs: Int,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        Text(
            text = stringResource(R.string.rates_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
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
                        text = stringResource(R.string.state_needs_setup),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeenaTextWhite
                    )
                    Text(
                        text = stringResource(R.string.rates_setup),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }

            is WidgetState.Loading -> {
                Text(
                    text = stringResource(R.string.rates_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }

            is WidgetState.Ready, is WidgetState.Stale -> {
                val data = if (state is WidgetState.Ready) state.data else (state as WidgetState.Stale).data
                val base = data.baseCurrency.lowercase()
                val isStale = state is WidgetState.Stale
                val updatedAt = if (state is WidgetState.Ready) state.updatedAt else (state as WidgetState.Stale).updatedAt

                Text(
                    text = if (isStale) {
                        stringResource(R.string.rates_source_saved, updatedAgoText(updatedAt))
                    } else {
                        stringResource(R.string.rates_source_live, updatedAgoText(updatedAt))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )

                // AIO's one-line strip: "1 usd = 4.08 myr   1 sgd = 3.17 myr". Each pair is held together
                // with no-break spaces, so a line only breaks between pairs. Tap to update.
                val strip = data.rates.take(maxPairs).map { item ->
                    val target = item.currency.lowercase()
                    val unit = if (target == "jpy" || target == "thb" || target == "idr") 100 else 1
                    val price = if (unit == 100) item.rateToBase * 100 else item.rateToBase
                    stringResource(R.string.rates_pair, unit, target, "%.2f".format(price), base)
                }.joinToString("   ")
                Text(
                    text = strip,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { onRefresh() }
                )
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
                        text = stringResource(R.string.tap_to_retry),
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )
                }
            }

            is WidgetState.Empty -> {
                Text(
                    text = stringResource(R.string.rates_empty),
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
    maxItems: Int,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.widget_name_spending),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (totalSpentToday > 0 && state is WidgetState.Ready) {
                Text(
                    text = stringResource(R.string.spending_today, "%.2f".format(totalSpentToday)),
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
                        text = stringResource(R.string.spending_turn_on),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextWhite
                    )
                    Text(
                        text = stringResource(R.string.spending_opt_in),
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
                        text = stringResource(R.string.spending_quiet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextWhite
                    )
                    Text(
                        text = stringResource(R.string.spending_quiet_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MeenaTextMuted
                    )
                }
            }
            else -> {
            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                transactions.take(maxItems).forEach { tx ->
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
                                text = stringResource(R.string.settings_status_pair, tx.bank.lowercase(), tx.formattedTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                        Text(
                            text = stringResource(R.string.spending_amount, "%.2f".format(tx.amount)),
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





/**
 * Markets: AIO's quotes chart and finance list in one. A year of gold, bitcoin, ether and silver on
 * one chart, then "name   price   change" per line, each name in its line's colour.
 * Daily reference prices from Frankfurter, said plainly so nobody takes them for live quotes.
 */
@Composable
private fun MarketsSection(
    state: WidgetState<MarketsData>,
    maxItems: Int,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        Text(
            text = stringResource(R.string.widget_name_markets),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        when (state) {
            is WidgetState.NeedsSetup, is WidgetState.Loading -> {
                Text(
                    text = stringResource(R.string.markets_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }

            is WidgetState.Ready, is WidgetState.Stale -> {
                val data = if (state is WidgetState.Ready) state.data else (state as WidgetState.Stale).data
                val updatedAt = if (state is WidgetState.Ready) state.updatedAt else (state as WidgetState.Stale).updatedAt
                val palette = my.hana.meena.theme.LocalMeenaPalette.current
                val colours = my.hana.meena.theme.chartSeriesColours(palette)
                val quotes = data.quotes.take(maxItems)
                val colourOf = { quote: MarketQuote -> colours[Markets.ASSETS.indexOf(quote.code).coerceIn(0, colours.lastIndex)] }
                val names = quotes.map { marketName(it.code) }

                my.hana.meena.ui.components.MarketChart(
                    series = quotes.map { my.hana.meena.ui.components.ChartSeries(it.history, colourOf(it)) },
                    zeroLineColor = MeenaTextMuted,
                    monochrome = palette.monochrome,
                    description = stringResource(R.string.markets_chart_description, names.joinToString(", ")),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
                )
                val firstDate = quotes.mapNotNull { it.history.firstOrNull()?.date }.minOrNull()
                val lastDate = quotes.maxOfOrNull { it.date }
                if (firstDate != null && lastDate != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Markets.monthLabel(firstDate), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                        Text(Markets.monthLabel(lastDate), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                    }
                }

                quotes.forEachIndexed { index, quote ->
                    val change = quote.dayChangePercent
                    val perOunce = quote.code == "XAU" || quote.code == "XAG"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = names[index],
                            style = MaterialTheme.typography.bodyLarge,
                            color = colourOf(quote),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (perOunce) stringResource(R.string.markets_price_ounce, Markets.formatPrice(quote.price))
                            else stringResource(R.string.finance_amount, Markets.formatPrice(quote.price)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MeenaTextWhite
                        )
                        Text(
                            text = if (change == null) stringResource(R.string.finance_no_price) else signedPercent(change),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                change == null -> MeenaTextMuted
                                change >= 0 -> MeenaProfitGreen
                                else -> MaterialTheme.colorScheme.error
                            },
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            modifier = Modifier.widthIn(min = 48.dp)
                        )
                    }
                }

                Text(
                    text = if (state is WidgetState.Stale) stringResource(R.string.markets_source_saved, updatedAgoText(updatedAt))
                    else stringResource(R.string.markets_source, updatedAgoText(updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
                Text(
                    text = stringResource(R.string.markets_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
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
                }
            }

            is WidgetState.Empty -> {
                Text(
                    text = stringResource(R.string.markets_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
        }
    }
}

/**
 * Kijang Emas: Bank Negara's gold coin, one line per size with what you pay and what you get back,
 * like a bank's price board. Published once each business day, so the date is always shown.
 */
@Composable
private fun KijangEmasSection(
    state: WidgetState<KijangEmasData>,
    maxItems: Int,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
        Text(
            text = stringResource(R.string.widget_name_kijang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        when (state) {
            is WidgetState.NeedsSetup, is WidgetState.Loading -> {
                Text(
                    text = stringResource(R.string.kijang_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }

            is WidgetState.Ready, is WidgetState.Stale -> {
                val data = if (state is WidgetState.Ready) state.data else (state as WidgetState.Stale).data
                val updatedAt = if (state is WidgetState.Ready) state.updatedAt else (state as WidgetState.Stale).updatedAt
                val change = data.changePercent
                val since = data.previousDate?.let { KijangEmas.dayLabel(it) }.orEmpty()
                if (change != null && since.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.kijang_change, signedPercent(change), since),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (change >= 0) MeenaProfitGreen else MaterialTheme.colorScheme.error
                    )
                }
                KijangEmasRow(
                    label = "",
                    pay = stringResource(R.string.kijang_you_pay),
                    get = stringResource(R.string.kijang_you_get),
                    isHeader = true
                )
                data.prices.take(maxItems).forEach { price ->
                    KijangEmasRow(
                        label = stringResource(
                            when (price.size) {
                                "one_oz" -> R.string.kijang_one_oz
                                "half_oz" -> R.string.kijang_half_oz
                                else -> R.string.kijang_quarter_oz
                            }
                        ),
                        pay = stringResource(R.string.finance_amount, Markets.formatPrice(price.youPay)),
                        get = stringResource(R.string.finance_amount, Markets.formatPrice(price.youGet)),
                        isHeader = false
                    )
                }
                val day = KijangEmas.dayLabel(data.date)
                Text(
                    text = if (state is WidgetState.Stale) stringResource(R.string.kijang_source_saved, updatedAgoText(updatedAt), day)
                    else stringResource(R.string.kijang_source, updatedAgoText(updatedAt), day),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
                Text(
                    text = stringResource(R.string.kijang_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
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
                }
            }

            is WidgetState.Empty -> {
                Text(
                    text = stringResource(R.string.markets_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted
                )
            }
        }
    }
}

@Composable
private fun KijangEmasRow(label: String, pay: String, get: String, isHeader: Boolean) {
    val style = if (isHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium
    val colour = if (isHeader) MeenaTextMuted else MeenaTextWhite
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MeenaTextWhite,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = pay,
            style = style,
            color = colour,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.widthIn(min = 96.dp)
        )
        Text(
            text = get,
            style = style,
            color = colour,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.widthIn(min = 96.dp)
        )
    }
}

@Composable
private fun marketName(code: String): String = stringResource(
    when (code) {
        "XAU" -> R.string.markets_gold
        "BTC" -> R.string.markets_bitcoin
        "ETH" -> R.string.markets_ether
        else -> R.string.markets_silver
    }
)

/** "+rm 1.20" or "-rm 1.20". */
@androidx.compose.runtime.Composable
private fun signedRm(value: Double): String =
    stringResource(R.string.finance_signed_amount, if (value < 0) "-" else "+", "%.2f".format(kotlin.math.abs(value)))

/** "+1.5%" or "-1.5%". */
@androidx.compose.runtime.Composable
private fun signedPercent(value: Double): String =
    stringResource(R.string.finance_signed_percent, "%+.1f".format(value))
