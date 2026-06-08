package com.dasariravi145.agrolynch.ui.screens.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.repository.ReportRepository
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.ReportExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
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
    val datePreset: DatePreset = DatePreset.THIS_MONTH,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val searchQuery: String = "",
    val selectedProduct: String? = null,
    val selectedCategory: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val companyRepository: CompanyRepository,
    private val syncRepository: SyncRepository,
    private val premiumStateManager: PremiumStateManager,
    private val exportService: ReportExportService
) : ViewModel() {

    init {
        viewModelScope.launch {
            reportRepository.recalculateCommissions()
        }
    }

    val isPremium = premiumStateManager.isPremium
    private val companyProfile = companyRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow(ReportState(
        startDate = getStartOfMonth(System.currentTimeMillis()),
        endDate = getEndOfDay(System.currentTimeMillis())
    ))
    val state: StateFlow<ReportState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _showExportOptions = MutableStateFlow<List<Any>?>(null)
    val showExportOptions: StateFlow<List<Any>?> = _showExportOptions.asStateFlow()

    val filters = _state.map { it.startDate to it.endDate }.distinctUntilChanged()

    fun onExportClick(data: List<Any>) {
        _showExportOptions.value = data
    }

    fun dismissExportOptions() {
        _showExportOptions.value = null
    }

    fun exportReport(context: Context, format: ExportFormat, reportName: String, data: List<Any>) {
        if (data.isEmpty()) {
            viewModelScope.launch {
                _exportStatus.emit("FAILED: No report data available for selected period.")
            }
            return
        }

        viewModelScope.launch {
            _showExportOptions.value = null
            
            if (!premiumStateManager.getCachedPremiumStatus()) {
                _exportStatus.emit("PREMIUM_REQUIRED")
                return@launch
            }

            try {
                _isLoading.value = true
                val profile = companyProfile.value ?: CompanyProfileEntity()
                if (profile.companyName.isEmpty()) {
                    _exportStatus.emit("FAILED: Company profile is missing. Please set it up in Settings.")
                    _isLoading.value = false
                    return@launch
                }

                Timber.d("REPORT_EXPORT: Starting export for $reportName. Format: $format. Data count: ${data.size}")

                val file = when (format) {
                    ExportFormat.PDF -> exportService.exportToPdf(context, profile, reportName, data)
                    ExportFormat.EXCEL -> exportService.exportToExcel(context, reportName, data)
                    ExportFormat.CSV -> exportService.exportToCsv(context, reportName, data)
                }

                if (file != null && file.exists() && file.length() > 0) {
                    Timber.d("REPORT_EXPORT: Export successful. File path: ${file.absolutePath}. Size: ${file.length()} bytes")
                    
                    // Background cloud upload for premium users
                    if (format == ExportFormat.PDF) {
                        viewModelScope.launch {
                            try {
                                syncRepository.uploadFile(file, "reports/$reportName")
                            } catch (e: Exception) {
                                Timber.e(e, "Cloud backup failed for report")
                                // We don't fail the local export if cloud upload fails
                            }
                        }
                    }
                    _exportStatus.emit("SUCCESS:${file.absolutePath}")
                } else {
                    val reason = if (file == null) "PDF generation returned null" 
                                else if (!file.exists()) "File was not created"
                                else "File is empty (0 bytes)"
                    _exportStatus.emit("FAILED: Export failed: $reason")
                }
            } catch (e: Exception) {
                Timber.e(e, "Export failed")
                _exportStatus.emit("FAILED: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    val summaryTotals: StateFlow<Map<String, Double>> = filters.flatMapLatest { (start, end) ->
        combine(
            reportRepository.getTotalSales(start, end),
            reportRepository.getTotalPurchases(start, end),
            reportRepository.getTotalCommission(start, end),
            reportRepository.getBuyerPendingTotal(),
            reportRepository.getFarmerPendingTotal()
        ) { sales, purchases, comm, bPending, fPending ->
            mapOf(
                "Sales" to (sales ?: 0.0),
                "Purchases" to (purchases ?: 0.0),
                "Commission" to (comm ?: 0.0),
                "BuyerPending" to (bPending ?: 0.0),
                "FarmerPending" to (fPending ?: 0.0)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val buyerDetailedReport: StateFlow<List<DetailedSaleReportModel>> = filters.flatMapLatest { (start, end) ->
        reportRepository.getBuyerDetailedReport(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val farmerDetailedReport: StateFlow<List<DetailedArrivalReportModel>> = filters.flatMapLatest { (start, end) ->
        reportRepository.getFarmerDetailedReport(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockReport: StateFlow<List<StockReportModel>> = reportRepository.getStockReport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productPerformanceReport: StateFlow<List<ProductPerformanceModel>> = reportRepository.getProductPerformanceReport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outstandingAgingReport: StateFlow<List<OutstandingAgingModel>> = reportRepository.getOutstandingAgingReport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentReport: StateFlow<List<PaymentReportModel>> = filters.flatMapLatest { (start, end) ->
        reportRepository.getPaymentReport(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commissionReport: StateFlow<List<CommissionReportModel>> = filters.flatMapLatest { (start, end) ->
        reportRepository.getCommissionReport(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesTrend: StateFlow<List<ChartDataModel>> = reportRepository.getSalesTrend(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
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
        _state.value = _state.value.copy(datePreset = preset, startDate = start, endDate = end)
    }

    fun updateCustomDateRange(start: Long, end: Long) {
        _state.value = _state.value.copy(datePreset = DatePreset.CUSTOM, startDate = start, endDate = end)
    }

    fun formatDate(time: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(time))

    private fun getStartOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getEndOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    private fun getStartOfWeek(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    private fun getStartOfMonth(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis
}
