package com.dasariravi145.agrolynch.ui.screens.receipt

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.model.ReceiptData
import com.dasariravi145.agrolynch.util.PdfGenerator
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val companyRepository: CompanyRepository,
    private val pdfService: com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService,
    private val premiumStateManager: com.dasariravi145.agrolynch.util.PremiumStateManager
) : ViewModel() {

    private val _companyProfile = companyRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val companyProfile: StateFlow<com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity?> = _companyProfile

    val isPremium = premiumStateManager.isPremium

    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()

    fun saveDefaultTemplate(templateId: String) {
        viewModelScope.launch {
            _companyProfile.value?.let { profile ->
                companyRepository.updateProfile(profile.copy(defaultTemplate = templateId))
            }
        }
    }

    fun generateAndSharePdf(context: Context, data: ReceiptData) {
        viewModelScope.launch {
            val file = _generatedPdfFile.value ?: run {
                val profile = _companyProfile.value ?: return@launch
                // Use the new rendering engine service methods
                pdfService.generatePaymentReceiptPdf(context, profile, data)
            }
            
            if (file != null) {
                _generatedPdfFile.value = file
                PdfGenerator.sharePdf(context, file)
            }
        }
    }

    fun generateAndPrintPdf(context: Context, data: ReceiptData) {
        viewModelScope.launch {
            val file = _generatedPdfFile.value ?: run {
                val profile = _companyProfile.value ?: return@launch
                pdfService.generatePaymentReceiptPdf(context, profile, data)
            }
            
            if (file != null) {
                _generatedPdfFile.value = file
                PdfGenerator.printPdf(context, file)
            }
        }
    }

    fun printBill(context: Context, file: File) {
        PdfGenerator.printPdf(context, file)
    }

    fun shareBill(context: Context, file: File) {
        PdfGenerator.sharePdf(context, file)
    }
}
