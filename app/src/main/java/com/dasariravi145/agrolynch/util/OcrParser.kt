package com.dasariravi145.agrolynch.util

import java.text.SimpleDateFormat
import java.util.*

data class ExtractedData(
    val billNumber: String = "",
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val ocrText: String = ""
)

object OcrParser {
    fun parse(text: String): ExtractedData {
        val lines = text.split("\n")
        var amount = 0.0
        var date = System.currentTimeMillis()
        var billNumber = ""

        // Specific patterns for market bills
        val amountRegex = Regex("(?i)(?:total|amount|net|paid|cash|rs|₹|inr)\\s*[:=-]?\\s*(\\d+(?:[,.]\\d{1,2})?)")
        val dateRegex = Regex("(?i)(?:date|dt|on)\\s*[:=-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")
        val billNoRegex = Regex("(?i)(?:bill|inv|receipt|no|#|ref|sl)\\s*[:=-]?\\s*([A-Za-z0-9-]+)")

        for (line in lines) {
            // Amount Extraction (usually larger numbers or after labels)
            if (amount == 0.0) {
                amountRegex.find(line)?.let { 
                    amount = it.groupValues.last().replace(",", "").toDoubleOrNull() ?: 0.0 
                }
            }
            
            // Date Extraction
            if (dateRegex.containsMatchIn(line)) {
                dateRegex.find(line)?.let { 
                    val dateStr = it.groupValues.last()
                    date = parseDate(dateStr)
                }
            }

            // Bill Number Extraction
            if (billNumber.isEmpty()) {
                billNoRegex.find(line)?.let { 
                    billNumber = it.groupValues.last() 
                }
            }
        }

        return ExtractedData(
            billNumber = billNumber,
            amount = amount,
            date = date,
            ocrText = text
        )
    }

    private fun parseDate(dateStr: String): Long {
        val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy", "MM/dd/yyyy", "yyyy-MM-dd")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
