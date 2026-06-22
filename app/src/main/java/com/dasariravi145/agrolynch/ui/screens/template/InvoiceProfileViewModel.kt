package com.dasariravi145.agrolynch.ui.screens.template

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.util.pdf.InvoiceHtmlGenerator
import com.dasariravi145.agrolynch.util.pdf.renderer.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class InvoiceProfileViewModel @Inject constructor(
    private val companyRepository: CompanyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _profile = companyRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val profile = _profile

    private val _previewHtml = MutableStateFlow<String?>(null)
    val previewHtml = _previewHtml.asStateFlow()

    // Keep previewFile for compatibility if needed, but we'll use HTML for live preview
    private val _previewFile = MutableStateFlow<File?>(null)
    val previewFile = _previewFile.asStateFlow()

    fun updateProfile(update: (CompanyProfileEntity) -> CompanyProfileEntity) {
        _profile.value?.let { current ->
            viewModelScope.launch {
                companyRepository.updateProfile(update(current))
            }
        }
    }

    fun saveAssetLocally(uri: Uri, type: String) {
        viewModelScope.launch {
            val file = File(context.filesDir, "branding_${type}_${System.currentTimeMillis()}.png")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                val path = file.absolutePath
                updateProfile { profile ->
                    when (type) {
                        "logo" -> profile.copy(logoPath = path)
                        "god" -> profile.copy(godImagePath = path)
                        "signature" -> profile.copy(signaturePath = path)
                        "stamp" -> profile.copy(stampPath = path)
                        "upi_qr" -> profile.copy(upiQrPath = path)
                        else -> profile
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "ASSET_SAVE_FAILED")
            }
        }
    }

    fun generateLivePreview() {
        val p = _profile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val businessProfile = BusinessProfile(
                    companyName = p.companyName,
                    address = p.address,
                    mobile = p.mobile1,
                    gstNumber = p.gstNumber,
                    tagline = p.tagline,
                    logoPath = p.logoPath,
                    qrPath = p.upiQrPath,
                    signaturePath = p.signaturePath,
                    godImagePath = p.godImagePath,
                    stampPath = p.stampPath
                )
                
                val sampleInvoice = InvoiceData(
                    billNumber = "SAMPLE-001",
                    date = System.currentTimeMillis(),
                    customerName = "Sample Customer",
                    customerMobile = "9876543210",
                    products = listOf(
                        InvoiceProduct("Product A", "Premium", "KG", 100.0, 50.0, 5000.0),
                        InvoiceProduct("Product B", "Medium", "KG", 200.0, 30.0, 6000.0)
                    ),
                    subtotal = 11000.0,
                    commission = 550.0,
                    transport = 200.0,
                    labour = 150.0,
                    advance = 0.0,
                    others = 0.0,
                    grandTotal = 10100.0,
                    vehicleNumber = "KA-01-AB-1234"
                )
                
                val templateId = when (p.defaultTemplate) {
                    "GK_FRUITS_CLASSIC" -> "gk_fruits_classic"
                    "ROYAL_HERITAGE_MANDI" -> "royal_heritage_mandi"
                    "DIAMOND_BUSINESS_ELITE" -> "diamond_business_elite"
                    "PREMIUM_FRUIT_GALLERY" -> "premium_fruit_gallery"
                    "EXECUTIVE_GLASS_STYLE" -> "executive_glass_style"
                    "COMPACT_THERMAL_PRINT" -> "compact_print"
                    else -> "gk_fruits_classic"
                }
                
                val html = InvoiceHtmlGenerator.buildHtml(context, templateId, businessProfile, sampleInvoice)
                _previewHtml.value = html
            } catch (e: Exception) {
                Timber.e(e, "PREVIEW_FAILED")
            }
        }
    }

    fun saveAll() {
        generateLivePreview()
    }
}
