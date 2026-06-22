package com.dasariravi145.agrolynch.ui.screens.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.dasariravi145.agrolynch.util.ocr.ExtractedBillData
import com.dasariravi145.agrolynch.util.ocr.BillValidationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

data class OcrReviewUiState(
    val bitmap: Bitmap? = null,
    val originalData: ExtractedBillData = ExtractedBillData(),
    val mappedData: ExtractedBillData = ExtractedBillData(),
    val target: ScanTarget = ScanTarget.STOCK_ENTRY,
    val isConfirmed: Boolean = false
) {
    val isValid: Boolean
        get() = BillValidationEngine.checkStatus(mappedData).isValid
}

@HiltViewModel
class OcrReviewViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(OcrReviewUiState())
    val state = _state.asStateFlow()

    fun initialize(bitmap: Bitmap?, data: ExtractedBillData, target: ScanTarget) {
        _state.update { it.copy(
            bitmap = bitmap,
            originalData = data,
            mappedData = data,
            target = target,
            isConfirmed = false
        ) }
    }

    fun updateData(updated: ExtractedBillData) {
        _state.update { it.copy(mappedData = updated) }
    }

    fun confirm() {
        if (_state.value.isValid) {
            _state.update { it.copy(isConfirmed = true) }
        }
    }
}
