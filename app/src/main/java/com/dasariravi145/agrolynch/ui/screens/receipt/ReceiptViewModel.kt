package com.dasariravi145.agrolynch.ui.screens.receipt

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.model.ReceiptData
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService
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
    private val pdfService: TemplateInvoicePdfService,
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

    fun generatePdf(context: Context, data: ReceiptData) {
        viewModelScope.launch {
            _companyProfile.value?.let { profile ->
                val file = pdfService.generatePaymentReceiptPdf(context, profile, data)
                _generatedPdfFile.value = file
            }
        }
    }
}
