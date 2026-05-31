package com.dasariravi145.agrolynch.ui.screens.report

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.domain.repository.ReportRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.ReportExportService
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class ExportFormat {
    PDF, EXCEL, CSV
}

enum class DatePreset {
    TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, CUSTOM
}

data class ReportState(
    val datePreset: DatePreset = DatePreset.TODAY,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val searchQuery: String = "",
    val selectedProduct: String? = null,
    val selectedCategory: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val premiumStateManager: PremiumStateManager,
    private val exportService: ReportExportService
) : ViewModel() {

    val isPremium = premiumStateManager.isPremium

    private val _state = MutableStateFlow(ReportState(
        startDate = getStartOfDay(System.currentTimeMillis()),
        endDate = getEndOfDay(System.currentTimeMillis())
    ))
    val state = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _showExportOptions = MutableStateFlow<List<Any>?>(null)
    val showExportOptions = _showExportOptions.asStateFlow()

    private val filters = _state.map { s -> Pair(s.startDate, s.endDate) }.distinctUntilChanged()

    fun onExportClick(data: List<Any>) {
        _showExportOptions.value = data
    }

    fun dismissExportOptions() {
        _showExportOptions.value = null
    }

    fun exportReport(context: Context, format: ExportFormat, reportName: String, data: List<Any>) {
        viewModelScope.launch {
            if (!premiumStateManager.getCachedPremiumStatus()) {
                _exportStatus.emit("PREMIUM_REQUIRED")
                return@launch
            }

            try {
                _isLoading.value = true
                Timber.d("Report Download Clicked: $reportName as $format")
                
                val file = when (format) {
                    ExportFormat.PDF -> exportService.exportToPdf(context, reportName, data)
                    ExportFormat.EXCEL -> exportService.exportToExcel(context, reportName, data)
                    ExportFormat.CSV -> exportService.exportToCsv(context, reportName, data)
                }

                if (file != null && file.exists()) {
                    Timber.d("Report Export Success: ${file.absolutePath}")
                    
                    // Cloud Upload for Premium
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val remotePath = "reports/$uid/${file.name}"
                        val uploadResult = exportService.uploadToCloud(file, remotePath)
                        if (uploadResult is Resource.Success) {
                            Timber.d("Cloud Upload Success: ${uploadResult.data}")
                        }
                    }
                    
                    _exportStatus.emit("SUCCESS:${file.absolutePath}")
                } else {
                    _exportStatus.emit("FAILED: File generation failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Report Export Failed")
                _exportStatus.emit("FAILED: ${e.message}")
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            } finally {
                _isLoading.value = false
                _showExportOptions.value = null
            }
        }
    }

    val summaryTotals = filters.flatMapLatest { (start, end) ->
        combine(
            reportRepository.getTotalSales(start, end),
            reportRepository.getTotalPurchases(start, end),
            reportRepository.getTotalCommission(start, end),
            reportRepository.getBuyerPendingTotal(),
            reportRepository.getFarmerPendingTotal()
        ) { sales, purchases, commission, bPending, fPending ->
            mapOf(
                "Total Sales" to (sales ?: 0.0),
                "Total Purchases" to (purchases ?: 0.0),
                "Total Commission" to (commission ?: 0.0),
                "Buyer Pending" to (bPending ?: 0.0),
                "Farmer Pending" to (fPending ?: 0.0),
                "Net Profit" to (commission ?: 0.0)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val buyerDetailedReport = filters.flatMapLatest { (start, end) ->
        reportRepository.getBuyerDetailedReport(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val farmerDetailedReport = filters.flatMapLatest { (start, end) ->
        reportRepository.getFarmerDetailedReport(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockReport = reportRepository.getStockReport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productPerformanceReport = reportRepository.getProductPerformanceReport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outstandingAgingReport = reportRepository.getOutstandingAgingReport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentReport = filters.flatMapLatest { (start, end) ->
        reportRepository.getPaymentReport(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commissionReport: StateFlow<List<CommissionReportModel>> = filters.flatMapLatest { (start, end) ->
        reportRepository.getCommissionReport(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesTrend = reportRepository.getSalesTrend(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setDatePreset(preset: DatePreset) {
        val now = System.currentTimeMillis()
        val (start, end) = when (preset) {
            DatePreset.TODAY -> getStartOfDay(now) to getEndOfDay(now)
            DatePreset.YESTERDAY -> getStartOfDay(now - 86400000) to getEndOfDay(now - 86400000)
            DatePreset.THIS_WEEK -> getStartOfWeek(now) to getEndOfDay(now)
            DatePreset.THIS_MONTH -> getStartOfMonth(now) to getEndOfDay(now)
            DatePreset.CUSTOM -> _state.value.startDate to _state.value.endDate
        }
        _state.update { it.copy(datePreset = preset, startDate = start, endDate = end) }
    }

    fun updateCustomDateRange(start: Long, end: Long) {
        _state.update { it.copy(datePreset = DatePreset.CUSTOM, startDate = start, endDate = end) }
    }

    private fun formatDate(time: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(time))

    private fun getStartOfDay(time: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(time: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun getStartOfWeek(time: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return getStartOfDay(cal.timeInMillis)
    }

    private fun getStartOfMonth(time: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(cal.timeInMillis)
    }
}
