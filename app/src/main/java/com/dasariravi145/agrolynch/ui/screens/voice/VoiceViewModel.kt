package com.dasariravi145.agrolynch.ui.screens.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
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
    private val buyerRepository: BuyerRepository,
    private val productRepository: ProductRepository,
    private val arrivalRepository: ArrivalRepository,
    private val saleRepository: SaleRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceState())
    val uiState = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    
    val languages = listOf(
        VoiceLanguage("English", "en", "en-US"),
        VoiceLanguage("Telugu", "te", "te-IN"),
        VoiceLanguage("Hindi", "hi", "hi-IN"),
        VoiceLanguage("Tamil", "ta", "ta-IN"),
        VoiceLanguage("Kannada", "kn", "kn-IN"),
        VoiceLanguage("Malayalam", "ml", "ml-IN"),
        VoiceLanguage("Marathi", "mr", "mr-IN")
    )

    init {
        viewModelScope.launch {
            premiumStateManager.isPremium.collect { isPremium ->
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun selectLanguage(language: VoiceLanguage) {
        _uiState.update { it.copy(selectedLanguage = language, step = VoiceStep.SERVICE_SELECTION) }
    }

    fun selectService(service: VoiceService) {
        _uiState.update { it.copy(selectedService = service, step = VoiceStep.INTERACTIVE_QUESTIONS, currentQuestionIndex = 0, capturedData = emptyMap()) }
    }

    private fun getQuestionsForService(service: VoiceService): List<Question> {
        return when (service) {
            VoiceService.ADD_FARMER -> listOf(
                Question("name", mapOf("en" to "Farmer Name?", "te" to "రైతు పేరు?", "hi" to "किसान का नाम?")),
                Question("mobile", mapOf("en" to "Mobile Number?", "te" to "మొబైల్ సంఖ్య?", "hi" to "मोबाइल नंबर?")),
                Question("village", mapOf("en" to "Village?", "te" to "గ్రామం?", "hi" to "गांव?")),
                Question("address", mapOf("en" to "Address?", "te" to "చిరునామా?", "hi" to "पता?")),
                Question("notes", mapOf("en" to "Any Notes?", "te" to "నోట్స్?", "hi" to "कोई टिप्पणी?"))
            )
            VoiceService.ADD_BUYER -> listOf(
                Question("name", mapOf("en" to "Buyer Name?", "te" to "వ్యాపారి పేరు?", "hi" to "खरीदार का नाम?")),
                Question("mobile", mapOf("en" to "Mobile Number?", "te" to "మొబైల్ సంఖ్య?", "hi" to "मोबाइल नंबर?")),
                Question("location", mapOf("en" to "Location?", "te" to "ప్రాంతం?", "hi" to "स्थान?")),
                Question("address", mapOf("en" to "Address?", "te" to "చిరునామా?", "hi" to "पता?")),
                Question("notes", mapOf("en" to "Any Notes?", "te" to "నోట్స్?", "hi" to "कोई टिप्पणी?"))
            )
            VoiceService.ADD_PRODUCT -> listOf(
                Question("name", mapOf("en" to "Product Name?", "te" to "ఉత్పత్తి పేరు?", "hi" to "उत्पाद का नाम?")),
                Question("category", mapOf("en" to "Category (Fruit or Vegetable)?", "te" to "వర్గం (పండు లేదా కూరగాయ)?", "hi" to "श्रेणी (फल या सब्जी)?")),
                Question("grade", mapOf("en" to "Grade (A, B, C)?", "te" to "గ్రేడ్ (A, B, C)?", "hi" to "ग्रेड (A, B, C)?")),
                Question("unit", mapOf("en" to "Unit (KG, Bags, Boxes)?", "te" to "యూనిట్ (KG, సంచులు, బాక్సులు)?", "hi" to "इकाई (KG, बैग, बॉक्स)?"))
            )
            VoiceService.ADD_STOCK -> listOf(
                Question("farmer", mapOf("en" to "Farmer Name?", "te" to "రైతు పేరు?", "hi" to "किसान का नाम?")),
                Question("product", mapOf("en" to "Product Name?", "te" to "ఉత్పత్తి పేరు?", "hi" to "उत्पाद का नाम?")),
                Question("grade", mapOf("en" to "Grade?", "te" to "గ్రేడ్?", "hi" to "ग्रेड?")),
                Question("quantity", mapOf("en" to "Quantity?", "te" to "పరిమాణం?", "hi" to "मात्रा?")),
                Question("unit", mapOf("en" to "Unit?", "te" to "యూనిట్?", "hi" to "इकाई?")),
                Question("rate", mapOf("en" to "Purchase Rate?", "te" to "కొనుగోలు ధర?", "hi" to "खरीद दर?")),
                Question("commission", mapOf("en" to "Commission Percent?", "te" to "కమీషన్ శాతం?", "hi" to "कमीशन प्रतिशत?"))
            )
            VoiceService.ADD_SALE -> listOf(
                Question("buyer", mapOf("en" to "Buyer Name?", "te" to "వ్యాపారి పేరు?", "hi" to "खरीदार का नाम?")),
                Question("product", mapOf("en" to "Product Name?", "te" to "ఉత్పత్తి పేరు?", "hi" to "उत्पाद का नाम?")),
                Question("grade", mapOf("en" to "Grade?", "te" to "గ్రేడ్?", "hi" to "ग्रेड?")),
                Question("quantity", mapOf("en" to "Quantity?", "te" to "పరిమాణం?", "hi" to "मात्रा?")),
                Question("rate", mapOf("en" to "Sale Rate?", "te" to "అమ్మకం ధర?", "hi" to "बिक्री दर?")),
                Question("labor", mapOf("en" to "Labor Charge?", "te" to "హమాలీ ఛార్జ్?", "hi" to "मजदूरी शुल्क?")),
                Question("transport", mapOf("en" to "Transport Charge?", "te" to "రవాణా ఛార్జ్?", "hi" to "परिवहन शुल्क?"))
            )
            VoiceService.ADD_PAYMENT -> listOf(
                Question("type", mapOf("en" to "Payment For (Farmer or Buyer)?", "te" to "చెల్లింపు ఎవరికి (రైతు లేదా వ్యాపారి)?", "hi" to "भुगतान किसके लिए (किसान या खरीदार)?")),
                Question("name", mapOf("en" to "Name?", "te" to "పేరు?", "hi" to "नाम?")),
                Question("amount", mapOf("en" to "Amount?", "te" to "మొత్తం?", "hi" to "राशि?")),
                Question("method", mapOf("en" to "Payment Method (Cash, Online, Cheque)?", "te" to "చెల్లింపు విధానం (నగదు, ఆన్‌లైన్, చెక్కు)?", "hi" to "भुगतान विधि (नकद, ऑनलाइन, चेक)?")),
                Question("notes", mapOf("en" to "Notes?", "te" to "నోట్స్?", "hi" to "टिप्पणी?"))
            )
        }
    }

    fun getCurrentQuestion(): String {
        val state = _uiState.value
        val service = state.selectedService ?: return ""
        val lang = state.selectedLanguage?.code ?: "en"
        val questions = getQuestionsForService(service)
        val index = state.currentQuestionIndex
        if (index >= questions.size) return ""
        return questions[index].text[lang] ?: questions[index].text["en"] ?: ""
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
                    _uiState.update { it.copy(isListening = false, error = "Speech recognition error code: $error") }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) handleSpokenResult(matches[0])
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun handleSpokenResult(text: String) {
        val state = _uiState.value
        val questions = getQuestionsForService(state.selectedService!!)
        val currentQuestion = questions[state.currentQuestionIndex]
        
        val updatedCapturedData = state.capturedData.toMutableMap()
        updatedCapturedData[currentQuestion.key] = text
        _uiState.update { it.copy(capturedData = updatedCapturedData, spokenText = text) }

        // Smart Auto-Fetch Logic
        if (currentQuestion.key == "name" || currentQuestion.key == "farmer" || currentQuestion.key == "buyer" || currentQuestion.key == "product") {
            performSmartSearch(currentQuestion.key, text)
        } else {
            moveToNextQuestion()
        }
    }

    private fun performSmartSearch(key: String, value: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (key) {
                "name", "farmer" -> {
                    val farmer = farmerRepository.getFarmers().first().find { it.name.contains(value, true) }
                    if (farmer != null) {
                        _uiState.update { it.copy(isLoading = false, existingRecordFound = true, suggestedRecord = farmer) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        moveToNextQuestion()
                    }
                }
                "buyer" -> {
                    val buyer = buyerRepository.getBuyers().first().find { it.name.contains(value, true) }
                    if (buyer != null) {
                        _uiState.update { it.copy(isLoading = false, existingRecordFound = true, suggestedRecord = buyer) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        moveToNextQuestion()
                    }
                }
                "product" -> {
                    val product = productRepository.getProductByName(value)
                    if (product != null) {
                        _uiState.update { it.copy(isLoading = false, existingRecordFound = true, suggestedRecord = product) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        moveToNextQuestion()
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                    moveToNextQuestion()
                }
            }
        }
    }

    fun useExistingRecord(use: Boolean) {
        val state = _uiState.value
        if (use && state.suggestedRecord != null) {
            val record = state.suggestedRecord
            val data = state.capturedData.toMutableMap()
            when (record) {
                is FarmerEntity -> {
                    data["farmer_id"] = record.id
                    data["farmer"] = record.name
                    data["name"] = record.name
                    data["mobile"] = record.mobileNumber
                    data["village"] = record.village
                }
                is BuyerEntity -> {
                    data["buyer_id"] = record.id
                    data["buyer"] = record.name
                    data["name"] = record.name
                    data["mobile"] = record.mobileNumber
                }
                is ProductEntity -> {
                    data["product_id"] = record.id
                    data["product"] = record.name
                    data["name"] = record.name
                }
            }
            _uiState.update { it.copy(capturedData = data) }
        }
        _uiState.update { it.copy(existingRecordFound = false, suggestedRecord = null) }
        moveToNextQuestion()
    }

    private fun moveToNextQuestion() {
        val state = _uiState.value
        val questions = getQuestionsForService(state.selectedService!!)
        if (state.currentQuestionIndex < questions.size - 1) {
            _uiState.update { it.copy(currentQuestionIndex = state.currentQuestionIndex + 1, spokenText = "") }
        } else {
            _uiState.update { it.copy(step = VoiceStep.REVIEW, spokenText = "") }
        }
    }

    fun saveEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val data = _uiState.value.capturedData
            val result: Resource<Unit> = when (_uiState.value.selectedService) {
                VoiceService.ADD_FARMER -> {
                    farmerRepository.addFarmer(FarmerEntity(
                        id = UUID.randomUUID().toString(),
                        name = data["name"] ?: "",
                        mobileNumber = data["mobile"] ?: "",
                        village = data["village"] ?: "",
                        notes = data["notes"] ?: ""
                    ))
                }
                VoiceService.ADD_BUYER -> {
                    buyerRepository.addBuyer(BuyerEntity(
                        id = UUID.randomUUID().toString(),
                        name = data["name"] ?: "",
                        mobileNumber = data["mobile"] ?: "",
                        address = data["address"] ?: "",
                        notes = data["notes"] ?: ""
                    ))
                }
                VoiceService.ADD_PRODUCT -> {
                    productRepository.addProduct(ProductEntity(
                        id = UUID.randomUUID().toString(),
                        name = data["name"] ?: "",
                        category = data["category"] ?: "General",
                        availableGrades = listOf(data["grade"] ?: "A Grade")
                    ), null)
                }
                VoiceService.ADD_STOCK -> {
                    val qty = data["quantity"]?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
                    val rate = data["rate"]?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
                    val commPercent = data["commission"]?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 5.0
                    val gross = qty * rate
                    val commAmt = (gross * commPercent) / 100
                    
                    arrivalRepository.addArrival(ArrivalEntity(
                        id = UUID.randomUUID().toString(),
                        farmerId = data["farmer_id"] ?: UUID.randomUUID().toString(),
                        farmerName = data["farmer"] ?: "Unknown",
                        productId = data["product_id"] ?: UUID.randomUUID().toString(),
                        productName = data["product"] ?: "Unknown",
                        grade = data["grade"] ?: "A Grade",
                        quantity = qty,
                        remainingQuantity = qty,
                        unit = data["unit"] ?: "KG",
                        purchaseRate = rate,
                        grossAmount = gross,
                        commissionPercent = commPercent,
                        commissionAmount = commAmt,
                        netAmount = gross - commAmt
                    ))
                }
                else -> Resource.Success(Unit) 
            }

            if (result is Resource.Success) {
                _uiState.update { it.copy(isLoading = false, step = VoiceStep.SUCCESS) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = (result as Resource.Error).message) }
            }
        }
    }

    fun reset() {
        _uiState.update { VoiceState(isPremium = it.isPremium) }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}
