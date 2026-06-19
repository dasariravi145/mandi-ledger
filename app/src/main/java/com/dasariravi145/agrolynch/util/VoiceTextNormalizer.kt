package com.dasariravi145.agrolynch.util

import com.dasariravi145.agrolynch.ui.screens.voice.VoiceStep
import timber.log.Timber

object VoiceTextNormalizer {

    fun normalizeVoiceText(rawText: String, currentStep: VoiceStep): String {
        val lowerText = rawText.lowercase().trim()

        // 1. Handle "Skip" in all supported languages
        if (isSkipCommand(lowerText)) {
            Timber.d("VOICE_NORMALIZED_TEXT: SKIP (Step: $currentStep)")
            return "SKIP"
        }

        // 2. Step-specific normalization
        val result = when (currentStep) {
            VoiceStep.FARMER_NAME, VoiceStep.VILLAGE, VoiceStep.GRADE -> rawText.trim()
            VoiceStep.PRODUCT_NAME -> normalizeProductName(lowerText)
            VoiceStep.UNIT -> normalizeUnit(lowerText)
            VoiceStep.QUANTITY, VoiceStep.WASTE, VoiceStep.RATE, VoiceStep.COMMISSION,
            VoiceStep.LABOUR_CHARGES, VoiceStep.TRANSPORT_CHARGES, VoiceStep.OTHER_DEDUCTIONS -> {
                parseNumberFromText(lowerText)
            }
            VoiceStep.PHONE_NUMBER -> lowerText.filter { it.isDigit() }
            else -> rawText.trim()
        }
        
        Timber.d("VOICE_NORMALIZED_TEXT: $result (Step: $currentStep)")
        return result
    }

    private fun isSkipCommand(text: String): Boolean {
        val skipWords = listOf(
            "skip", "leave", "next", "vaddu", "vaddhu", "vaddule", "vadhule", "vadilei", "vadileyi", 
            "chhod do", "chhodo", "chodo", "vidal", "bitubidu", "vadu"
        )
        return skipWords.any { text.contains(it) }
    }

    private fun normalizeProductName(text: String): String {
        // Mango mapping
        val mangoWords = listOf("mango", "mamidi", "మామిడి", "aam", "आम", "maampazham", "மாம்பழம்", "mavinahannu", "మావినహణ్ణు")
        if (mangoWords.any { text.contains(it) }) return "Mango"
        
        // Tomato mapping
        val tomatoWords = listOf("tomato", "tamata", "tamatar", "thakkali", "టమాటా", "టమాట", "टमाटर", "தக்காளி")
        if (tomatoWords.any { text.contains(it) }) return "Tomato"

        // Potato mapping
        val potatoWords = listOf("potato", "bangaladumpa", "aaloo", "urulaikizhangu", "బంగాళదుంప", "आलू", "உருಳೆக்கிழங்கு")
        if (potatoWords.any { text.contains(it) }) return "Potato"

        // Onion mapping
        val onionWords = listOf("onion", "ullipaya", "pyaaz", "vengayam", "ఉల్లిపాయ", "प्याज", "வெங்காயம்")
        if (onionWords.any { text.contains(it) }) return "Onion"
        
        return text.replaceFirstChar { it.uppercase() }
    }

    private fun normalizeUnit(text: String): String {
        val kgWords = listOf("kg", "kilo", "kilogram", "కేజీ", "కిలో", "किलो", "கிலோ", "ಕೆಜಿ")
        if (kgWords.any { text.contains(it) }) return "KG"

        val tonWords = listOf("ton", "tonne", "టన్", "टन", "ಟನ್")
        if (tonWords.any { text.contains(it) }) return "Ton"

        val boxWords = listOf("box", "boxes", "petti", "పెట్టె", "dabba", "डब्बा", "பெட்டி", "ಪೆಟ್ಟಿಗೆ")
        if (boxWords.any { text.contains(it) }) return "Boxes"

        return "KG" // Default
    }

    private fun parseNumberFromText(text: String): String {
        // First try direct numeric extraction
        val directMatch = Regex("(\\d+(?:\\.\\d+)?)").find(text)
        if (directMatch != null) return directMatch.groupValues[1]

        // If not, try word to number conversion
        return wordToNumber(text).toString()
    }

    private fun wordToNumber(text: String): Double {
        var total = 0.0
        
        // Comprehensive mapping for supported languages
        val map = mapOf(
            // English
            "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
            "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
            "fifteen" to 15.0, "twenty" to 20.0, "thirty" to 30.0, "forty" to 40.0, "fifty" to 50.0,
            "hundred" to 100.0, "thousand" to 1000.0,
            
            // Telugu
            "okati" to 1.0, "rendu" to 2.0, "moodu" to 3.0, "naalugu" to 4.0, "aidu" to 5.0,
            "aaru" to 6.0, "edu" to 7.0, "enimidi" to 8.0, "tommidi" to 9.0, "padi" to 10.0,
            "vanda" to 100.0, "vandalu" to 100.0, "veyyi" to 1000.0,
            
            // Hindi
            "ek" to 1.0, "do" to 2.0, "teen" to 3.0, "char" to 4.0, "paanch" to 5.0,
            "chhay" to 6.0, "saat" to 7.0, "aath" to 8.0, "nau" to 9.0, "das" to 10.0,
            "sau" to 100.0, "hazaar" to 1000.0, "pandrah" to 15.0,
            
            // Tamil
            "onnu" to 1.0, "rendu" to 2.0, "moonu" to 3.0, "naalu" to 4.0, "anju" to 5.0,
            "aaru" to 6.0, "elu" to 7.0, "ettu" to 8.0, "onbadhu" to 9.0, "pathu" to 10.0,
            "nooru" to 100.0, "ayiram" to 1000.0,
            
            // Kannada
            "ondu" to 1.0, "eradu" to 2.0, "mooru" to 3.0, "nalku" to 4.0, "aidu" to 5.0,
            "aru" to 6.0, "elu" to 7.0, "entu" to 8.0, "ombattu" to 9.0, "hattu" to 10.0,
            "nooru" to 100.0, "savira" to 1000.0
        )

        val words = text.split(" ")
        var current = 0.0
        for (word in words) {
            val value = map[word]
            if (value != null) {
                if (value >= 100.0 && (current > 0 || total > 0)) {
                    if (current == 0.0) current = 1.0
                    current *= value
                    total += current
                    current = 0.0
                } else {
                    current += value
                }
            }
        }
        total += current
        
        return total
    }
}
