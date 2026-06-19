package com.dasariravi145.agrolynch.util.pdf.renderer

data class InvoiceData(
    val billNumber: String,
    val date: Long,
    val customerName: String,
    val customerMobile: String,
    val products: List<InvoiceProduct>,
    val subtotal: Double,
    val commission: Double,
    val transport: Double,
    val labour: Double,
    val advance: Double,
    val others: Double,
    val grandTotal: Double
)

data class InvoiceProduct(
    val name: String,
    val grade: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double
)
