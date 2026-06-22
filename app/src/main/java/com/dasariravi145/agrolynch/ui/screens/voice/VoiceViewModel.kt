package com.dasariravi145.agrolynch.ui.screens.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.model.FarmerArrivalDraft
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.VoiceTextNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

sealed class VoiceNavigationEvent {
    data class NavigateToArrival(val draft: FarmerArrivalDraft, val autoSave: Boolean = false) : VoiceNavigationEvent()
}

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<VoiceNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    val languages = listOf(
        VoiceLanguage("English", "en", "en-IN"),
        VoiceLanguage("Telugu", "te", "te-IN"),
        VoiceLanguage("Hindi", "hi", "hi-IN"),
        VoiceLanguage("Tamil", "ta", "ta-IN"),
        VoiceLanguage("Kannada", "kn", "kn-IN")
    )

    private val stepQuestions = mapOf(
        VoiceStep.FARMER_NAME to mapOf("en" to "Say farmer name", "te" to "రైతు పేరు చెప్పండి", "hi" to "किसान का नाम बोलें", "ta" to "விவசாயி பெயரைச் சொல்லுங்கள்", "kn" to "ರೈತರ ಹೆಸರನ್ನು ಹೇಳಿ"),
        VoiceStep.VILLAGE to mapOf("en" to "Say farmer village", "te" to "రైతు గ్రామం పేరు చెప్పండి", "hi" to "किसान के गांव का नाम बोलें", "ta" to "விவசாயியின் ஊரைச் சொல்லுங்கள்", "kn" to "ರೈತರ ಗ್ರಾಮವನ್ನು ಹೇಳಿ"),
        VoiceStep.PRODUCT_NAME to mapOf("en" to "Say product name", "te" to "పంట పేరు చెప్పండి", "hi" to "उत्पाद का नाम बोलें", "ta" to "பொருள் பெயரைச் சொல்லுங்கள்", "kn" to "ಉತ್ಪನ್ನದ ಹೆಸರನ್ನು ಹೇಳಿ"),
        VoiceStep.GRADE to mapOf("en" to "Say grade (Grade A, B, or C)", "te" to "గ్రేడ్ చెప్పండి", "hi" to "ग्रेड बोलें", "ta" to "தரத்தைச் சொல்லுங்கள்", "kn" to "ಗ್ರೇಡ್ ಹೇಳಿ"),
        VoiceStep.UNIT to mapOf("en" to "Say unit KG, Ton, or Boxes", "te" to "యూనిట్ కేజీ, టన్, లేదా బాక్సులు చెప్పండి", "hi" to "यूनिट केजी, टन, या बॉक्स बोलें", "ta" to "அலகு கேஜி, டன் அல்லது பெட்டிகளைச் சொல்லுங்கள்", "kn" to "ಘಟಕ ಕೆಜಿ, ಟನ್ ಅಥವಾ ಪೆಟ್ಟಿಗೆಗಳನ್ನು ಹೇಳಿ"),
        
        // KG/Ton specific
        VoiceStep.QUANTITY to mapOf("en" to "Say total quantity", "te" to "పరిమాణం చెప్పండి", "hi" to "मात्रा बोलें", "ta" to "அளவைச் சொல்லுங்கள்", "kn" to "ಪ್ರಮಾಣವನ್ನು ಹೇಳಿ"),
        VoiceStep.WASTE to mapOf("en" to "Say waste quantity", "te" to "వేస్ట్ పరిమాణం చెప్పండి", "hi" to "वेस्ट मात्रा बोलें", "ta" to "கழிவு அளவைச் சொல்லுங்கள்", "kn" to "ತ್ಯಾಜ್ಯ ಪ್ರಮಾಣವನ್ನು ಹೇಳಿ"),
        
        // Boxes specific
        VoiceStep.BOX_COUNT to mapOf("en" to "Say number of boxes", "te" to "బాక్సుల సంఖ్య చెప్పండి", "hi" to "बॉक्स की संख्या बोलें", "ta" to "பெட்டிகளின் எண்ணிக்கையைச் சொல்லுங்கள்", "kn" to "ಪೆಟ್ಟಿಗೆಗಳ ಸಂಖ್ಯೆಯನ್ನು ಹೇಳಿ"),
        VoiceStep.TOTAL_WEIGHT_TON to mapOf("en" to "Say total weight in Tons", "te" to "మొత్తం బరువు టన్నులలో చెప్పండి", "hi" to "टन में कुल वजन बोलें", "ta" to "டன் கணக்கில் மொத்த எடையைச் சொல்லுங்கள்", "kn" to "ಟನ್‌ಗಳಲ್ಲಿ ಒಟ್ಟು ತೂಕವನ್ನು ಹೇಳಿ"),
        VoiceStep.EMPTY_BOX_WEIGHT to mapOf("en" to "Say empty weight per box in KG", "te" to "ఖాళీ బాక్స్ బరువు కేజీలలో చెప్పండి", "hi" to "प्रति बॉक्स खाली वजन बोलें", "ta" to "ஒரு பெட்டியின் காலி எடையைச் சொல்லுங்கள்", "kn" to "ಪ್ರತಿ ಪೆಟ್ಟಿಗೆಯ ಖಾಲಿ ತೂಕವನ್ನು ಹೇಳಿ"),
        VoiceStep.SPOILAGE_PERCENT to mapOf("en" to "Say spoilage percentage", "te" to "వేస్ట్ శాతం చెప్పండి", "hi" to "वेस्ट प्रतिशत बोलें", "ta" to "கழிவு சதவீதத்தைச் சொல்லுங்கள்", "kn" to "ತ್ಯಾಜ್ಯ ಶೇಕಡಾವಾರು ಹೇಳಿ"),
        
        VoiceStep.RATE to mapOf("en" to "Say rate per unit", "te" to "ధర చెప్పండి", "hi" to "दर बोलें", "ta" to "விலையைச் சொல்லுங்கள்", "kn" to "ದರ ಹೇಳಿ"),
    )

    init {
        viewModelScope.launch {
            premiumStateManager.isPremium.collect { isPremium ->
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun selectLanguage(language: VoiceLanguage) {
        _uiState.update { it.copy(selectedLanguage = language, step = VoiceStep.FARMER_NAME) }
    }

    fun getCurrentQuestion(): String {
        val state = _uiState.value
        val lang = state.selectedLanguage?.code ?: "en"
        return stepQuestions[state.step]?.get(lang) ?: stepQuestions[state.step]?.get("en") ?: ""
    }

    fun onSpokenResult(text: String) {
        val normalized = VoiceTextNormalizer.normalizeVoiceText(text, _uiState.value.step)
        _uiState.update { it.copy(spokenText = text, detectedValue = normalized, awaitingConfirmation = true) }
    }

    fun confirmValue() {
        val state = _uiState.value
        val value = state.detectedValue
        val isSkip = value == "SKIP"
        
        val updatedDraft = when (state.step) {
            VoiceStep.FARMER_NAME -> state.draft.copy(farmerName = value)
            VoiceStep.VILLAGE -> state.draft.copy(village = if (isSkip) "" else value)
            VoiceStep.PRODUCT_NAME -> state.draft.copy(productName = value)
            VoiceStep.GRADE -> state.draft.copy(grade = if (isSkip) "Grade A" else value)
            VoiceStep.UNIT -> state.draft.copy(unitType = if (isSkip) "KG" else value)
            
            // KG/Ton mapping
            VoiceStep.QUANTITY -> state.draft.copy(quantity = value.toDoubleOrNull() ?: 0.0)
            VoiceStep.WASTE -> state.draft.copy(waste = if (isSkip) 0.0 else value.toDoubleOrNull() ?: 0.0)
            
            // Boxes mapping
            VoiceStep.BOX_COUNT -> state.draft.copy(numBoxes = value.toDoubleOrNull()?.toInt() ?: 0)
            VoiceStep.TOTAL_WEIGHT_TON -> state.draft.copy(totalWeightTon = value.toDoubleOrNull() ?: 0.0)
            VoiceStep.EMPTY_BOX_WEIGHT -> state.draft.copy(emptyWeightPerBox = value.toDoubleOrNull() ?: 0.0)
            VoiceStep.SPOILAGE_PERCENT -> state.draft.copy(spoilagePercent = value.toDoubleOrNull() ?: 0.0)
            
            VoiceStep.RATE -> state.draft.copy(rate = value.toDoubleOrNull() ?: 0.0)
            else -> state.draft
        }

        val nextStep = getNextStep(state.step, updatedDraft.unitType)
        _uiState.update { it.copy(
            draft = updatedDraft, 
            step = nextStep, 
            awaitingConfirmation = false, 
            spokenText = "", 
            detectedValue = ""
        ) }
        
        if (nextStep == VoiceStep.COMPLETED) {
            viewModelScope.launch {
                _navigationEvent.emit(VoiceNavigationEvent.NavigateToArrival(updatedDraft, autoSave = false))
            }
        }
    }

    private fun getNextStep(current: VoiceStep, unitType: String): VoiceStep {
        return when (current) {
            VoiceStep.FARMER_NAME -> VoiceStep.VILLAGE
            VoiceStep.VILLAGE -> VoiceStep.PRODUCT_NAME
            VoiceStep.PRODUCT_NAME -> VoiceStep.GRADE
            VoiceStep.GRADE -> VoiceStep.UNIT
            VoiceStep.UNIT -> {
                if (unitType == "Boxes") VoiceStep.BOX_COUNT else VoiceStep.QUANTITY
            }
            
            // KG/Ton Flow
            VoiceStep.QUANTITY -> VoiceStep.WASTE
            VoiceStep.WASTE -> VoiceStep.RATE
            
            // Boxes Flow
            VoiceStep.BOX_COUNT -> VoiceStep.TOTAL_WEIGHT_TON
            VoiceStep.TOTAL_WEIGHT_TON -> VoiceStep.EMPTY_BOX_WEIGHT
            VoiceStep.EMPTY_BOX_WEIGHT -> VoiceStep.SPOILAGE_PERCENT
            VoiceStep.SPOILAGE_PERCENT -> VoiceStep.RATE
            
            VoiceStep.RATE -> VoiceStep.COMPLETED
            else -> VoiceStep.COMPLETED
        }
    }

    fun isOptional(step: VoiceStep): Boolean {
        return when(step) {
            VoiceStep.VILLAGE, VoiceStep.WASTE, VoiceStep.SPOILAGE_PERCENT -> true
            else -> false
        }
    }

    fun retry() { _uiState.update { it.copy(awaitingConfirmation = false, spokenText = "", detectedValue = "", error = null) } }
    fun updateManually(value: String) { _uiState.update { it.copy(detectedValue = value) } }
    fun skipCurrentStep() { updateManually("SKIP"); confirmValue() }
    
    fun editField(step: VoiceStep) { _uiState.update { it.copy(step = step) } }
    
    fun confirmAndSave() {
        viewModelScope.launch {
            _navigationEvent.emit(VoiceNavigationEvent.NavigateToArrival(_uiState.value.draft, autoSave = false))
        }
    }

    fun reset() { _uiState.update { VoiceState(isPremium = it.isPremium) } }
    fun forceComplete() {
        viewModelScope.launch {
            _navigationEvent.emit(VoiceNavigationEvent.NavigateToArrival(_uiState.value.draft, autoSave = false))
        }
    }
}
