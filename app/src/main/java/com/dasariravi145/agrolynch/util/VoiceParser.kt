package com.dasariravi145.agrolynch.util

object VoiceParser {

    data class VoiceEntry(
        val name: String = "",
        val product: String = "",
        val quantity: Double = 0.0,
        val unit: String = ""
    )

    /**
     * Parses strings like "Ravi mango 50 boxes" or Telugu equivalent
     * Heuristic based parsing for mandi workflow
     */
    fun parseInput(text: String): VoiceEntry {
        val words = text.split(" ")
        var name = ""
        var product = ""
        var quantity = 0.0
        var unit = ""

        // Regex to find numbers
        val numberRegex = Regex("""\d+(\.\d+)?""")
        val match = numberRegex.find(text)
        
        if (match != null) {
            quantity = match.value.toDoubleOrNull() ?: 0.0
            val numberIndex = text.indexOf(match.value)
            
            // Text before number is likely name and product
            val beforeNumber = text.substring(0, numberIndex).trim().split(" ")
            if (beforeNumber.isNotEmpty()) {
                name = beforeNumber[0]
                if (beforeNumber.size > 1) {
                    product = beforeNumber.subList(1, beforeNumber.size).joinToString(" ")
                }
            }

            // Text after number is likely unit
            val afterNumber = text.substring(numberIndex + match.value.length).trim().split(" ")
            if (afterNumber.isNotEmpty()) {
                unit = afterNumber[0]
            }
        }

        return VoiceEntry(name, product, quantity, unit)
    }
}
