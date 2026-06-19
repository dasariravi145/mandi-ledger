package com.dasariravi145.agrolynch.ui.screens.ledger

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.model.*
import com.dasariravi145.agrolynch.domain.repository.LedgerRepository
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.util.LedgerExportService
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LedgerFilter(
    val query: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val product: String = "",
    val transactionType: TransactionType? = null
)

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val companyRepository: CompanyRepository,
    private val premiumStateManager: PremiumStateManager,
    private val exportService: LedgerExportService
) : ViewModel() {

    val isPremium = premiumStateManager.isPremium
    val companyProfile = companyRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

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

    private val _currentSummary = MutableStateFlow<LedgerSummary?>(null)

    fun getFarmerLedger(farmerId: String): Flow<LedgerSummary> {
        return repository.getFarmerLedger(farmerId).onEach { _currentSummary.value = it }.map { applyDetailFilter(it) }
    }

    fun getBuyerLedger(buyerId: String): Flow<LedgerSummary> {
        return repository.getBuyerLedger(buyerId).onEach { _currentSummary.value = it }.map { applyDetailFilter(it) }
    }

    fun exportLedgerEntry(context: Context, entry: LedgerEntry, partyType: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val profile = companyProfile.value ?: CompanyProfileEntity()
                val details = entry.details
                
                val file = when (entry.transactionType) {
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

                if (file != null && file.exists()) {
                    _exportStatus.emit("SUCCESS:${file.absolutePath}")
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "PDF Generation Failed")
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyDetailFilter(summary: LedgerSummary): LedgerSummary {
        val f = _filter.value
        val filteredEntries = summary.entries.filter { entry ->
            val dateMatch = (f.startDate == null || entry.date >= f.startDate) && 
                          (f.endDate == null || entry.date <= f.endDate)
            val typeMatch = f.transactionType == null || entry.transactionType == f.transactionType
            val productMatch = f.product.isBlank() || entry.details?.productName?.contains(f.product, ignoreCase = true) == true
            val queryMatch = f.query.isBlank() || 
                           entry.title.contains(f.query, ignoreCase = true) || 
                           entry.details?.billNumber?.contains(f.query, ignoreCase = true) == true
            
            dateMatch && typeMatch && productMatch && queryMatch
        }
        return summary.copy(entries = filteredEntries)
    }

    fun clearFilters() {
        _filter.value = LedgerFilter()
    }
}
