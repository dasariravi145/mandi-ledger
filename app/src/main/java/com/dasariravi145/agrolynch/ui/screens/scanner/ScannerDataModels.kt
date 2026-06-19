package com.dasariravi145.agrolynch.ui.screens.scanner

data class DetectedProduct(
    val gradeName: String,
    val grossQty: Double,
    val damageQty: Double,
    val netQty: Double,
    val rate: Double,
    val amount: Double,
    val confidence: Int = 100
)

data class ScannedBillResult(
    val billNumber: String = "",
    val date: String = "",
    val farmerName: String = "",
    val phoneNumber: String = "",
    val village: String = "",
    val products: List<DetectedProduct> = emptyList(),
    val commission: Double = 0.0,
    val transport: Double = 0.0,
    val cooli: Double = 0.0,
    val gate: Double = 0.0,
    val paper: Double = 0.0,
    val advance: Double = 0.0,
    val grandTotal: Double = 0.0,
    val confidenceScores: Map<String, Int> = emptyMap()
)

object GradeDictionary {
    private val grades = listOf(
        "Sindhura", "Totapuri", "Banganapalli", "Rasalu", 
        "Kesar", "Alphonso", "Mallika", "Himayat", "Neelam"
    )

    fun normalize(input: String): String {
        val trimmed = input.trim()
        return grades.find { it.equals(trimmed, ignoreCase = true) }
            ?: grades.find { it.contains(trimmed, ignoreCase = true) || trimmed.contains(it, ignoreCase = true) }
            ?: trimmed
    }
}
