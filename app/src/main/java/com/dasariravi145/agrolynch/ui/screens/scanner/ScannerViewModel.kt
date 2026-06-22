package com.dasariravi145.agrolynch.ui.screens.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    data class Captured(val bitmap: Bitmap) : ScannerUiState()
    object Processing : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onImageCaptured(bitmap: Bitmap) {
        _uiState.value = ScannerUiState.Captured(bitmap)
    }

    fun reset() {
        _uiState.value = ScannerUiState.Idle
    }
}
