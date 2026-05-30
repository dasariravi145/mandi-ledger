package com.dasariravi145.agrolynch.domain.model

data class ReceiptData(
    val receiptId: String,
    val date: Long,
    val agentName: String = "AgroLynch Mandi Agent",
    val agentContact: String = "+91 9876543210",
    val partyName: String,
    val partyType: String, // Farmer / Buyer
    val transactionType: String, // Sale / Payment / Purchase
    val items: List<ReceiptItem> = emptyList(),
    val totalAmount: Double,
    val notes: String = ""
)

data class ReceiptItem(
    val description: String,
    val quantity: String,
    val rate: String,
    val amount: String
)
