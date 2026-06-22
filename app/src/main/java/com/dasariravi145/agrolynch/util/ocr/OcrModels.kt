package com.dasariravi145.agrolynch.util.ocr

data class ExtractedBillItem(
    val productName: String = "",
    val grade: String? = null,
    val quantityKg: Double = 0.0,
    val rate: Double = 0.0,
    val amount: Double = 0.0,
    val warning: String? = null,
    val unit: String = "KG",
    val spoilage: Double = 0.0
)

data class ExtractedDeduction(
    val type: String = "",
    val amount: Double = 0.0,
    val customName: String = ""
)

data class OcrConfidence(
    val score: Float = 0f,
    val lowConfidenceFields: Set<String> = emptySet()
)

data class ExtractedBillData(
    val businessName: String = "",
    val proprietorName: String = "",
    val businessType: String = "",
    val location: String = "",
    val mobileNumbers: String = "",
    val originalBillRefNo: String? = null,
    val date: Long = System.currentTimeMillis(),
    val farmerName: String = "",
    val farmerPlace: String = "",
    val category: String = "Fruit",
    
    val items: List<ExtractedBillItem> = emptyList(),
    
    // Deductions
    val commission: Double = 0.0,
    val labour: Double = 0.0,
    val transport: Double = 0.0,
    val gateOrPaper: Double = 0.0,
    val advance: Double = 0.0,
    val others: Double = 0.0,
    
    val grossAmount: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val netAmount: Double = 0.0,
    
    val rawText: String = "",
    val confidence: OcrConfidence = OcrConfidence()
)
