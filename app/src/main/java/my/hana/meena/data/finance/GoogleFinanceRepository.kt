package my.hana.meena.data.finance

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState

/**
 * Compatibility shim delegating to FinanceRepository.
 * Stripped of Yahoo scraping and fake default data per Meena UI Hard Rule 1 & Phase 6 compliance.
 */
object GoogleFinanceRepository : WidgetSpec<FinanceData> {
    override val id = "widget-finance-charts"
    override val title = "finance"
    override val source = "Google Sheets Bridge & Local Holdings"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 15

    val state: StateFlow<WidgetState<FinanceData>> = FinanceRepository.state

    override fun fetchInitialState(): WidgetState<FinanceData> = FinanceRepository.fetchInitialState()

    override suspend fun refresh(): WidgetState<FinanceData> = FinanceRepository.refresh()

    fun getTotalPortfolioValue(): Double = FinanceRepository.getTotalPortfolioValue()

    fun getTotalPortfolioChange(): Double = FinanceRepository.getTotalPortfolioChange()

    fun getTotalPortfolioChangePercent(): Double = FinanceRepository.getTotalPortfolioChangePercent()

    fun getCandles(symbol: String, timeframe: String): List<FinanceCandle> = emptyList()

    fun openGoogleFinance(context: Context, url: String = "https://www.google.com/finance") {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
