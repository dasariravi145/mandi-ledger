package com.dasariravi145.agrolynch.domain.model

data class LedgerEntry(
    val id: String,
    val title: String,
    val amount: Double,
    val type: LedgerType,
    val transactionType: TransactionType,
    val date: Long,
    val balance: Double = 0.0,
    val reference: String = "",
    val status: LedgerStatus = LedgerStatus.PENDING,
    val details: LedgerEntryDetails? = null
)

data class LedgerEntryDetails(
    val billNumber: String = "",
    val productName: String = "",
    val category: String = "",
    val grade: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val rate: Double = 0.0,
    val grossAmount: Double = 0.0,
    val commissionPercent: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val otherDeductions: Double = 0.0,
    val netAmount: Double = 0.0,
    val paymentMade: Double = 0.0
)

enum class LedgerType {
    DEBIT, CREDIT
}

enum class TransactionType {
    ARRIVAL, SALE, PAYMENT, ADJUSTMENT
}

enum class LedgerStatus {
    PAID, PARTIAL, PENDING, OVERDUE, ADVANCE
}

data class LedgerSummary(
    val partyId: String,
    val partyName: String,
    val totalDebit: Double,
    val totalCredit: Double,
    val balance: Double,
    val advanceAmount: Double = 0.0,
    val totalTransactions: Int = 0,
    val lastTransactionDate: Long = 0,
    val entries: List<LedgerEntry> = emptyList()
)
