package com.dasariravi145.agrolynch.ui.screens.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.ExtractedData
import com.dasariravi145.agrolynch.util.OcrParser
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

enum class ScanTarget {
    STOCK_ENTRY, SALE_ENTRY, PAYMENT
}

data class BillScanUiState(
    val isLoading: Boolean = false,
    val extractedData: ExtractedData = ExtractedData(),
    val currentImageBitmap: Bitmap? = null,
    val target: ScanTarget? = null,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val ocrFinished: Boolean = false
)

@HiltViewModel
class BillScanViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    private val _state = MutableStateFlow(BillScanUiState())
    val state = _state.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        timber.log.Timber.d("BillScanViewModel: OCR Engine initialized")
    }

    fun setScanTarget(target: ScanTarget) {
        _state.update { it.copy(target = target) }
    }

    fun processImage(bitmap: Bitmap) {
        Timber.d("BillScanViewModel: Processing image...")
        _state.update { it.copy(isLoading = true, error = null, currentImageBitmap = bitmap) }
        
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                viewModelScope.launch(Dispatchers.Default) {
                    try {
                        Timber.d("BillScanViewModel: Parsing OCR text...")
                        val parsed = OcrParser.parse(visionText.text)
                        withContext(Dispatchers.Main) {
                            _state.update { it.copy(
                                isLoading = false,
                                extractedData = parsed,
                                ocrFinished = true
                            ) }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "BillScanViewModel: Error parsing OCR results")
                        withContext(Dispatchers.Main) {
                            _state.update { it.copy(isLoading = false, error = "Failed to parse receipt") }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Timber.e(e, "BillScanViewModel: OCR recognition failed")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
    }

    fun confirmOcrData(updatedData: ExtractedData) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val isPremium = premiumStateManager.getCachedPremiumStatus()
            val bitmap = if (isPremium) _state.value.currentImageBitmap else null

            val scan = OcrScanEntity(
                scanId = UUID.randomUUID().toString(),
                billNumber = updatedData.billNumber,
                amount = updatedData.amount,
                billDate = updatedData.date,
                ocrText = updatedData.ocrText,
                transactionType = _state.value.target?.name ?: "",
                createdAt = System.currentTimeMillis()
            )
            
            ocrRepository.saveScanWithImage(scan, bitmap)
            
            _state.update { it.copy(isLoading = false, isSuccess = true, extractedData = updatedData) }
        }
    }

    fun resetState() {
        _state.value = BillScanUiState()
    }
}
