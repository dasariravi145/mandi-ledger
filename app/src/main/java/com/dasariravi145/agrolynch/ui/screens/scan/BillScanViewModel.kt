package com.dasariravi145.agrolynch.ui.screens.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.*
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
    val extractedData: ExtractedData = ExtractedData(),
    val currentImageBitmap: Bitmap? = null,
    val target: ScanTarget? = ScanTarget.STOCK_ENTRY,
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

    fun setScanTarget(target: ScanTarget) {
        _state.update { it.copy(target = target) }
    }

    fun processImage(bitmap: Bitmap, rotation: Int = 0) {
        val target = _state.value.target ?: return
        _state.update { it.copy(isLoading = true, error = null, currentImageBitmap = bitmap) }
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Preprocess & Orientation Correction
                val processedBitmap = ImagePreprocessor.preprocess(bitmap, rotation)
                
                // 2. Quality Validation
                val quality = ImagePreprocessor.checkQuality(processedBitmap)
                Timber.d("OCR_DEBUG: Size: ${processedBitmap.width}x${processedBitmap.height}, Rotation: $rotation")
                Timber.d("OCR_DEBUG: Quality: $quality")

                if (!quality.isClear) {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(isLoading = false, error = quality.message) }
                    }
                    return@launch
                }

                // 3. Try ML Kit First (Basic Extraction)
                val image = InputImage.fromBitmap(processedBitmap, 0)
                val visionText = recognizer.process(image).await()
                var parsed = OcrParser.parse(visionText.text, target.name)
                
                Timber.d("OCR_DEBUG: ML Kit Text Length: ${visionText.text.length}")
                Timber.d("OCR_DEBUG: ML Kit Confidence: ${parsed.confidenceScore}")

                // 4. If Premium and (Low Confidence or missing required fields), use AI Vision
                val isPremium = premiumStateManager.getCachedPremiumStatus()
                val isLowConfidence = parsed.confidenceScore < 75f || parsed.lowConfidenceFields.isNotEmpty()
                
                if (isPremium && isLowConfidence) {
                    Timber.i("OCR_DEBUG: Starting AI Vision extraction (ML Kit confidence too low)...")
                    val aiParsed = GeminiService.extractBillData(processedBitmap, target.name)
                    if (aiParsed != null) {
                        parsed = aiParsed
                        Timber.i("OCR_DEBUG: AI Vision successful. Confidence: ${parsed.confidenceScore}")
                    } else {
                        Timber.e("OCR_DEBUG: AI Vision extraction failed.")
                    }
                }

                withContext(Dispatchers.Main) {
                    _state.update { it.copy(
                        isLoading = false,
                        extractedData = parsed,
                        ocrFinished = true,
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

    fun confirmOcrData(updatedData: ExtractedData) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val isPremium = premiumStateManager.getCachedPremiumStatus()
            val bitmap = if (isPremium) _state.value.currentImageBitmap else null

            val scan = OcrScanEntity(
                scanId = UUID.randomUUID().toString(),
                billNumber = updatedData.billNumber,
                amount = if (updatedData.amount > 0) updatedData.amount else updatedData.netAmount,
                billDate = updatedData.date,
                ocrText = updatedData.ocrText,
                transactionType = _state.value.target?.name ?: "",
                farmerName = updatedData.farmerName,
                productName = updatedData.productName,
                productGrade = updatedData.grade,
                unit = updatedData.unit,
                numberOfBoxes = updatedData.numberOfBoxes,
                totalWeightTon = updatedData.totalWeightTon,
                emptyBoxWeightPerBox = updatedData.emptyBoxWeightPerBox,
                totalEmptyBoxWeightKg = updatedData.totalEmptyBoxWeightKg,
                spoilagePercentage = updatedData.spoilagePercentage,
                rate = updatedData.rate,
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
