package com.dasariravi145.agrolynch.ui.screens.voice

enum class VoiceStep(val label: String) {
    LANGUAGE_SELECTION("Language Selection"),
    FARMER_NAME("Farmer Name"),
    VILLAGE("Village"),
    PRODUCT_NAME("Product"),
    GRADE("Grade"),
    UNIT("Unit"),
    
    // Unit-specific Quantity fields
    QUANTITY("Quantity"), // Used for KG/Ton
    WASTE("Waste"),       // Used for KG/Ton
    
    // Boxes-specific fields
    BOX_COUNT("Number of Boxes"),
    TOTAL_WEIGHT_TON("Total Weight (Ton)"),
    EMPTY_BOX_WEIGHT("Empty Weight per Box"),
    SPOILAGE_PERCENT("Spoilage %"),
    
    RATE("Rate"),
    COMPLETED("Completed")
}

data class VoiceLanguage(val name: String, val code: String, val locale: String)

data class VoiceState(
    val step: VoiceStep = VoiceStep.LANGUAGE_SELECTION,
    val selectedLanguage: VoiceLanguage? = null,
    val isListening: Boolean = false,
    val spokenText: String = "",
    val detectedValue: String = "",
    val awaitingConfirmation: Boolean = false,
    val confidence: Float = 1.0f,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val draft: com.dasariravi145.agrolynch.domain.model.FarmerArrivalDraft = com.dasariravi145.agrolynch.domain.model.FarmerArrivalDraft()
)
