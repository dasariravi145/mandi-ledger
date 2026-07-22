package com.dasariravi145.agrolynch.ui.screens.ledger

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.model.*
import com.dasariravi145.agrolynch.domain.repository.LedgerRepository
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.domain.repository.FarmerRepository
import com.dasariravi145.agrolynch.domain.repository.BuyerRepository
import com.dasariravi145.agrolynch.util.LedgerExportService
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.findActivity
import com.dasariravi145.agrolynch.util.PdfGenerator
import com.dasariravi145.agrolynch.util.PdfPrintHelper
import com.dasariravi145.agrolynch.util.PdfActionManager
import com.dasariravi145.agrolynch.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class LedgerFilter(
    val query: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val product: String = "",
    val transactionType: TransactionType? = null
)

enum class ShareType {
    WHATSAPP, PDF, OTHER
}

enum class ArchiveState {
    IDLE, VALIDATING, SNAPSHOT_CREATING, BACKING_UP, READY_FOR_DELETE, DELETING, SUCCESS, FAILED
}

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repository: com.dasariravi145.agrolynch.domain.repository.LedgerRepository,
    private val archiveRepository: com.dasariravi145.agrolynch.domain.repository.ArchiveRepository,
    private val backupRepository: com.dasariravi145.agrolynch.domain.repository.BackupRepository,
    private val settingsRepository: com.dasariravi145.agrolynch.domain.repository.SettingsRepository,
    private val saleRepository: com.dasariravi145.agrolynch.domain.repository.SaleRepository,
    private val companyRepository: CompanyRepository,
    private val farmerRepository: FarmerRepository,
    private val buyerRepository: BuyerRepository,
    private val premiumStateManager: PremiumStateManager,
    private val exportService: LedgerExportService
) : ViewModel() {

    val isPremium = premiumStateManager.isPremium
    val companyProfile = companyRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lastRestoreInfo = settingsRepository.lastRestoreInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun dismissRestoreBanner() {
        viewModelScope.launch {
            settingsRepository.updateLastRestoreInfo(null)
        }
    }

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _archiveState = MutableStateFlow(ArchiveState.IDLE)
    val archiveState = _archiveState.asStateFlow()
    
    private val _archiveMessage = MutableSharedFlow<String>()
    val archiveMessage = _archiveMessage.asSharedFlow()
    
    private var pendingArchiveId: String? = null

    private val _isPrinting = MutableStateFlow<String?>(null)
    val isPrinting = _isPrinting.asStateFlow()

    private val _isSharing = MutableStateFlow<String?>(null)
    val isSharing = _isSharing.asStateFlow()

    private val _tabIndex = MutableStateFlow(0)
    val tabIndex: StateFlow<Int> = _tabIndex.asStateFlow()

    private val _filter = MutableStateFlow(LedgerFilter())
    val filter = _filter.asStateFlow()

    val farmerSummaries: StateFlow<List<LedgerSummary>> = repository.getAllFarmerSummaries()
        .map { summaries ->
            val f = _filter.value
            summaries.filter { it.partyName.contains(f.query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val buyerSummaries: StateFlow<List<LedgerSummary>> = repository.getAllBuyerSummaries()
        .map { summaries ->
            val f = _filter.value
            summaries.filter { it.partyName.contains(f.query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(index: Int) {
        _tabIndex.value = index
    }

    fun updateSearchQuery(query: String) {
        _filter.value = _filter.value.copy(query = query)
    }

    fun updateTypeFilter(type: TransactionType?) {
        _filter.value = _filter.value.copy(transactionType = type)
    }

    fun updateDateRange(start: Long?, end: Long?) {
        _filter.value = _filter.value.copy(startDate = start, endDate = end)
    }

    private val _currentSummary = MutableStateFlow<LedgerSummary?>(null)

    fun getFarmerLedger(farmerId: String): Flow<LedgerSummary> {
        return repository.getFarmerLedger(farmerId).onEach { _currentSummary.value = it }.map { applyDetailFilter(it) }
    }

    fun getBuyerLedger(buyerId: String): Flow<LedgerSummary> {
        return repository.getBuyerLedger(buyerId).onEach { _currentSummary.value = it }.map { applyDetailFilter(it) }
    }

    fun exportLedgerEntry(context: Context, entry: LedgerEntry, partyType: String) {
        // Deprecated, use printLedgerEntry or shareLedgerEntry
    }

    fun printLedgerEntry(context: Context, entry: LedgerEntry, partyType: String) {
        val billNo = entry.details?.billNumber?.ifEmpty { entry.id } ?: entry.id
        viewModelScope.launch {
            try {
                _isPrinting.value = billNo
                
                val activity = context.findActivity()
                if (activity == null) {
                    _exportStatus.emit("FAILED: Unable to open print. Please try again.")
                    return@launch
                }

                val profile = companyProfile.value ?: CompanyProfileEntity()
                val file = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    generateEntryPdf(context, profile, entry, partyType)
                }

                if (file != null && file.exists()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfPrintHelper.print(activity, uri)
                    }
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Print Failed")
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isPrinting.value = null
            }
        }
    }

    fun shareLedgerEntry(
        context: Context, 
        entry: LedgerEntry, 
        partyType: String,
        shareType: ShareType = ShareType.OTHER,
        phoneNumber: String? = null
    ) {
        val billNo = entry.details?.billNumber?.ifEmpty { entry.id } ?: entry.id
        viewModelScope.launch {
            try {
                _isSharing.value = billNo
                
                val profile = companyProfile.value ?: CompanyProfileEntity()
                val file = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    generateEntryPdf(context, profile, entry, partyType)
                }

                if (file != null && file.exists()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        when (shareType) {
                            ShareType.WHATSAPP -> {
                                CommunicationUtils.shareFileToWhatsApp(context, uri, phoneNumber)
                            }
                            ShareType.PDF -> {
                                PdfActionManager.openPdf(context, uri)
                            }
                            ShareType.OTHER -> {
                                PdfActionManager.sharePdf(context, uri)
                            }
                        }
                    }
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Share Failed")
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isSharing.value = null
            }
        }
    }

    suspend fun getPartyMobileNumber(partyId: String, partyType: String): String? {
        return if (partyType == "FARMER") {
            farmerRepository.getFarmerById(partyId)?.mobileNumber
        } else {
            buyerRepository.getBuyerById(partyId)?.mobileNumber
        }
    }

    private suspend fun generateEntryPdf(context: Context, profile: CompanyProfileEntity, entry: LedgerEntry, partyType: String): java.io.File? {
        val details = entry.details
        return when (entry.transactionType) {
            TransactionType.ARRIVAL -> {
                if (details != null && details.arrivalItems.isNotEmpty()) {
                    exportService.exportArrivalToPdf(context, profile, details.arrivalItems, details.deductions)
                } else null
            }
            TransactionType.SALE -> {
                if (details != null) {
                    val sale = com.dasariravi145.agrolynch.data.local.entity.SaleEntity(
                        id = entry.id,
                        buyerName = _currentSummary.value?.partyName ?: "",
                        totalAmount = details.grossAmount,
                        totalNetAmount = details.netAmount,
                        laborCharges = details.laborCharges,
                        transportCharges = details.transportCharges,
                        billNumber = details.billNumber,
                        date = entry.date
                    )
                    exportService.exportSaleToPdf(context, profile, sale, details.saleItems, details.deductions)
                } else null
            }
            TransactionType.PAYMENT -> {
                val payment = com.dasariravi145.agrolynch.data.local.entity.PaymentEntity(
                    id = entry.id,
                    partyName = _currentSummary.value?.partyName ?: "",
                    partyType = partyType,
                    amount = entry.amount,
                    paymentMode = if(entry.title.contains(":")) entry.title.split(":")[1].trim() else "CASH",
                    referenceNumber = entry.reference,
                    billNumber = details?.billNumber ?: "",
                    date = entry.date
                )
                exportService.exportPaymentToPdf(context, profile, payment, partyType)
            }
            else -> null
        }
    }

    private fun applyDetailFilter(summary: LedgerSummary): LedgerSummary {
        val f = _filter.value
        val query = f.query.trim().lowercase(Locale.getDefault())
        if (f.startDate == null && f.endDate == null && f.transactionType == null && query.isBlank()) {
            return summary
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        val filteredEntries = summary.entries.filter { entry ->
            val dateMatch = (f.startDate == null || entry.date >= f.startDate) && 
                          (f.endDate == null || entry.date <= f.endDate)
            val typeMatch = f.transactionType == null || entry.transactionType == f.transactionType
            
            if (!(dateMatch && typeMatch)) return@filter false

            if (query.isBlank()) return@filter true

            val entryTitleLower = entry.title.lowercase(Locale.getDefault())
            val billNumberLower = entry.details?.billNumber?.lowercase(Locale.getDefault()) ?: ""
            val productNameLower = entry.details?.productName?.lowercase(Locale.getDefault()) ?: ""
            val farmerNameLower = entry.details?.farmerName?.lowercase(Locale.getDefault()) ?: ""
            val productTypeLower = entry.details?.productType?.lowercase(Locale.getDefault()) ?: ""
            val gradeLower = entry.details?.grade?.lowercase(Locale.getDefault()) ?: ""
            val transTypeLower = entry.transactionType.name.lowercase(Locale.getDefault())
            val referenceLower = entry.reference.lowercase(Locale.getDefault())

            val formattedDate = dateFormat.format(Date(entry.date)).lowercase(Locale.getDefault())
            val amountStr = entry.amount.toString()
            
            entryTitleLower.contains(query) || 
            billNumberLower.contains(query) ||
            productNameLower.contains(query) ||
            farmerNameLower.contains(query) ||
            productTypeLower.contains(query) ||
            gradeLower.contains(query) ||
            transTypeLower.contains(query) ||
            formattedDate.contains(query) ||
            amountStr.contains(query) ||
            (entry.details?.arrivalItems?.any { it.farmerName.lowercase(Locale.getDefault()).contains(query) } == true) ||
            referenceLower.contains(query)
        }
        return summary.copy(entries = filteredEntries)
    }

    fun clearFilters() {
        _filter.value = LedgerFilter()
    }

    fun deleteSale(saleId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = saleRepository.deleteSale(saleId)
                if (result is Resource.Error) {
                    _exportStatus.emit("FAILED: ${result.message}")
                }
            } catch (e: Exception) {
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Archiving Feature ---

    fun prepareArchive(summary: LedgerSummary, partyType: String) {
        if (_archiveState.value != ArchiveState.IDLE) return
        
        viewModelScope.launch(Dispatchers.Default) {
            _archiveState.value = ArchiveState.VALIDATING
            if (!Formatter.isAccountSettled(summary.totalDebit, summary.totalCredit)) {
                _archiveMessage.emit("UNSETTLED: Settle all dues before archiving.")
                _archiveState.value = ArchiveState.IDLE
                return@launch
            }
            
            _archiveState.value = ArchiveState.SNAPSHOT_CREATING
            when (val result = archiveRepository.createArchive(summary, partyType)) {
                is Resource.Success -> {
                    pendingArchiveId = result.data
                    performMandatoryBackup()
                }
                is Resource.Error -> {
                    _archiveMessage.emit("FAILED: ${result.message}")
                    _archiveState.value = ArchiveState.IDLE
                }
                else -> {}
            }
        }
    }

    private suspend fun performMandatoryBackup() {
        _archiveState.value = ArchiveState.BACKING_UP
        val premium = premiumStateManager.getCachedPremiumStatus()
        
        // 1. Mandatory Local Safety Backup
        val backupResult = backupRepository.createLocalBackup("ARCHIVE_AUTO")
        if (backupResult is Resource.Success) {
            // Local safety is enough to proceed to "Ready for Delete" phase
            _archiveState.value = ArchiveState.READY_FOR_DELETE
            
            // 2. Perform Cloud Backup in background if premium, without blocking the main workflow
            if (premium) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        backupRepository.uploadBackupToCloud(backupResult.data!!, "ARCHIVE_AUTO")
                    } catch (e: Exception) {
                        Timber.e(e, "Background cloud backup failed during archive")
                    }
                }
            }
        } else {
            _archiveMessage.emit("BACKUP_FAILED: Mandatory local backup failed. Archive aborted for safety.")
            _archiveState.value = ArchiveState.IDLE
        }
    }

    fun confirmDeletion() {
        if (_isLoading.value) return
        val aid = pendingArchiveId ?: return
        if (_archiveState.value != ArchiveState.READY_FOR_DELETE) return
        
        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            _archiveState.value = ArchiveState.DELETING
            when (val result = archiveRepository.deleteLiveHistory(aid)) {
                is Resource.Success -> {
                    _archiveMessage.emit("SUCCESS: Account history archived and deleted.")
                    _archiveState.value = ArchiveState.IDLE
                    pendingArchiveId = null
                }
                is Resource.Error -> {
                    _archiveMessage.emit("DELETE_FAILED: ${result.message}")
                    _archiveState.value = ArchiveState.READY_FOR_DELETE
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun cancelDeletion() {
        _archiveState.value = ArchiveState.IDLE
        pendingArchiveId = null
    }
}
