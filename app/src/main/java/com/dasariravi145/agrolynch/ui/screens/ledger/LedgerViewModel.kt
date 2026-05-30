package com.dasariravi145.agrolynch.ui.screens.ledger

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.model.LedgerSummary
import com.dasariravi145.agrolynch.domain.model.TransactionType
import com.dasariravi145.agrolynch.domain.repository.LedgerRepository
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
    private val premiumStateManager: PremiumStateManager,
    private val exportService: LedgerExportService
) : ViewModel() {

    val isPremium = premiumStateManager.isPremium

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

    fun getFarmerLedger(farmerId: String): Flow<LedgerSummary> {
        return repository.getFarmerLedger(farmerId).map { applyDetailFilter(it) }
    }

    fun getBuyerLedger(buyerId: String): Flow<LedgerSummary> {
        return repository.getBuyerLedger(buyerId).map { applyDetailFilter(it) }
    }

    fun exportLedger(context: Context, summary: LedgerSummary, partyType: String) {
        viewModelScope.launch {
            if (!premiumStateManager.getCachedPremiumStatus()) {
                _exportStatus.emit("PREMIUM_REQUIRED")
                return@launch
            }

            try {
                _isLoading.value = true
                Timber.d("PDF Button Clicked for ${summary.partyName}")
                
                Timber.d("PDF Generation Started")
                val file = exportService.exportLedgerToPdf(context, summary, partyType)

                if (file != null && file.exists()) {
                    Timber.d("PDF Generation Success: ${file.absolutePath}")
                    _exportStatus.emit("SUCCESS:${file.absolutePath}")
                } else {
                    Timber.e("PDF Generation Failed: File not created")
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "PDF Generation Failed")
                _exportStatus.emit("FAILED: ${e.message}")
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
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
