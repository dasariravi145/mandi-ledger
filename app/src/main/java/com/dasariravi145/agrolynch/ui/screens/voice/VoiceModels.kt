package com.dasariravi145.agrolynch.ui.screens.voice

enum class VoiceStep(val label: String) {
    LANGUAGE_SELECTION("Language Selection"),
    FARMER_NAME("Farmer Name"),
    PHONE_NUMBER("Phone Number"),
    VILLAGE("Village"),
    PRODUCT_NAME("Product Name"),
    GRADE("Grade"),
    UNIT("Unit"),
    QUANTITY("Quantity"),
    WASTE("Waste/Damage"),
    RATE("Rate"),
    COMMISSION("Commission %"),
    LABOUR_CHARGES("Labour Charges"),
    TRANSPORT_CHARGES("Transport Charges"),
    OTHER_DEDUCTIONS("Other Deductions"),
    COMPLETED("Completed")
}

data class VoiceLanguage(val name: String, val code: String, val locale: String)

data class Question(
    val key: String, 
    val text: Map<String, String>
)

data class VoiceState(
    val step: VoiceStep = VoiceStep.LANGUAGE_SELECTION,
    val selectedLanguage: VoiceLanguage? = null,
    val isListening: Boolean = false,
    val spokenText: String = "",
    val confidence: Float = 1.0f,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val draft: com.dasariravi145.agrolynch.domain.model.FarmerArrivalDraft = com.dasariravi145.agrolynch.domain.model.FarmerArrivalDraft()
)
