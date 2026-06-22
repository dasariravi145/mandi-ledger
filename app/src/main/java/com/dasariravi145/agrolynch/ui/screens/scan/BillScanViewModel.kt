package com.dasariravi145.agrolynch.ui.screens.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.*
import com.dasariravi145.agrolynch.util.ocr.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

enum class ScanTarget(val label: String) {
    STOCK_ENTRY("Farmer Stock Bill"),
    SALE_ENTRY("Buyer Sale Bill"),
    PAYMENT("Payment Receipt"),
    CHEQUE("Cheque"),
    EXPENSE("Expense Bill")
}

data class BillScanUiState(
    val isLoading: Boolean = false,
    val extractedData: ExtractedBillData = ExtractedBillData(),
    val currentImageBitmap: Bitmap? = null,
    val target: ScanTarget? = ScanTarget.STOCK_ENTRY,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val ocrFinished: Boolean = false,
    val showReview: Boolean = false
)

@HiltViewModel
class BillScanViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    private val _state = MutableStateFlow(BillScanUiState())
    val state = _state.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun setScanTarget(target: ScanTarget) {
        _state.update { it.copy(target = target) }
    }

    fun processImage(bitmap: Bitmap, rotation: Int = 0) {
        Timber.d("OCR_SCAN_STARTED")
        val target = _state.value.target ?: return
        _state.update { it.copy(isLoading = true, error = null, currentImageBitmap = bitmap) }
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Preprocess using the new OcrPreProcessor
                val processedBitmap = OcrPreProcessor.preprocess(bitmap, rotation)
                
                // 2. OCR using ML Kit via BillTextExtractor
                val visionText = BillTextExtractor.extractText(processedBitmap)
                
                if (visionText == null) {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(isLoading = false, error = "Could not read bill clearly. Please retry or enter manually.") }
                    }
                    return@launch
                }

                // 3. Parse and Validate
                var parsed = BillStructureParser.parse(visionText)
                parsed = BillValidationEngine.validate(parsed)
                
                Timber.d("OCR_DEBUG: Extracted Farmer: ${parsed.farmerName}")
                Timber.d("OCR_DEBUG: Items found: ${parsed.items.size}")

                withContext(Dispatchers.Main) {
                    _state.update { it.copy(
                        isLoading = false,
                        extractedData = parsed,
                        ocrFinished = true,
                        showReview = true,
                        currentImageBitmap = processedBitmap
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "OCR_DEBUG: Processing pipeline crashed")
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isLoading = false, error = "Failed to process bill. Please try again.") }
                }
            }
        }
    }

    fun updateExtractedData(data: ExtractedBillData) {
        _state.update { it.copy(extractedData = data) }
    }

    fun confirmOcrData(updatedData: ExtractedBillData) {
        _state.update { it.copy(extractedData = updatedData, isSuccess = true) }
    }

    fun resetState() {
        _state.value = BillScanUiState()
    }
}
