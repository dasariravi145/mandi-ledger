package com.dasariravi145.agrolynch.ui.screens.voice

enum class VoiceStep {
    LANGUAGE_SELECTION,
    INTERACTIVE_QUESTIONS,
    EDITABLE_FORM,
    SUCCESS
}

enum class VoiceService {
    ADD_STOCK
}

data class VoiceLanguage(val name: String, val code: String, val locale: String)

data class Question(
    val key: String, 
    val text: Map<String, String>,
    val fieldType: QuestionType = QuestionType.TEXT
)

enum class QuestionType {
    TEXT, NUMBER, CHOICE
}

// Transaction-specific models for editable form
data class VoiceSession(
    var farmerName: String = "",
    var farmerPhone: String = "",
    var farmerAddress: String = "",
    var productName: String = "",
    var productCategory: String = "Fruit",
    var grade: String = "Grade A",
    var quantity: Double = 0.0,
    var spoilage: Double = 0.0,
    var unit: String = "KG",
    var rate: Double = 0.0,
    var amount: Double = 0.0, // Voice captured amount
    var commission: Double = 5.0,
    var transport: Double = 0.0,
    var labor: Double = 0.0,
    var packing: Double = 0.0,
    var remarks: String = "",
    var date: Long = System.currentTimeMillis()
)

data class VoiceState(
    val step: VoiceStep = VoiceStep.LANGUAGE_SELECTION,
    val selectedLanguage: VoiceLanguage? = null,
    val selectedService: VoiceService = VoiceService.ADD_STOCK,
    val isListening: Boolean = false,
    val currentQuestionIndex: Int = 0,
    val session: VoiceSession = VoiceSession(),
    val spokenText: String = "",
    val confidence: Float = 1.0f,
    val error: String? = null,
    val isLoading: Boolean = false,
    val dynamicQuestions: List<Question> = emptyList(),
    val isPremium: Boolean = false
)
