package com.dasariravi145.agrolynch.ui.screens.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val premiumStateManager: PremiumStateManager,
    private val farmerRepository: FarmerRepository,
    private val productRepository: ProductRepository,
    private val arrivalRepository: ArrivalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceState())
    val uiState = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    
    val languages = listOf(
        VoiceLanguage("English", "en", "en-US"),
        VoiceLanguage("Telugu", "te", "te-IN"),
        VoiceLanguage("Hindi", "hi", "hi-IN")
    )

    private val stockQuestions = listOf(
        Question("farmer_name", mapOf("en" to "Farmer Name?", "te" to "రైతు పేరు?")),
        Question("product_name", mapOf("en" to "Product Name?", "te" to "ఉత్పత్తి పేరు?")),
        Question("amount", mapOf("en" to "Approximate Amount?", "te" to "సుమారు మొత్తం?"))
    )

    init {
        viewModelScope.launch {
            premiumStateManager.isPremium.collect { isPremium ->
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun selectLanguage(language: VoiceLanguage) {
        _uiState.update { it.copy(
            selectedLanguage = language, 
            step = VoiceStep.INTERACTIVE_QUESTIONS,
            dynamicQuestions = stockQuestions,
            currentQuestionIndex = 0,
            session = VoiceSession()
        ) }
    }

    fun getCurrentQuestion(): String {
        val state = _uiState.value
        val lang = state.selectedLanguage?.code ?: "en"
        if (state.currentQuestionIndex >= state.dynamicQuestions.size) return ""
        val question = state.dynamicQuestions[state.currentQuestionIndex]
        return question.text[lang] ?: question.text["en"] ?: ""
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
                override fun onError(error: Int) { _uiState.update { it.copy(isListening = false, error = "Speech error: $error") } }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    if (!matches.isNullOrEmpty()) {
                        handleSpokenResult(matches[0], confidences?.get(0) ?: 1.0f)
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

    private fun handleSpokenResult(text: String, confidence: Float) {
        val state = _uiState.value
        val currentQuestion = state.dynamicQuestions[state.currentQuestionIndex]
        val session = state.session.copy()

        _uiState.update { it.copy(spokenText = text, confidence = confidence) }

        viewModelScope.launch {
            when (currentQuestion.key) {
                "farmer_name" -> {
                    session.farmerName = text
                    val existing = farmerRepository.getFarmers().first().find { it.name.contains(text, true) }
                    if (existing != null) {
                        session.farmerName = existing.name
                        session.farmerPhone = existing.mobileNumber
                        session.farmerAddress = existing.village
                    }
                }
                "product_name" -> {
                    session.productName = text
                    val existing = productRepository.getProductByName(text)
                    if (existing != null) {
                        session.productName = existing.name
                        session.productCategory = existing.category
                        session.unit = if (existing.availableGrades.isNotEmpty()) "KG" else "KG" // Default KG
                    }
                }
                "amount" -> {
                    session.amount = text.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                }
            }

            _uiState.update { it.copy(session = session) }
            
            if (state.currentQuestionIndex < state.dynamicQuestions.size - 1) {
                _uiState.update { it.copy(currentQuestionIndex = state.currentQuestionIndex + 1, spokenText = "") }
            } else {
                _uiState.update { it.copy(step = VoiceStep.EDITABLE_FORM, spokenText = "") }
            }
        }
    }

    fun updateSession(updatedSession: VoiceSession) {
        _uiState.update { it.copy(session = updatedSession) }
    }

    fun saveEntry() {
        if (_uiState.value.session.farmerPhone.isNotBlank() && _uiState.value.session.farmerPhone.length != 10) {
            _uiState.update { it.copy(error = "Please enter a valid 10-digit mobile number") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val session = _uiState.value.session
            
            // 1. Handle Farmer Creation/Resolution
            val existingFarmers = farmerRepository.getFarmers().first()
            val farmer = existingFarmers.find { it.name.equals(session.farmerName, true) }
            val farmerId = if (farmer == null) {
                val newId = UUID.randomUUID().toString()
                farmerRepository.addFarmer(FarmerEntity(
                    id = newId,
                    name = session.farmerName,
                    mobileNumber = session.farmerPhone,
                    village = session.farmerAddress
                ))
                newId
            } else {
                farmer.id
            }

            // 2. Handle Product Creation/Resolution
            val existingProduct = productRepository.getProductByName(session.productName)
            val productId = if (existingProduct == null) {
                val newId = UUID.randomUUID().toString()
                productRepository.addProduct(ProductEntity(
                    id = newId,
                    name = session.productName,
                    category = session.productCategory,
                    availableGrades = listOf(session.grade)
                ), null)
                newId
            } else {
                existingProduct.id
            }

            // 3. Save Arrival
            val result = arrivalRepository.addArrival(ArrivalEntity(
                id = UUID.randomUUID().toString(),
                farmerId = farmerId,
                farmerName = session.farmerName,
                productId = productId,
                productName = session.productName,
                productCategory = session.productCategory,
                grade = session.grade,
                quantity = session.quantity,
                remainingQuantity = session.quantity - session.spoilage,
                spoilageQuantity = session.spoilage,
                unit = session.unit,
                purchaseRate = session.rate,
                grossAmount = session.quantity * session.rate,
                commissionPercent = session.commission,
                commissionAmount = (session.quantity * session.rate * session.commission / 100),
                transportCharges = session.transport,
                laborCharges = session.labor,
                packingCharges = session.packing,
                netAmount = (session.quantity * session.rate) - (session.quantity * session.rate * session.commission / 100) - session.transport - session.labor - session.packing,
                date = session.date
            ))

            if (result is Resource.Success) {
                _uiState.update { it.copy(isLoading = false, step = VoiceStep.SUCCESS) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun reset() { _uiState.update { VoiceState(isPremium = it.isPremium) } }
    fun stopListening() { speechRecognizer?.stopListening() }
    
    override fun onCleared() { 
        super.onCleared()
        speechRecognizer?.destroy()
    }
}
