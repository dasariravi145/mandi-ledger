package com.dasariravi145.agrolynch.domain.model

data class FarmerArrivalDraft(
    val farmerName: String = "",
    val phone: String = "",
    val village: String = "",
    val address: String = "",
    val productName: String = "",
    val category: String = "Fruit",
    val grade: String = "Grade A",
    val unitType: String = "KG",
    val quantity: Double = 0.0,
    val waste: Double = 0.0,
    val rate: Double = 0.0,
    
    // Boxes specific
    val numBoxes: Int = 0,
    val totalWeightTon: Double = 0.0,
    val emptyWeightPerBox: Double = 0.0,
    val spoilagePercent: Double = 0.0,
    
    val commissionPercent: Double = 5.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val otherDeductions: Double = 0.0,
    val advance: Double = 0.0,
    val billNumber: String = "",
    val date: Long = System.currentTimeMillis()
)
