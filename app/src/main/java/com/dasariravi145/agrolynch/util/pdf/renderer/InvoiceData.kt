package com.dasariravi145.agrolynch.util.pdf.renderer

data class InvoiceProduct(
    val name: String,
    val productType: String = "",
    val grade: String,
    val unit: String = "KG",
    val quantity: Double,
    val rate: Double,
    val amount: Double
)

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
    val grandTotal: Double,
    val vehicleNumber: String = "",
    val customerGstin: String? = null,
    val customerState: String? = null,
    val placeOfSupply: String? = null,
    val reverseCharge: Boolean = false,
    val paymentMode: String? = null
)
