package com.example.meenalauncher.data.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

object SpendingRepository {
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    private val _transactionsFlow = MutableStateFlow<List<SpendingTransaction>>(emptyList())
    val transactionsFlow: StateFlow<List<SpendingTransaction>> = _transactionsFlow.asStateFlow()

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
        val current = _transactionsFlow.value.toMutableList()
        current.add(0, newTx)
        _transactionsFlow.value = current
    }

    fun parseAndRecordFromNotification(packageName: String, title: String, text: String): Boolean {
        // Regex to extract currency amounts: RM 45.00, RM45, MYR 50.00, $25.00
        val regex = Regex("""(?:RM|MYR|\$)\s*([0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val match = regex.find("$title $text") ?: return false
        val amount = match.groupValues[1].toDoubleOrNull() ?: return false

        // Detect Bank / Service
        val (bank, bankCode, channel) = when {
            packageName.contains("maybank", ignoreCase = true) || title.contains("MAE", true) || text.contains("Maybank", true) ->
                Triple("Maybank MAE", "M", "Maybank QR / DuitNow")
            packageName.contains("tng", ignoreCase = true) || text.contains("Touch 'n Go", true) || text.contains("TNG", true) ->
                Triple("TNG eWallet", "T", "TNG eWallet NFC")
            packageName.contains("cimb", ignoreCase = true) || text.contains("CIMB", true) ->
                Triple("CIMB Octo", "C", "CIMB Mastercard")
            packageName.contains("grab", ignoreCase = true) || text.contains("GrabPay", true) ->
                Triple("GrabPay", "G", "GrabPay Wallet")
            packageName.contains("wallet", ignoreCase = true) ->
                Triple("Google Wallet", "W", "NFC Payment")
            packageName.contains("messaging", ignoreCase = true) || packageName.contains("mms", ignoreCase = true) ->
                Triple("Bank SMS Alert", "S", "SMS Debit Notification")
            else ->
                Triple("Bank Alert", "B", "Debit Notification")
        }

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
