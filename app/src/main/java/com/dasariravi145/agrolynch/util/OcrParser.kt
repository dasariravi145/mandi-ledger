package com.dasariravi145.agrolynch.util

import java.text.SimpleDateFormat
import java.util.*

data class ExtractedData(
    val billNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val farmerName: String = "",
    val buyerName: String = "",
    val partyName: String = "", // Used for Payment/General
    val productName: String = "",
    val category: String = "General",
    val grade: String = "",
    val quantity: Double = 0.0,
    val damageOrSoot: Double = 0.0,
    val netQuantity: Double = 0.0,
    val rate: Double = 0.0, // Purchase Rate or Sale Rate
    val grossAmount: Double = 0.0,
    val amount: Double = 0.0, // Sale Amount or Payment Amount
    val commission: Double = 0.0,
    val transport: Double = 0.0,
    val labor: Double = 0.0,
    val cooli: Double = 0.0,
    val gate: Double = 0.0,
    val advance: Double = 0.0,
    val netAmount: Double = 0.0,
    val paymentMode: String = "",
    val referenceNumber: String = "",
    val remarks: String = "",
    val bankName: String = "",
    val chequeNumber: String = "",
    val chequeDate: String = "",
    val accountHolderName: String = "",
    val confidenceScore: Float = 0.0f,
    val lowConfidenceFields: Set<String> = emptySet(),
    val detectedNumbers: List<String> = emptyList(),
    val ocrText: String = ""
)

object OcrParser {
    fun parse(text: String, target: String): ExtractedData {
        val lines = text.split("\n")
        val lowConfidenceFields = mutableSetOf<String>()
        
        // Basic Patterns (Keep as fallback)
        val amountRegex = Regex("(?i)(?:total|amount|net|paid|cash|rs|₹|inr)\\s*[:=-]?\\s*(\\d+(?:[,.]\\d{1,2})?)")
        val dateRegex = Regex("(?i)(?:date|dt|on)\\s*[:=-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")
        val billNoRegex = Regex("(?i)(?:bill|inv|receipt|no|#|ref|sl)\\s*[:=-]?\\s*([A-Za-z0-9-]+)")
        
        val nameRegex = Regex("(?i)(?:name|farmer|buyer|m/s|to)\\s*[:=-]?\\s*([A-Za-z\\s]{3,})")
        val productRegex = Regex("(?i)(?:product|item|commodity)\\s*[:=-]?\\s*([A-Za-z\\s]+)")
        val qtyRegex = Regex("(?i)(?:qty|quantity|weight|kgs|bags)\\s*[:=-]?\\s*(\\d+(?:\\.\\d{1,2})?)")
        val rateRegex = Regex("(?i)(?:rate|price)\\s*[:=-]?\\s*(\\d+(?:\\.\\d{1,2})?)")
        
        val modeRegex = Regex("(?i)(?:mode|type|pay)\\s*[:=-]?\\s*(cash|cheque|upi|online|bank)")
        val chequeRegex = Regex("(?i)(?:cheque|chq|ref)\\s*[:=-]?\\s*(\\d{6,})")

        var billNumber = ""
        var amount = 0.0
        var date = System.currentTimeMillis()
        var partyName = ""
        var product = ""
        var qty = 0.0
        var rate = 0.0
        var mode = ""
        var chq = ""

        lines.forEach { line ->
            if (billNumber.isEmpty()) billNoRegex.find(line)?.let { billNumber = it.groupValues[1] }
            if (amount == 0.0) amountRegex.find(line)?.let { amount = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0 }
            if (dateRegex.containsMatchIn(line)) dateRegex.find(line)?.let { date = parseDate(it.groupValues[1]) }
            
            if (partyName.isEmpty()) nameRegex.find(line)?.let { partyName = it.groupValues[1].trim() }
            if (product.isEmpty()) productRegex.find(line)?.let { product = it.groupValues[1].trim() }
            if (qty == 0.0) qtyRegex.find(line)?.let { qty = it.groupValues[1].toDoubleOrNull() ?: 0.0 }
            if (rate == 0.0) rateRegex.find(line)?.let { rate = it.groupValues[1].toDoubleOrNull() ?: 0.0 }
            
            if (mode.isEmpty()) modeRegex.find(line)?.let { mode = it.groupValues[1].uppercase() }
            if (chq.isEmpty()) chequeRegex.find(line)?.let { chq = it.groupValues[1] }
        }

        // Validate and mark low confidence
        if (amount == 0.0) lowConfidenceFields.add("amount")
        if (partyName.isEmpty()) lowConfidenceFields.add("partyName")
        if (billNumber.isEmpty()) lowConfidenceFields.add("billNumber")

        return ExtractedData(
            billNumber = billNumber,
            amount = amount,
            date = date,
            partyName = partyName,
            farmerName = if (target == "STOCK_ENTRY") partyName else "",
            buyerName = if (target == "SALE_ENTRY") partyName else "",
            productName = product,
            quantity = qty,
            rate = rate,
            paymentMode = mode,
            chequeNumber = chq,
            confidenceScore = calculateConfidence(text),
            lowConfidenceFields = lowConfidenceFields,
            detectedNumbers = extractNumbersAndDates(text),
            ocrText = text
        )
    }

    private fun extractNumbersAndDates(text: String): List<String> {
        val results = mutableListOf<String>()
        // Dates
        val dateRegex = Regex("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}")
        results.addAll(dateRegex.findAll(text).map { it.value })
        // Numbers
        val numberRegex = Regex("\\b\\d+(?:\\.\\d+)?\\b")
        results.addAll(numberRegex.findAll(text).map { it.value }.filter { it !in results })
        return results.distinct()
    }

    private fun calculateConfidence(text: String): Float {
        if (text.isEmpty()) return 0f
        val keywords = listOf("bill", "date", "amount", "total", "no", "name")
        val found = keywords.count { text.contains(it, ignoreCase = true) }
        return (found.toFloat() / keywords.size) * 100f
    }

    private fun parseDate(dateStr: String): Long {
        val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy", "yyyy-MM-dd")
        formats.forEach { format ->
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(dateStr)?.time ?: return@forEach
            } catch (e: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
