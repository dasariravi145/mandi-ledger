package com.dasariravi145.agrolynch.ui.screens.template

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.model.InvoiceWizardConfig
import com.dasariravi145.agrolynch.domain.model.ElementLayout
import com.dasariravi145.agrolynch.util.pdf.InvoiceHtmlGenerator
import com.dasariravi145.agrolynch.util.pdf.renderer.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import timber.log.Timber

enum class EditorMode {
    GUIDED, FREE_EDIT, REVIEW
}

@HiltViewModel
class InvoiceProfileViewModel @Inject constructor(
    private val companyRepository: CompanyRepository,
    private val templateRepository: com.dasariravi145.agrolynch.domain.repository.TemplatePositionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfessionalInvoiceUiState())
    val uiState = _uiState.asStateFlow()

    private val _previewHtml = MutableStateFlow<String?>(null)
    val previewHtml = _previewHtml.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    // Performance Optimization: Cache Base64 strings of assets
    private val assetBase64Cache = mutableMapOf<String, String>()
    private var previewJob: Job? = null

    val guidedElementOrder = listOf(
        InvoiceWizardConfig.KEY_ADDRESS,
        InvoiceWizardConfig.KEY_PHONE,
        InvoiceWizardConfig.KEY_GSTIN,
        InvoiceWizardConfig.KEY_GOD_IMAGE,
        InvoiceWizardConfig.KEY_SHOP_NAME,
        InvoiceWizardConfig.KEY_TAGLINE,
        InvoiceWizardConfig.KEY_LOGO,
        InvoiceWizardConfig.KEY_QR_CODE,
        InvoiceWizardConfig.KEY_THANK_YOU,
        InvoiceWizardConfig.KEY_SIGNATURE_BLOCK,
        InvoiceWizardConfig.KEY_STAMP,
        InvoiceWizardConfig.KEY_WATERMARK
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val profile = companyRepository.getProfile().firstOrNull() ?: CompanyProfileEntity(
                    id = 1,
                    companyName = "My Mandi Shop",
                    address = "Mandi Market",
                    defaultTemplate = "GK_FRUITS_CLASSIC"
                )
                
                if (companyRepository.getProfile().firstOrNull() == null) {
                    companyRepository.updateProfile(profile)
                }

                val config = templateRepository.getWizardConfig(profile.defaultTemplate) 
                    ?: getDefaultConfigForTemplate(profile.defaultTemplate)

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        wizardConfig = config,
                        selectedTemplateId = profile.defaultTemplate,
                        selectedElementKey = guidedElementOrder.first()
                    )
                }
                generateLivePreview(immediate = true)
            } catch (e: Exception) {
                Timber.e(e, "LOAD_INITIAL_DATA_FAILED")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun getDefaultConfigForTemplate(templateId: String): InvoiceWizardConfig {
        return when (templateId) {
            "GK_FRUITS_CLASSIC" -> InvoiceWizardConfig(
                template = templateId,
                shopNameFontSize = 46f,
                shopNameAlignment = "CENTER"
            )
            "ROYAL_HERITAGE_MANDI" -> InvoiceWizardConfig(
                template = templateId,
                shopNameFontSize = 42f,
                shopNameAlignment = "CENTER",
                theme = "GOLD"
            )
            else -> InvoiceWizardConfig(template = templateId)
        }
    }

    fun onBusinessDetailChanged(update: (CompanyProfileEntity) -> CompanyProfileEntity) {
        _uiState.update { 
            val newProfile = update(it.profile)
            it.copy(profile = newProfile, isDirty = true)
        }
        generateLivePreview()
    }

    fun onWizardConfigChanged(update: (InvoiceWizardConfig) -> InvoiceWizardConfig) {
        _uiState.update { state ->
            val newConfig = update(state.wizardConfig)
            state.copy(
                wizardConfig = newConfig, 
                isDirty = true,
                undoStack = (listOf(state.wizardConfig) + state.undoStack).take(20),
                redoStack = emptyList()
            )
        }
        generateLivePreview()
    }

    fun setEditorMode(mode: EditorMode) {
        _uiState.update { state ->
            val nextKey = if (mode == EditorMode.GUIDED) guidedElementOrder.first() else state.selectedElementKey
            state.copy(editorMode = mode, selectedElementKey = nextKey)
        }
    }

    fun selectElement(key: String?) {
        _uiState.update { state ->
            val index = if (key != null) guidedElementOrder.indexOf(key) else state.guidedStepIndex
            state.copy(
                selectedElementKey = key,
                guidedStepIndex = if (index != -1) index else state.guidedStepIndex
            )
        }
    }

    fun nextStep() {
        _uiState.update { state ->
            val visibleElements = guidedElementOrder.filter { state.wizardConfig.getLayout(it).visible }
            val currentKey = state.selectedElementKey
            val currentIndex = visibleElements.indexOf(currentKey)
            
            if (currentIndex < visibleElements.size - 1) {
                val nextKey = visibleElements[currentIndex + 1]
                state.copy(
                    guidedStepIndex = guidedElementOrder.indexOf(nextKey),
                    selectedElementKey = nextKey
                )
            } else {
                // Before finishing, check for overlaps
                val overlapError = checkCriticalOverlaps(state.wizardConfig)
                if (overlapError != null) {
                    viewModelScope.launch { _message.emit(overlapError) }
                    state // Stay on last step if error
                } else {
                    state.copy(editorMode = EditorMode.REVIEW, selectedElementKey = null)
                }
            }
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            val visibleElements = guidedElementOrder.filter { state.wizardConfig.getLayout(it).visible }
            val currentKey = state.selectedElementKey
            val currentIndex = visibleElements.indexOf(currentKey)
            
            if (currentIndex > 0) {
                val prevKey = visibleElements[currentIndex - 1]
                state.copy(
                    guidedStepIndex = guidedElementOrder.indexOf(prevKey),
                    selectedElementKey = prevKey
                )
            } else state
        }
    }

    fun skipStep() {
        nextStep()
    }

    fun updateElementLayout(key: String, update: (ElementLayout) -> ElementLayout) {
        _uiState.update { state ->
            val currentLayout = state.wizardConfig.getLayout(key)
            val newLayout = update(currentLayout)
            val newLayouts = state.wizardConfig.layouts.toMutableMap()
            newLayouts[key] = newLayout
            val newConfig = state.wizardConfig.copy(layouts = newLayouts)
            state.copy(wizardConfig = newConfig, isDirty = true)
        }
        generateLivePreview()
    }

    fun applyLayoutChange(key: String, update: (ElementLayout) -> ElementLayout) {
        updateElementLayout(key, update)
        recordUndo()
    }

    fun recordUndo() {
        _uiState.update { state ->
            state.copy(
                undoStack = (listOf(state.wizardConfig) + state.undoStack).take(20),
                redoStack = emptyList()
            )
        }
    }

    fun undo() {
        _uiState.update { state ->
            if (state.undoStack.isNotEmpty()) {
                val prev = state.undoStack.first()
                state.copy(
                    wizardConfig = prev,
                    undoStack = state.undoStack.drop(1),
                    redoStack = (listOf(state.wizardConfig) + state.redoStack).take(20),
                    isDirty = true
                )
            } else state
        }
        generateLivePreview(immediate = true)
    }

    fun redo() {
        _uiState.update { state ->
            if (state.redoStack.isNotEmpty()) {
                val next = state.redoStack.first()
                state.copy(
                    wizardConfig = next,
                    redoStack = state.redoStack.drop(1),
                    undoStack = (listOf(state.wizardConfig) + state.undoStack).take(20),
                    isDirty = true
                )
            } else state
        }
        generateLivePreview(immediate = true)
    }

    fun onTemplateSelected(templateId: String) {
        val previousId = _uiState.value.selectedTemplateId
        Timber.d("TEMPLATE_SELECT: Clicked=$templateId, Previous=$previousId")
        
        viewModelScope.launch {
            // Force the config to have the correct template ID even if loaded from DB
            val config = (templateRepository.getWizardConfig(templateId) 
                ?: getDefaultConfigForTemplate(templateId)).copy(template = templateId)
            
            // Clear image cache when template changes to avoid reusing old assets incorrectly
            InvoiceHtmlGenerator.clearCache()
            
            _uiState.update { 
                it.copy(
                    wizardConfig = config,
                    selectedTemplateId = templateId,
                    isDirty = true,
                    guidedStepIndex = 0,
                    selectedElementKey = guidedElementOrder.first()
                )
            }
            
            // Also update the profile's default template immediately in the state so generateLivePreview uses it
            onBusinessDetailChanged { it.copy(defaultTemplate = templateId) }
            
            Timber.d("TEMPLATE_SELECT: New State ID=${_uiState.value.selectedTemplateId}, Profile Default=${_uiState.value.profile.defaultTemplate}")
            generateLivePreview(immediate = true)
        }
    }

    fun saveAssetLocally(uri: Uri, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(context.filesDir, "branding_${type}_${System.currentTimeMillis()}.png")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                val path = file.absolutePath
                assetBase64Cache.remove(type)

                _uiState.update { state ->
                    val updatedProfile = when (type) {
                        "logo" -> state.profile.copy(logoPath = path)
                        "god" -> state.profile.copy(godImagePath = path)
                        "signature" -> state.profile.copy(signaturePath = path)
                        "stamp" -> state.profile.copy(stampPath = path)
                        "upi_qr" -> state.profile.copy(upiQrPath = path)
                        "watermark" -> state.profile.copy(customTemplatePath = path)
                        else -> state.profile
                    }
                    state.copy(profile = updatedProfile, isDirty = true)
                }
                // Refresh QR scan test if needed
                generateLivePreview(immediate = true)
            } catch (e: Exception) {
                Timber.e(e, "ASSET_SAVE_FAILED")
                _message.emit("Unable to update the selected image.")
            }
        }
    }

    fun removeAsset(type: String) {
        assetBase64Cache.remove(type)
        _uiState.update { state ->
            val updatedProfile = when (type) {
                "logo" -> state.profile.copy(logoPath = null)
                "god" -> state.profile.copy(godImagePath = null)
                "signature" -> state.profile.copy(signaturePath = null)
                "stamp" -> state.profile.copy(stampPath = null)
                "upi_qr" -> state.profile.copy(upiQrPath = null)
                "watermark" -> state.profile.copy(customTemplatePath = null)
                else -> state.profile
            }
            state.copy(profile = updatedProfile, isDirty = true)
        }
        generateLivePreview(immediate = true)
    }

    fun resetToDefault() {
        val templateId = _uiState.value.selectedTemplateId
        val defaultConfig = getDefaultConfigForTemplate(templateId)
        _uiState.update { it.copy(wizardConfig = defaultConfig, isDirty = true) }
        generateLivePreview(immediate = true)
    }

    fun autoArrangeLayout() {
        _uiState.update { state ->
            val cleanConfig = state.wizardConfig.copy(layouts = emptyMap())
            state.copy(wizardConfig = cleanConfig, isDirty = true)
        }
        generateLivePreview(immediate = true)
    }

    fun updatePageStyle(update: (InvoiceWizardConfig) -> InvoiceWizardConfig) {
        _uiState.update { state ->
            val newConfig = update(state.wizardConfig)
            state.copy(wizardConfig = newConfig, isDirty = true)
        }
        generateLivePreview()
    }

    fun resetPageStyle() {
        _uiState.update { state ->
            val newConfig = state.wizardConfig.copy(
                pageBackgroundOption = "DEFAULT",
                pageBorderThickness = "NONE",
                pageCornerStyle = "SQUARE"
            )
            state.copy(wizardConfig = newConfig, isDirty = true)
        }
        generateLivePreview(immediate = true)
    }

    fun resetElement(key: String) {
        _uiState.update { state ->
            val currentConfig = state.wizardConfig
            val newLayouts = currentConfig.layouts.toMutableMap()
            newLayouts.remove(key)
            
            var newConfig = currentConfig.copy(layouts = newLayouts)
            
            if (key == InvoiceWizardConfig.KEY_QR_CODE) {
                newConfig = newConfig.copy(
                    qrSizeOption = "MEDIUM",
                    showQrLabel = true,
                    qrLabelStyle = "BOLD",
                    qrLabelColorOption = "DEFAULT"
                )
            }

            state.copy(wizardConfig = newConfig, isDirty = true)
        }
        generateLivePreview(immediate = true)
    }

    fun generateLivePreview(immediate: Boolean = false) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            if (!immediate) {
                delay(300) 
            }
            
            val state = _uiState.value
            val p = state.profile
            val cfg = state.wizardConfig
            
            try {
                val businessProfile = BusinessProfile(
                    companyName = p.companyName,
                    address = formatAddress(p),
                    mobile = com.dasariravi145.agrolynch.util.Formatter.formatBusinessPhones(p.mobile1, p.mobile2),
                    gstNumber = p.gstNumber,
                    tagline = p.tagline,
                    logoPath = p.logoPath,
                    qrPath = p.upiQrPath,
                    signaturePath = p.signaturePath,
                    godImagePath = p.godImagePath,
                    stampPath = p.stampPath,
                    proprietor = p.proprietorName,
                    watermarkImagePath = p.customTemplatePath,
                    marketName = p.marketName,
                    city = p.city,
                    state = p.state,
                    pincode = p.pincode
                )
                
                val sampleInvoice = CANONICAL_SAMPLE_DATA.copy(
                    billNumber = "BILL-123",
                    date = System.currentTimeMillis(),
                    customerName = "SAMPLE CUSTOMER NAME",
                    customerMobile = "9876543210",
                    customerGstin = "36AAAAA0000A1Z5",
                    customerState = "Telangana",
                    placeOfSupply = p.state ?: "Telangana",
                    reverseCharge = false,
                    paymentMode = "CASH"
                )
                val templateId = mapTemplateTypeToId(state.selectedTemplateId)
                Timber.d("PREVIEW_GEN: SelectedID=${state.selectedTemplateId}, ResolvedTemplate=$templateId")
                
                val html = InvoiceHtmlGenerator.buildHtml(context, templateId, businessProfile, sampleInvoice, cfg)
                _previewHtml.value = html
            } catch (e: Exception) {
                Timber.e(e, "PREVIEW_GENERATION_FAILED")
            }
        }
    }

    private fun formatAddress(p: CompanyProfileEntity): String {
        val parts = mutableListOf<String>()
        if (p.address.isNotBlank()) parts.add(p.address)
        if (p.marketName.isNotBlank()) parts.add(p.marketName)
        if (p.village.isNotBlank()) parts.add(p.village)
        if (p.city.isNotBlank()) parts.add(p.city)
        if (p.district.isNotBlank()) parts.add(p.district)
        if (p.state.isNotBlank()) parts.add(p.state)
        if (p.pincode.isNotBlank()) parts.add(p.pincode)
        return parts.distinct().joinToString(", ")
    }

    private fun mapTemplateTypeToId(type: String): String {
        return when (type) {
            "GK_FRUITS_CLASSIC" -> "gk_fruits_classic"
            "ROYAL_HERITAGE_MANDI" -> "royal_heritage_mandi"
            "DIAMOND_BUSINESS_ELITE" -> "diamond_business_elite"
            "PREMIUM_FRUIT_GALLERY" -> "premium_fruit_gallery"
            "EXECUTIVE_GLASS_STYLE" -> "executive_glass_style"
            else -> "gk_fruits_classic"
        }
    }

    fun saveAll() {
        val state = _uiState.value
        viewModelScope.launch {
            // Basic Overlap/Bounds Validation
            val overlapError = checkCriticalOverlaps(state.wizardConfig)
            if (overlapError != null) {
                _message.emit(overlapError)
                return@launch
            }
            
            // QR Size Validation
            val qrLayout = state.wizardConfig.getLayout(InvoiceWizardConfig.KEY_QR_CODE)
            if (qrLayout.visible && state.wizardConfig.qrSizeOption == "SMALL") {
                _message.emit("QR code is small and may be difficult to scan reliably. Consider using Medium size.")
            }

            _uiState.update { it.copy(isSaving = true) }
            try {
                // CRITICAL: Force synchronization of template ID across all parts of the state
                val templateId = state.selectedTemplateId
                val finalProfile = state.profile.copy(defaultTemplate = templateId)
                val finalConfig = state.wizardConfig.copy(template = templateId)
                
                Timber.d("SAVE_ALL: Saving TemplateID=$templateId")
                
                companyRepository.updateProfile(finalProfile)
                // Clear cache on save to ensure next PDF/Print uses fresh assets with the new template
                InvoiceHtmlGenerator.clearCache()
                
                _uiState.update { it.copy(
                    profile = finalProfile,
                    wizardConfig = finalConfig,
                    isSaving = false, 
                    isDirty = false
                ) }
                _message.emit("Template saved successfully.")
            } catch (e: Exception) {
                Timber.e(e, "SAVE_ALL_FAILED")
                _uiState.update { it.copy(isSaving = false) }
                _message.emit("Unable to save template settings.")
            }
        }
    }

    private fun checkCriticalOverlaps(config: InvoiceWizardConfig): String? {
        val table = config.getLayout(InvoiceWizardConfig.KEY_PRODUCT_TABLE)
        val totals = config.getLayout(InvoiceWizardConfig.KEY_TOTALS_BOX)
        val qr = config.getLayout(InvoiceWizardConfig.KEY_QR_CODE)
        val sig = config.getLayout(InvoiceWizardConfig.KEY_SIGNATURE_BLOCK)
        
        // Example overlap check: Table and Totals
        if (table.visible && totals.visible) {
            if (isOverlapping(table, totals)) {
                return "Product Table and Totals Box are overlapping. Please adjust them."
            }
        }
        
        if (qr.visible && (isOverlapping(qr, table) || isOverlapping(qr, totals) || (sig.visible && isOverlapping(qr, sig)))) {
            return "QR Code is overlapping with other elements. Please move it."
        }

        if (sig.visible && isOverlapping(sig, totals)) {
            return "Signature is overlapping with Totals Box. Please move it down."
        }

        return null
    }

    private fun isOverlapping(l1: ElementLayout, l2: ElementLayout): Boolean {
        // Use a small buffer to avoid "touching" being counted as overlap
        val buffer = 0.5f
        return !(l1.xPercent + l1.widthPercent <= l2.xPercent + buffer ||
                 l2.xPercent + l2.widthPercent <= l1.xPercent + buffer ||
                 l1.yPercent + l1.heightPercent <= l2.yPercent + buffer ||
                 l2.yPercent + l2.heightPercent <= l1.yPercent + buffer)
    }

    companion object {
        val CANONICAL_SAMPLE_DATA = InvoiceData(
            billNumber = "SAMPLE-001",
            date = 1721584200000L, 
            customerName = "SAMPLE CUSTOMER",
            customerMobile = "9876543210",
            products = listOf(
                InvoiceProduct("Product A", "Variety X", "Premium", "KG", 100.0, 50.0, 5000.0),
                InvoiceProduct("Product B", "Variety Y", "Medium", "KG", 200.0, 30.0, 6000.0)
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
    }
}

data class ProfessionalInvoiceUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val profile: CompanyProfileEntity = CompanyProfileEntity(),
    val wizardConfig: InvoiceWizardConfig = InvoiceWizardConfig(),
    val selectedTemplateId: String = "GK_FRUITS_CLASSIC",
    val isDirty: Boolean = false,
    val editorMode: EditorMode = EditorMode.GUIDED,
    val guidedStepIndex: Int = 0,
    val selectedElementKey: String? = null,
    val undoStack: List<InvoiceWizardConfig> = emptyList(),
    val redoStack: List<InvoiceWizardConfig> = emptyList()
)
