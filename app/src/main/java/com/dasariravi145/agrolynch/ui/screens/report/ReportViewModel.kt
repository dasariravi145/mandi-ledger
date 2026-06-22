package com.dasariravi145.agrolynch.ui.screens.report

import android.content.Context
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.dao.*
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.repository.ReportRepository
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.util.ReportExportService
import com.dasariravi145.agrolynch.util.LedgerExportService
import com.dasariravi145.agrolynch.util.findActivity
import com.dasariravi145.agrolynch.util.PdfGenerator
import com.dasariravi145.agrolynch.util.PdfPrintHelper
import com.dasariravi145.agrolynch.util.PdfActionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
    private val exportService: ReportExportService,
    private val ledgerExportService: LedgerExportService
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

    private val _isPrinting = MutableStateFlow<String?>(null) // Stores billNo or ID being printed
    val isPrinting: StateFlow<String?> = _isPrinting.asStateFlow()

    private val _isSharing = MutableStateFlow<String?>(null) // Stores billNo or ID being shared
    val isSharing: StateFlow<String?> = _isSharing.asStateFlow()

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
        // Deprecated, use shareReport or printReport
        if (format == ExportFormat.PDF) {
            shareReport(context, reportName, data)
        } else {
            // ... existing logic for EXCEL/CSV if needed ...
        }
    }

    fun shareReport(context: Context, reportName: String, data: List<Any>) {
        viewModelScope.launch {
            _showExportOptions.value = null
            if (!premiumStateManager.getCachedPremiumStatus()) {
                _exportStatus.emit("PREMIUM_REQUIRED")
                return@launch
            }
            try {
                _isLoading.value = true
                Timber.d("SHARE_REPORT_STARTED: name=$reportName, dataSize=${data.size}")
                
                val profile = companyProfile.value ?: CompanyProfileEntity()
                if (profile.companyName.isEmpty()) {
                    Timber.w("SHARE_REPORT_FAILED: Company profile is missing.")
                    _exportStatus.emit("FAILED: Company profile is missing.")
                    return@launch
                }

                val file = withContext(Dispatchers.IO) {
                    exportService.exportToPdf(context, profile, reportName, data)
                }

                if (file != null && file.exists()) {
                    Timber.d("SHARE_REPORT_SUCCESS: ${file.absolutePath}")
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfActionManager.sharePdf(context, uri)
                    }
                } else {
                    Timber.e("SHARE_REPORT_FAILED: exportToPdf returned null or file doesn't exist.")
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "SHARE_REPORT_EXCEPTION: ${e.message}")
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun printReport(context: Context, reportName: String, data: List<Any>) {
        viewModelScope.launch {
            _showExportOptions.value = null
            if (!premiumStateManager.getCachedPremiumStatus()) {
                _exportStatus.emit("PREMIUM_REQUIRED")
                return@launch
            }
            try {
                _isLoading.value = true
                Timber.d("PRINT_REPORT_STARTED: name=$reportName, dataSize=${data.size}")
                
                val activity = context.findActivity()
                if (activity == null) {
                    Timber.w("PRINT_REPORT_FAILED: Activity is null")
                    _exportStatus.emit("FAILED: Unable to open print.")
                    return@launch
                }
                val profile = companyProfile.value ?: CompanyProfileEntity()
                val file = withContext(Dispatchers.IO) {
                    exportService.exportToPdf(context, profile, reportName, data)
                }

                if (file != null && file.exists()) {
                    Timber.d("PRINT_REPORT_SUCCESS: ${file.absolutePath}")
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfPrintHelper.print(activity, uri)
                    }
                } else {
                    Timber.e("PRINT_REPORT_FAILED: exportToPdf returned null or file doesn't exist.")
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "PRINT_REPORT_EXCEPTION: ${e.message}")
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun printArrival(context: Context, items: List<DetailedArrivalReportModel>) {
        val first = items.firstOrNull() ?: return
        val billNo = first.billNumber
        
        viewModelScope.launch {
            try {
                _isPrinting.value = billNo
                
                val activity = context.findActivity()
                if (activity == null) {
                    _exportStatus.emit("FAILED: Unable to open print.")
                    return@launch
                }

                val profile = companyProfile.value ?: CompanyProfileEntity()
                
                val arrivals = items.map { item ->
                    com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity(
                        id = item.id,
                        farmerName = item.farmerName,
                        productName = item.productName,
                        grade = item.grade,
                        quantity = item.quantity,
                        unit = item.unit,
                        purchaseRate = item.rate,
                        ratePerKg = item.rate,
                        grossAmount = item.grossAmount,
                        commissionPercent = item.commissionPercent,
                        commissionAmount = item.commissionAmount,
                        laborCharges = item.laborCharges,
                        transportCharges = item.transportCharges,
                        packingCharges = item.packingCharges,
                        otherDeductions = item.otherDeductions,
                        netAmount = item.netAmount,
                        billNumber = item.billNumber,
                        finalNetWeightKg = item.finalNetWeightKg,
                        date = item.date
                    )
                }

                // Map advance as a deduction entity if it exists
                val deductions = items.filter { it.advanceAmount > 0 }.map { 
                    com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity(
                        entryId = it.id,
                        entryType = "STOCK",
                        billId = it.billNumber,
                        deductionType = "Advance",
                        amount = it.advanceAmount
                    )
                }

                val file = withContext(Dispatchers.IO) {
                    ledgerExportService.exportArrivalToPdf(context, profile, arrivals, deductions)
                }
                
                if (file != null && file.exists()) {
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfPrintHelper.print(activity, uri)
                    }
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isPrinting.value = null
            }
        }
    }

    fun shareArrival(context: Context, items: List<DetailedArrivalReportModel>) {
        val first = items.firstOrNull() ?: return
        val billNo = first.billNumber
        
        viewModelScope.launch {
            try {
                _isSharing.value = billNo
                
                val profile = companyProfile.value ?: CompanyProfileEntity()
                
                val arrivals = items.map { item ->
                    com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity(
                        id = item.id,
                        farmerName = item.farmerName,
                        productName = item.productName,
                        grade = item.grade,
                        quantity = item.quantity,
                        unit = item.unit,
                        purchaseRate = item.rate,
                        ratePerKg = item.rate,
                        grossAmount = item.grossAmount,
                        commissionPercent = item.commissionPercent,
                        commissionAmount = item.commissionAmount,
                        laborCharges = item.laborCharges,
                        transportCharges = item.transportCharges,
                        packingCharges = item.packingCharges,
                        otherDeductions = item.otherDeductions,
                        netAmount = item.netAmount,
                        billNumber = item.billNumber,
                        finalNetWeightKg = item.finalNetWeightKg,
                        date = item.date
                    )
                }

                // Map advance as a deduction entity if it exists
                val deductions = items.filter { it.advanceAmount > 0 }.map { 
                    com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity(
                        entryId = it.id,
                        entryType = "STOCK",
                        billId = it.billNumber,
                        deductionType = "Advance",
                        amount = it.advanceAmount
                    )
                }

                val file = withContext(Dispatchers.IO) {
                    ledgerExportService.exportArrivalToPdf(context, profile, arrivals, deductions)
                }
                
                if (file != null && file.exists()) {
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfActionManager.sharePdf(context, uri)
                    }
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isSharing.value = null
            }
        }
    }

    fun printSale(context: Context, items: List<DetailedSaleReportModel>) {
        val first = items.firstOrNull() ?: return
        val billNo = first.billNumber

        viewModelScope.launch {
            try {
                _isPrinting.value = billNo

                val activity = context.findActivity()
                if (activity == null) {
                    _exportStatus.emit("FAILED: Unable to open print.")
                    return@launch
                }

                val profile = companyProfile.value ?: CompanyProfileEntity()
                
                val sale = com.dasariravi145.agrolynch.data.local.entity.SaleEntity(
                    id = first.saleId,
                    buyerName = first.buyerName,
                    totalAmount = items.sumOf { it.saleAmount },
                    totalNetAmount = items.sumOf { it.totalAmount },
                    laborCharges = items.sumOf { it.laborCharges },
                    transportCharges = items.sumOf { it.transportCharges },
                    billNumber = first.billNumber,
                    date = first.date
                )
                
                val saleItems = items.map { item ->
                    com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity(
                        productName = item.productName,
                        grade = item.grade,
                        quantitySold = item.quantity,
                        inputQuantity = item.inputQuantity,
                        unit = item.unit,
                        saleRate = item.rate,
                        saleAmount = item.saleAmount
                    )
                }

                val file = withContext(Dispatchers.IO) {
                    ledgerExportService.exportSaleToPdf(context, profile, sale, saleItems, emptyList())
                }

                if (file != null && file.exists()) {
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfPrintHelper.print(activity, uri)
                    }
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isPrinting.value = null
            }
        }
    }

    fun shareSale(context: Context, items: List<DetailedSaleReportModel>) {
        val first = items.firstOrNull() ?: return
        val billNo = first.billNumber

        viewModelScope.launch {
            try {
                _isSharing.value = billNo

                val profile = companyProfile.value ?: CompanyProfileEntity()
                
                val sale = com.dasariravi145.agrolynch.data.local.entity.SaleEntity(
                    id = first.saleId,
                    buyerName = first.buyerName,
                    totalAmount = items.sumOf { it.saleAmount },
                    totalNetAmount = items.sumOf { it.totalAmount },
                    laborCharges = items.sumOf { it.laborCharges },
                    transportCharges = items.sumOf { it.transportCharges },
                    billNumber = first.billNumber,
                    date = first.date
                )
                
                val saleItems = items.map { item ->
                    com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity(
                        productName = item.productName,
                        grade = item.grade,
                        quantitySold = item.quantity,
                        inputQuantity = item.inputQuantity,
                        unit = item.unit,
                        saleRate = item.rate,
                        saleAmount = item.saleAmount
                    )
                }

                val file = withContext(Dispatchers.IO) {
                    ledgerExportService.exportSaleToPdf(context, profile, sale, saleItems, emptyList())
                }

                if (file != null && file.exists()) {
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfActionManager.sharePdf(context, uri)
                    }
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed")
                }
            } catch (e: Exception) {
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isSharing.value = null
            }
        }
    }

    val summaryTotals: StateFlow<Map<String, Double>> = filters.flatMapLatest { (start, end) ->
        Timber.d("REPORT_OVERVIEW_REFRESH_STARTED: Range $start to $end")
        combine(
            reportRepository.getTotalSales(start, end),
            reportRepository.getTotalPurchases(start, end),
            reportRepository.getTotalCommission(start, end),
            reportRepository.getBuyerPendingTotal(),
            reportRepository.getFarmerPendingTotal(),
            reportRepository.getArrivalCount(),
            reportRepository.getSaleCount(),
            reportRepository.getPaymentCount()
        ) { args ->
            val sales = args[0] as? Double ?: 0.0
            val purchases = args[1] as? Double ?: 0.0
            val comm = args[2] as? Double ?: 0.0
            val bPending = args[3] as? Double ?: 0.0
            val fPending = args[4] as? Double ?: 0.0
            val aCount = args[5] as? Int ?: 0
            val sCount = args[6] as? Int ?: 0
            val pCount = args[7] as? Int ?: 0
            
            Timber.d("REPORT_TOTAL_SALES: $sales")
            Timber.d("REPORT_COMMISSION: $comm")
            Timber.d("REPORT_BUYER_BALANCE: $bPending")
            Timber.d("REPORT_FARMER_BALANCE: $fPending")
            Timber.d("arrivalCount: $aCount")
            Timber.d("saleCount: $sCount")
            Timber.d("paymentCount: $pCount")
            
            val overview = mapOf(
                "Total Sales" to sales,
                "Purchases" to purchases,
                "Total Commission" to comm,
                "Buyer Pending" to bPending,
                "Farmer Pending" to fPending
            )
            Timber.d("Final Business Overview State: $overview")
            overview
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
