package my.hana.meena.data.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SpendingTransaction(
    val id: String,
    val bank: String,
    val bankCode: String,
    val merchant: String,
    val channel: String,
    val amount: Double,
    val timestamp: Long,
    val formattedTime: String
)

object SpendingRepository : WidgetSpec<List<SpendingTransaction>> {
    override val id = "widget-spending-summary"
    override val title = "spending"
    override val source = "Bank Notifications"
    override val refreshPolicy = RefreshPolicy.MANUAL
    override val defaultRefreshMinutes = 0

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    // Allowed banking & payment apps (strictly allowlisted, no arbitrary notification parsing)
    val BANK_ALLOWLIST = mapOf(
        "com.maybank2u.life" to Triple("Maybank MAE", "M", "DuitNow / QR"),
        "com.v2u" to Triple("Maybank2u", "M", "Maybank Debit"),
        "my.com.tngdigital.ewallet" to Triple("TNG eWallet", "T", "TNG eWallet"),
        "com.cimb.octo" to Triple("CIMB Octo", "C", "CIMB Mastercard"),
        "com.cimb.cimbclicks" to Triple("CIMB Clicks", "C", "CIMB Clicks"),
        "com.publicbank.pbe" to Triple("Public Bank", "P", "PB Online"),
        "com.publicbank.myentp" to Triple("PB engage", "P", "PB engage"),
        "com.rhb.mobile" to Triple("RHB Mobile", "R", "RHB Pay"),
        "com.bankislam.mobile" to Triple("Bank Islam", "B", "Bank Islam"),
        "com.ambank.ambankmobile" to Triple("AmOnline", "A", "AmOnline"),
        "com.ocbc.mobile" to Triple("OCBC Malaysia", "O", "OCBC Pay"),
        "com.uob.mighty.my" to Triple("UOB TMRW", "U", "UOB TMRW"),
        "com.grabtaxi.passenger" to Triple("GrabPay", "G", "GrabPay"),
        "com.google.android.apps.walletnfcrel" to Triple("Google Wallet", "W", "Google Pay")
    )

    var isOptedIn: Boolean = false
        set(value) {
            field = value
            if (!value) {
                _transactionsFlow.value = WidgetState.NeedsSetup
            } else if (_transactionsFlow.value is WidgetState.NeedsSetup) {
                _transactionsFlow.value = WidgetState.Empty
            }
        }

    private val _transactionsFlow = MutableStateFlow<WidgetState<List<SpendingTransaction>>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<List<SpendingTransaction>>> = _transactionsFlow.asStateFlow()

    override fun fetchInitialState(): WidgetState<List<SpendingTransaction>> = _transactionsFlow.value

    override suspend fun refresh(): WidgetState<List<SpendingTransaction>> {
        return _transactionsFlow.value
    }

    fun addTransaction(
        bank: String,
        bankCode: String,
        merchant: String,
        channel: String,
        amount: Double
    ) {
        val newTx = SpendingTransaction(
            id = "tx-${System.currentTimeMillis()}",
            bank = bank,
            bankCode = bankCode,
            merchant = merchant,
            channel = channel,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            formattedTime = timeFormat.format(Date())
        )
        val currentData = (_transactionsFlow.value as? WidgetState.Ready)?.data ?: emptyList()
        val current = currentData.toMutableList()
        current.add(0, newTx)
        _transactionsFlow.value = WidgetState.Ready(current, System.currentTimeMillis())
    }

    fun parseAndRecordFromNotification(packageName: String, title: String, text: String): Boolean {
        if (!isOptedIn) return false

        // Match against known bank allowlist only
        val matchedBank = BANK_ALLOWLIST.entries.firstOrNull { (pkg, _) ->
            packageName.equals(pkg, ignoreCase = true) || packageName.contains(pkg, ignoreCase = true)
        }?.value ?: return false

        // Regex to extract currency amounts: RM 45.00, RM45, MYR 50.00, $25.00
        val regex = Regex("""(?:RM|MYR|\$)\s*([0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val match = regex.find("$title $text") ?: return false
        val amount = match.groupValues[1].toDoubleOrNull() ?: return false

        val (bank, bankCode, channel) = matchedBank

        // Detect Merchant
        var merchant = "Card Transaction"
        val atIndex = text.indexOf("at ", ignoreCase = true)
        val toIndex = text.indexOf("to ", ignoreCase = true)
        if (atIndex != -1) {
            val candidate = text.substring(atIndex + 3).split(Regex("[.,\n]")).firstOrNull()?.trim()
            if (!candidate.isNullOrBlank()) merchant = candidate.take(32)
        } else if (toIndex != -1) {
            val candidate = text.substring(toIndex + 3).split(Regex("[.,\n]")).firstOrNull()?.trim()
            if (!candidate.isNullOrBlank()) merchant = candidate.take(32)
        }

        addTransaction(
            bank = bank,
            bankCode = bankCode,
            merchant = merchant,
            channel = channel,
            amount = amount
        )
        return true
    }
}

