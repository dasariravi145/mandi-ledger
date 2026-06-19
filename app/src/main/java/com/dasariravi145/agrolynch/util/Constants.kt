package com.dasariravi145.agrolynch.util

object Constants {
    val DEFAULT_DEDUCTION_TYPES = listOf(
        "CAT",
        "Paper",
        "Advance",
        "Gate",
        "Cooli",
        "Packing",
        "Loading",
        "Unloading",
        "Market Fee",
        "Other"
    )

    object SeriesType {
        const val STOCK = "STOCK"
        const val SALE = "SALE"
        const val PAYMENT = "PAYMENT"
        const val LEDGER = "LEDGER"
    }

    object EntryType {
        const val STOCK = "STOCK"
        const val SALE = "SALE"
        const val PAYMENT = "PAYMENT"
    }
}
