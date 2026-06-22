package com.dasariravi145.agrolynch.ui.screens.arrival

import java.util.*

enum class VoiceEntryLanguage(val displayName: String, val locale: String) {
    AUTO("Auto Detect", "en-IN"),
    TELUGU("Telugu", "te-IN"),
    HINDI("Hindi", "hi-IN"),
    TAMIL("Tamil", "ta-IN"),
    KANNADA("Kannada", "kn-IN"),
    ENGLISH("English", "en-IN")
}

data class ParsedArrivalVoiceData(
    val heardText: String,
    val farmerName: String? = null,
    val product: String? = null,
    val category: String? = null,
    val grade: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val rate: Double? = null,
    val commission: Double? = null,
    val transport: Double? = null,
    val labor: Double? = null,
    val otherDeduction: Double? = null
)

object VoiceEntryParser {

    private val mangoKeywords = listOf("mango", "mamidi", "మామిడి", "aam", "आम", "maanga", "மாம்பழம்", "mavina hannu", "ಮಾವಿನ ಹಣ್ಣು")
    private val tonKeywords = listOf("ton", "tonne", "tons", "టన్ను", "టన్నులు", "टन", "டன்", "ಟನ್")
    private val kgKeywords = listOf("kg", "kilo", "kilos", "కిలో", "किलो", "கிலோ", "ಕೆಜಿ")
    private val rateKeywords = listOf("rate", "price", "రేట్", "ధర", "रेट", "भाव", "விலை", "ದರ")
    private val gradeKeywords = listOf("grade", "గ్రేడ్", "ग्रेड", "தரம்", "ಗ್ರೇಡ್")
    private val commissionKeywords = listOf("commission", "کమిషన్", "ಕಮಿಷನ್", "கமிஷன்", "ಕಮಿಷನ್")
    private val transportKeywords = listOf("transport", "ట్రాన్స్‌పోర్ట్", "किराया", "வண்டி செலவு", "ಸಾರಿಗೆ")
    private val laborKeywords = listOf("labour", "labor", "కూలి", "मजदूरी", "கூலி")
    private val deductionKeywords = listOf("cat", "cutting", "deduction", "కటింగ్", "कटौती", "கழிவு", "ಕಡಿತ")

    fun parse(text: String): ParsedArrivalVoiceData {
        val lower = text.lowercase(Locale.getDefault())
        val words = lower.split(Regex("\\s+"))

        var farmer: String? = null
        var product: String? = null
        var unit: String? = null
        var quantity: Double? = null
        var rate: Double? = null
        var grade: String? = null
        var commission: Double? = null
        var transport: Double? = null
        var labor: Double? = null
        var deduction: Double? = null

        // 1. Extract Product
        for (keyword in mangoKeywords) {
            if (lower.contains(keyword)) {
                product = "Mango"
                break
            }
        }

        // 2. Extract Unit and Quantity
        tonKeywords.forEach { k ->
            val match = Regex("(\\d+\\.?\\d*)\\s*$k").find(lower)
            if (match != null) {
                quantity = match.groupValues[1].toDoubleOrNull()
                unit = "Ton"
            }
        }
        if (unit == null) {
            kgKeywords.forEach { k ->
                val match = Regex("(\\d+\\.?\\d*)\\s*$k").find(lower)
                if (match != null) {
                    quantity = match.groupValues[1].toDoubleOrNull()
                    unit = "KG"
                }
            }
        }

        // 3. Extract Rate
        rateKeywords.forEach { k ->
            val match = Regex("$k\\s*(\\d+\\.?\\d*)").find(lower)
            if (match != null) {
                rate = match.groupValues[1].toDoubleOrNull()
            }
        }

        // 4. Extract Grade
        gradeKeywords.forEach { k ->
            val match = Regex("$k\\s*([a-z0-9]+)").find(lower)
            if (match != null) {
                grade = "Grade " + match.groupValues[1].uppercase()
            }
        }

        // 5. Extract Charges
        commissionKeywords.forEach { k ->
            val match = Regex("$k\\s*(\\d+\\.?\\d*)").find(lower)
            if (match != null) commission = match.groupValues[1].toDoubleOrNull()
        }
        transportKeywords.forEach { k ->
            val match = Regex("$k\\s*(\\d+\\.?\\d*)").find(lower)
            if (match != null) transport = match.groupValues[1].toDoubleOrNull()
        }
        laborKeywords.forEach { k ->
            val match = Regex("$k\\s*(\\d+\\.?\\d*)").find(lower)
            if (match != null) labor = match.groupValues[1].toDoubleOrNull()
        }
        deductionKeywords.forEach { k ->
            val match = Regex("$k\\s*(\\d+\\.?\\d*)").find(lower)
            if (match != null) deduction = match.groupValues[1].toDoubleOrNull()
        }

        // 6. Farmer Name (Fallback: assume first word if not a keyword/number)
        if (words.isNotEmpty()) {
            val first = words[0]
            if (first.toDoubleOrNull() == null && !mangoKeywords.contains(first) && !rateKeywords.contains(first)) {
                farmer = first.replaceFirstChar { it.uppercase() }
            }
        }

        return ParsedArrivalVoiceData(
            heardText = text,
            farmerName = farmer,
            product = product,
            unit = unit ?: "KG",
            quantity = quantity,
            rate = rate,
            grade = grade,
            commission = commission,
            transport = transport,
            labor = labor,
            otherDeduction = deduction
        )
    }
}
