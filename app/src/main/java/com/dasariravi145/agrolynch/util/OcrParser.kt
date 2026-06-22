package com.dasariravi145.agrolynch.util

import com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity
import java.text.SimpleDateFormat
import java.util.*

data class ExtractedItem(
    val product: String = "",
    val grade: String = "",
    val weight: Double = 0.0,
    val rate: Double = 0.0,
    val amount: Double = 0.0,
    val unit: String = "KG"
)

data class ExtractedData(
    // Business Details
    val businessName: String = "",
    val proprietor: String = "",
    val businessType: String = "",
    val location: String = "",
    val businessMobile: String = "",

    // Bill Details
    val billNumber: String = "",
    val date: Long = System.currentTimeMillis(),

    // Customer/Farmer Details
    val farmerName: String = "",
    val farmerVillage: String = "",
    val farmerPhone: String = "",

    // Items (Support for multiple)
    val items: List<ExtractedItem> = emptyList(),

    // Deductions
    val deductions: List<EntryDeductionEntity> = emptyList(),
    
    // Summary
    val grossAmount: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val netAmount: Double = 0.0,

    // Metadata
    val confidenceScore: Float = 0.0f,
    val lowConfidenceFields: Set<String> = emptySet(),
    val ocrText: String = "",
    
    // Legacy support (to avoid breaking current viewmodels immediately)
    val productName: String = items.firstOrNull()?.product ?: "",
    val quantity: Double = items.sumOf { it.weight },
    val rate: Double = items.firstOrNull()?.rate ?: 0.0,
    val amount: Double = items.sumOf { it.amount },
    val unit: String = items.firstOrNull()?.unit ?: "KG",

    // Additional fields for navigation compatibility
    val buyerName: String = "",
    val partyName: String = "",
    val category: String = "",
    val grade: String = items.firstOrNull()?.grade ?: "",
    val paymentMode: String = "",
    val numberOfBoxes: Int = 0,
    val totalWeightTon: Double = 0.0,
    val emptyBoxWeightPerBox: Double = 0.0,
    val spoilagePercentage: Double = 0.0,
    
    // AI/Gemini specific fields
    val damageOrSoot: Double = 0.0,
    val referenceNumber: String = "",
    val detectedNumbers: List<Double> = emptyList(),
    val detectedStrings: List<String> = emptyList()
)

object OcrParser {
    fun parse(text: String, target: String): ExtractedData {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        var businessName = ""
        var proprietor = ""
        var businessType = ""
        var location = ""
        var businessMobile = ""
        var billNumber = ""
        var date = System.currentTimeMillis()
        var farmerName = ""
        var farmerVillage = ""
        
        val items = mutableListOf<ExtractedItem>()
        val extraDeductions = mutableListOf<EntryDeductionEntity>()

        // 1. Detect Business Details (usually top of bill)
        val mobileRegex = Regex("(?i)(?:mob|cell|phone|contact)\\s*[:=-]?\\s*([6-9]\\d{9}(?:\\s*/\\s*[6-9]\\d{9})?)")
        
        lines.take(10).forEach { line ->
            if (businessName.isEmpty() && (line.contains("Fruits", true) || line.contains("Mandi", true) || line.contains("Ledger", true))) {
                businessName = line
            }
            if (proprietor.isEmpty() && line.contains("Prop", true)) {
                proprietor = line.replace(Regex("(?i)Prop:?"), "").trim()
            }
            if (businessMobile.isEmpty()) mobileRegex.find(line)?.let { businessMobile = it.groupValues[1] }
        }

        // 2. Detect Bill Details
        val billNoRegex = Regex("(?i)(?:bill|inv|receipt|no|#|ref|sl|stk|fta)\\s*[:=-]?\\s*([A-Za-z0-9-]+)")
        val dateRegex = Regex("(?i)(?:date|dt|on)\\s*[:=-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")

        // 3. Detect Customer/Farmer Details
        val nameRegex = Regex("(?i)(?:name|farmer|buyer|m/s|to)\\s*[:=-]?\\s*([A-Za-z\\s]{3,})")
        val placeRegex = Regex("(?i)(?:place|at|village|address)\\s*[:=-]?\\s*([A-Za-z\\s]{3,})")

        // 4. Detect Item Rows (Table Parsing Logic)
        // Look for lines that contain product keywords or match weight/rate/amount patterns
        val itemRegex = Regex("(\\D+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)") // Basic pattern: Product Weight Rate Amount

        lines.forEach { line ->
            if (billNumber.isEmpty()) billNoRegex.find(line)?.let { billNumber = it.groupValues[1] }
            if (dateRegex.containsMatchIn(line)) dateRegex.find(line)?.let { date = parseDate(it.groupValues[1]) }
            if (farmerName.isEmpty()) nameRegex.find(line)?.let { farmerName = it.groupValues[1].trim() }
            if (farmerVillage.isEmpty()) placeRegex.find(line)?.let { farmerVillage = it.groupValues[1].trim() }

            // Item detection
            itemRegex.find(line)?.let { match ->
                val prod = match.groupValues[1].trim()
                val weight = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val rate = match.groupValues[3].toDoubleOrNull() ?: 0.0
                val amt = match.groupValues[4].toDoubleOrNull() ?: 0.0
                if (weight > 0 && rate > 0) {
                    items.add(ExtractedItem(product = prod, weight = weight, rate = rate, amount = amt))
                }
            }
        }

        // 5. Detect Deductions
        val coolieRegex = Regex("(?i)(?:coolie|labor|hamali)\\s*[:=-]?\\s*(\\d+)")
        val paperRegex = Regex("(?i)(?:paper|gate|stamp|others)\\s*[:=-]?\\s*(\\d+)")
        val transportRegex = Regex("(?i)(?:transport|freight|vandi)\\s*[:=-]?\\s*(\\d+)")
        val advanceRegex = Regex("(?i)(?:advance|adv)\\s*[:=-]?\\s*(\\d+)")

        lines.forEach { line ->
            coolieRegex.find(line)?.let { extraDeductions.add(EntryDeductionEntity(entryId = "", entryType = "STOCK", billId = billNumber, deductionType = "Labor", amount = it.groupValues[1].toDoubleOrNull() ?: 0.0)) }
            paperRegex.find(line)?.let { extraDeductions.add(EntryDeductionEntity(entryId = "", entryType = "STOCK", billId = billNumber, deductionType = "Other", amount = it.groupValues[1].toDoubleOrNull() ?: 0.0)) }
            transportRegex.find(line)?.let { extraDeductions.add(EntryDeductionEntity(entryId = "", entryType = "STOCK", billId = billNumber, deductionType = "Transport", amount = it.groupValues[1].toDoubleOrNull() ?: 0.0)) }
            advanceRegex.find(line)?.let { extraDeductions.add(EntryDeductionEntity(entryId = "", entryType = "STOCK", billId = billNumber, deductionType = "Advance", amount = it.groupValues[1].toDoubleOrNull() ?: 0.0)) }
        }

        val gross = items.sumOf { it.amount }
        val totalDed = extraDeductions.sumOf { it.amount }
        val net = gross - totalDed

        return ExtractedData(
            businessName = businessName,
            proprietor = proprietor,
            businessMobile = businessMobile,
            billNumber = billNumber,
            date = date,
            farmerName = farmerName,
            farmerVillage = farmerVillage,
            items = items,
            deductions = extraDeductions,
            grossAmount = gross,
            totalDeductions = totalDed,
            netAmount = net,
            ocrText = text,
            confidenceScore = calculateConfidence(text)
        )
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

    private fun calculateConfidence(text: String): Float {
        if (text.isEmpty()) return 0f
        val keywords = listOf("bill", "date", "amount", "total", "no", "name", "rate", "weight")
        val found = keywords.count { text.contains(it, ignoreCase = true) }
        return (found.toFloat() / keywords.size) * 100f
    }
}
