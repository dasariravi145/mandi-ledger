package com.dasariravi145.agrolynch.ui.screens.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

sealed class VoiceNavigationEvent {
    data class NavigateToArrival(val draft: com.dasariravi145.agrolynch.domain.model.FarmerArrivalDraft) : VoiceNavigationEvent()
}

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<VoiceNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    
    val languages = listOf(
        VoiceLanguage("Auto", "auto", Locale.getDefault().toLanguageTag()),
        VoiceLanguage("English", "en", "en-US"),
        VoiceLanguage("Telugu", "te", "te-IN"),
        VoiceLanguage("Hindi", "hi", "hi-IN"),
        VoiceLanguage("Tamil", "ta", "ta-IN"),
        VoiceLanguage("Kannada", "kn", "kn-IN")
    )

    private val stepQuestions = mapOf(
        VoiceStep.FARMER_NAME to mapOf("en" to "Please say farmer name", "te" to "రైతు పేరు చెప్పండి", "hi" to "किसान का नाम बोलें"),
        VoiceStep.PHONE_NUMBER to mapOf("en" to "Say phone number or skip", "te" to "ఫోన్ నంబర్ చెప్పండి లేదా స్కిప్ చేయండి", "hi" to "फ़ोन नंबर बोलें या स्किप करें"),
        VoiceStep.VILLAGE to mapOf("en" to "Say village or skip", "te" to "గ్రామం చెప్పండి లేదా స్కిప్ చేయండి", "hi" to "गांव का नाम बोलें या स्किप करें"),
        VoiceStep.PRODUCT_NAME to mapOf("en" to "Say product name", "te" to "ఉత్పత్తి పేరు చెప్పండి", "hi" to "उत्पाद का नाम बोलें"),
        VoiceStep.GRADE to mapOf("en" to "Say grade", "te" to "గ్రేడ్ చెప్పండి", "hi" to "ग्रेड बोलें"),
        VoiceStep.UNIT to mapOf("en" to "Say unit: KG, Ton, or Boxes", "te" to "యూనిట్ చెప్పండి: కేజీ, టన్, లేదా బాక్సులు", "hi" to "यूनिट बोलें: केजी, टन, या बॉक्स"),
        VoiceStep.QUANTITY to mapOf("en" to "Say quantity", "te" to "పరిమాణం చెప్పండి", "hi" to "मात्रा बोलें"),
        VoiceStep.WASTE to mapOf("en" to "Say waste or skip", "te" to "వేస్ట్ లేదా డామేజ్ చెప్పండి", "hi" to "वेस्ट या डैमेज बोलें"),
        VoiceStep.RATE to mapOf("en" to "Say rate per KG", "te" to "కేజీ ధర చెప్పండి", "hi" to "प्रति केजी दर बोलें"),
        VoiceStep.COMMISSION to mapOf("en" to "Say commission percent or skip", "te" to "కమిషన్ శాతం చెప్పండి", "hi" to "कमीशन प्रतिशत बोलें"),
        VoiceStep.LABOUR_CHARGES to mapOf("en" to "Say labour charges or skip", "te" to "కూలి ఖర్చులు చెప్పండి", "hi" to "मजदूरी बोलें"),
        VoiceStep.TRANSPORT_CHARGES to mapOf("en" to "Say transport charges or skip", "te" to "రవాణా ఖర్చులు చెప్పండి", "hi" to "परिवहन शुल्क बोलें"),
        VoiceStep.OTHER_DEDUCTIONS to mapOf("en" to "Say other deductions or skip", "te" to "ఇతర తగ్గింపులు చెప్పండి", "hi" to "अन्य कटौती बोलें")
    )

    init {
        viewModelScope.launch {
            premiumStateManager.isPremium.collect { isPremium ->
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun selectLanguage(language: VoiceLanguage) {
        timber.log.Timber.d("VOICE_LANGUAGE_SELECTED: ${language.name}")
        _uiState.update { it.copy(
            selectedLanguage = language, 
            step = VoiceStep.FARMER_NAME
        ) }
    }

    fun getCurrentQuestion(): String {
        val state = _uiState.value
        val lang = state.selectedLanguage?.code ?: "en"
        return stepQuestions[state.step]?.get(lang) ?: stepQuestions[state.step]?.get("en") ?: ""
    }

    fun startListening(context: Context) {
        val locale = _uiState.value.selectedLanguage?.locale ?: "en-US"
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { _uiState.update { it.copy(isListening = true, error = null) } }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { _uiState.update { it.copy(isListening = false) } }
                override fun onError(error: Int) { 
                    timber.log.Timber.e("VOICE_STEP_FAILED: Step=${_uiState.value.step}, Error=$error")
                    _uiState.update { it.copy(isListening = false, error = "Speech error: $error") } 
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        handleSpokenResult(matches[0])
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        })
    }

    private fun handleSpokenResult(text: String) {
        val currentStep = _uiState.value.step
        timber.log.Timber.d("VOICE_STEP_CURRENT: $currentStep")
        timber.log.Timber.d("VOICE_RAW_TEXT: $text")
        
        val normalized = com.dasariravi145.agrolynch.util.VoiceTextNormalizer.normalizeVoiceText(text, currentStep)
        timber.log.Timber.d("VOICE_VALUE_DETECTED: $normalized")
        
        if (normalized.isBlank() && !isOptional(currentStep)) {
            _uiState.update { it.copy(error = "Please speak clearly") }
            return
        }

        updateDraftAndMoveToNextStep(normalized)
    }

    private fun isOptional(step: VoiceStep): Boolean {
        return when(step) {
            VoiceStep.PHONE_NUMBER, VoiceStep.VILLAGE, VoiceStep.WASTE, VoiceStep.COMMISSION,
            VoiceStep.LABOUR_CHARGES, VoiceStep.TRANSPORT_CHARGES, VoiceStep.OTHER_DEDUCTIONS -> true
            else -> false
        }
    }

    fun skipCurrentStep() {
        updateDraftAndMoveToNextStep("SKIP")
    }

    private fun updateDraftAndMoveToNextStep(value: String) {
        val currentStep = _uiState.value.step
        val isSkip = value == "SKIP"
        
        _uiState.update { state ->
            val updatedDraft = when (currentStep) {
                VoiceStep.FARMER_NAME -> state.draft.copy(farmerName = value)
                VoiceStep.PHONE_NUMBER -> state.draft.copy(phone = if (isSkip) "" else value)
                VoiceStep.VILLAGE -> state.draft.copy(village = if (isSkip) "" else value)
                VoiceStep.PRODUCT_NAME -> state.draft.copy(productName = value)
                VoiceStep.GRADE -> state.draft.copy(grade = value)
                VoiceStep.UNIT -> state.draft.copy(unitType = value)
                VoiceStep.QUANTITY -> state.draft.copy(quantity = value.toDoubleOrNull() ?: 0.0)
                VoiceStep.WASTE -> state.draft.copy(waste = if (isSkip) 0.0 else value.toDoubleOrNull() ?: 0.0)
                VoiceStep.RATE -> state.draft.copy(rate = value.toDoubleOrNull() ?: 0.0)
                VoiceStep.COMMISSION -> state.draft.copy(commissionPercent = if (isSkip) 0.0 else value.toDoubleOrNull() ?: 0.0)
                VoiceStep.LABOUR_CHARGES -> state.draft.copy(laborCharges = if (isSkip) 0.0 else value.toDoubleOrNull() ?: 0.0)
                VoiceStep.TRANSPORT_CHARGES -> state.draft.copy(transportCharges = if (isSkip) 0.0 else value.toDoubleOrNull() ?: 0.0)
                VoiceStep.OTHER_DEDUCTIONS -> state.draft.copy(otherDeductions = if (isSkip) 0.0 else value.toDoubleOrNull() ?: 0.0)
                else -> state.draft
            }
            
            timber.log.Timber.d("VOICE_FIELD_DETECTED: Field=$currentStep, Value=$value")
            timber.log.Timber.d("VOICE_DRAFT_UPDATED: ${updatedDraft}")

            val nextStep = getNextStep(currentStep)
            
            if (nextStep == VoiceStep.COMPLETED) {
                timber.log.Timber.d("VOICE_STEP_COMPLETED: Flow Finished")
                viewModelScope.launch {
                    _navigationEvent.emit(VoiceNavigationEvent.NavigateToArrival(updatedDraft))
                }
            }

            state.copy(
                draft = updatedDraft,
                step = nextStep,
                spokenText = if (isSkip) "Skipped" else value,
                error = null
            )
        }
    }

    private fun getNextStep(current: VoiceStep): VoiceStep {
        return when (current) {
            VoiceStep.FARMER_NAME -> VoiceStep.PHONE_NUMBER
            VoiceStep.PHONE_NUMBER -> VoiceStep.VILLAGE
            VoiceStep.VILLAGE -> VoiceStep.PRODUCT_NAME
            VoiceStep.PRODUCT_NAME -> VoiceStep.GRADE
            VoiceStep.GRADE -> VoiceStep.UNIT
            VoiceStep.UNIT -> VoiceStep.QUANTITY
            VoiceStep.QUANTITY -> VoiceStep.WASTE
            VoiceStep.WASTE -> VoiceStep.RATE
            VoiceStep.RATE -> VoiceStep.COMMISSION
            VoiceStep.COMMISSION -> VoiceStep.LABOUR_CHARGES
            VoiceStep.LABOUR_CHARGES -> VoiceStep.TRANSPORT_CHARGES
            VoiceStep.TRANSPORT_CHARGES -> VoiceStep.OTHER_DEDUCTIONS
            VoiceStep.OTHER_DEDUCTIONS -> VoiceStep.COMPLETED
            else -> VoiceStep.COMPLETED
        }
    }

    fun forceComplete() {
        timber.log.Timber.d("VOICE_REVIEW_OPENED: Transitioning to Manual Review")
        viewModelScope.launch {
            _navigationEvent.emit(VoiceNavigationEvent.NavigateToArrival(_uiState.value.draft))
        }
    }

    fun reset() { _uiState.update { VoiceState(isPremium = it.isPremium) } }
    fun stopListening() { speechRecognizer?.stopListening() }
    
    override fun onCleared() { 
        super.onCleared()
        speechRecognizer?.destroy()
    }
}
