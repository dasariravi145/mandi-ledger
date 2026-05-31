package com.dasariravi145.agrolynch.ui.screens.voice

enum class VoiceStep {
    LANGUAGE_SELECTION,
    SERVICE_SELECTION,
    INTERACTIVE_QUESTIONS,
    REVIEW,
    SUCCESS
}

enum class VoiceService {
    ADD_STOCK, ADD_SALE, BUYER_PAYMENT, FARMER_PAYMENT, ADD_FARMER, ADD_BUYER, ADD_PRODUCT
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

data class VoiceState(
    val step: VoiceStep = VoiceStep.LANGUAGE_SELECTION,
    val selectedLanguage: VoiceLanguage? = null,
    val selectedService: VoiceService? = null,
    val isListening: Boolean = false,
    val currentQuestionIndex: Int = 0,
    val capturedData: Map<String, String> = emptyMap(),
    val spokenText: String = "",
    val error: String? = null,
    val isPremium: Boolean = false,
    val existingRecordFound: Boolean = false,
    val isLoading: Boolean = false,
    val suggestedRecord: Any? = null,
    val dynamicQuestions: List<Question> = emptyList()
)
