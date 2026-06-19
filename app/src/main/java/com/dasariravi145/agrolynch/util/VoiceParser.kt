package com.dasariravi145.agrolynch.util

import timber.log.Timber

object VoiceParser {

    data class VoiceEntry(
        val name: String = "",
        val product: String = "",
        val grade: String = "",
        val quantity: Double = 0.0,
        val unit: String = "KG",
        val rate: Double = 0.0,
        val spoilage: Double = 0.0,
        val totalWeightTon: Double = 0.0,
        val emptyBoxWeight: Double = 0.0,
        val commission: Double = 5.0,
        val deductions: List<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity> = emptyList()
    )

    fun parseInput(text: String): VoiceEntry {
        var name = ""
        var product = ""
        var grade = ""
        var quantity = 0.0
        var unit = "KG"
        var rate = 0.0
        var spoilage = 0.0
        var totalWeightTon = 0.0
        var emptyBoxWeight = 0.0
        var commission = 5.0
        val extraDeductions = mutableListOf<com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity>()

        val lowerText = text.lowercase()

        // 1. Farmer Name
        val nameMatch = Regex("(?i)(?:farmer\\s+)?([A-Za-z]+)(?:\\s+([A-Za-z]+))?\\s+(?:mango|tomato|potato|onion|apple|banana|grape|pomegranate|papaya|guava|grade|\\d+)").find(text)
        name = nameMatch?.groupValues?.get(1)?.trim() ?: ""
        if (nameMatch?.groupValues?.get(2)?.isNotEmpty() == true) {
            name += " " + nameMatch.groupValues[2]
        }
        
        // 2. Product & Grade
        val productKeywords = listOf("mango", "tomato", "potato", "onion", "apple", "banana", "grape", "pomegranate", "papaya", "guava")
        productKeywords.forEach { kw ->
            if (lowerText.contains(kw)) {
                product = kw.replaceFirstChar { it.uppercase() }
            }
        }
        
        val gradeMatch = Regex("(?i)grade\\s+([a-z0-9]+)").find(lowerText)
        grade = gradeMatch?.groupValues?.get(1)?.uppercase()?.let { "Grade $it" } ?: ""

        // 3. Quantity & Unit
        val qtyMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(ton|tons|kg|kgs|boxes|bags)").find(lowerText)
        if (qtyMatch != null) {
            quantity = qtyMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            unit = when (qtyMatch.groupValues[2]) {
                "ton", "tons" -> "Ton"
                "boxes", "bags" -> "Boxes"
                else -> "KG"
            }
        }
        
        // Special case for Boxes mode: "500 boxes 5 ton 1.2 kg empty box"
        if (unit == "Boxes") {
            val weightMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(ton|tons)").find(lowerText)
            if (weightMatch != null) {
                totalWeightTon = weightMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            }
            val emptyBoxMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:kg|kgs)?\\s*(?:empty|box weight)").find(lowerText)
            if (emptyBoxMatch != null) {
                emptyBoxWeight = emptyBoxMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            }
        }

        // 4. Rate
        val rateMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:rupees|rs|per|/)").find(lowerText)
        rate = rateMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        // 5. Spoilage / Waste
        val spoilageMatch = Regex("(?i)(?:spoilage|waste|wastage|less|spoil)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:kg|percent|%|per ton)?").find(lowerText)
        spoilage = spoilageMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        // 6. Commission
        val commMatch = Regex("(?i)commission\\s*(\\d+(?:\\.\\d+)?)\\s*(?:percent|%)").find(lowerText)
        commission = commMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 5.0

        // 7. Deductions
        val deductionRegex = Regex("(?i)(cat|paper|advance|gate|cooli|packing|loading)\\s*(\\d+(?:\\.\\d+)?)(?:,|$|\\s)")
        deductionRegex.findAll(lowerText).forEach { match ->
            val type = match.groupValues[1].replaceFirstChar { it.uppercase() }
            val amount = match.groupValues[2].toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                extraDeductions.add(com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity(
                    entryId = "", entryType = "STOCK", billId = "", deductionType = type, amount = amount
                ))
            }
        }

        Timber.d("VoiceParser: Result -> Name: $name, Prod: $product, Qty: $quantity $unit, Rate: $rate, Spoil: $spoilage, Comm: $commission")

        return VoiceEntry(name, product, grade, quantity, unit, rate, spoilage, totalWeightTon, emptyBoxWeight, commission, extraDeductions)
    }
}
