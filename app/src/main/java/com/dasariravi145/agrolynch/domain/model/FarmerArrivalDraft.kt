package com.dasariravi145.agrolynch.domain.model

data class FarmerArrivalDraft(
    val farmerName: String = "",
    val phone: String = "",
    val village: String = "",
    val productName: String = "",
    val category: String = "General",
    val grade: String = "",
    val unitType: String = "KG",
    val quantity: Double = 0.0,
    val waste: Double = 0.0,
    val rate: Double = 0.0,
    val commissionPercent: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val otherDeductions: Double = 0.0,
    val billNumber: String = "",
    val date: Long = System.currentTimeMillis()
)
