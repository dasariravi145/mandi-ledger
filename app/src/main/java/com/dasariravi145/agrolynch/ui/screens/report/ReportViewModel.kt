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

enum class ReportCategory {
    OVERALL_BUSINESS, FARMER, BUYER, PRODUCT, SALES, ARRIVAL, PAYMENT, EXPENSE, PENDING, COMMISSION
}

enum class ReportPeriodType {
    TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, CUSTOM_DATE, CUSTOM_MONTH
}

data class DateRange(
    val startDate: Long,
    val endDate: Long,
    val displayLabel: String
)

data class ReportState(
    val selectedCategory: ReportCategory = ReportCategory.OVERALL_BUSINESS,
    val periodType: ReportPeriodType = ReportPeriodType.THIS_MONTH,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val rangeLabel: String = "This Month",
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedQuarter: Int = (Calendar.getInstance().get(Calendar.MONTH) / 3) + 1, // 1-4
    val selectedHalfYear: Int = if (Calendar.getInstance().get(Calendar.MONTH) < 6) 1 else 2, // 1-2
    val fromDate: Long = System.currentTimeMillis(),
    val toDate: Long = System.currentTimeMillis(),
    val fromMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val fromYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val toMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val toYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val searchQuery: String = "",
    val selectedProduct: String? = null,
    val selectedCategoryFilter: String? = null
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

    private val _state = MutableStateFlow(ReportState())
    init {
        updateReportRange()
    }
    val state: StateFlow<ReportState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPrinting = MutableStateFlow<String?>(null)
    val isPrinting: StateFlow<String?> = _isPrinting.asStateFlow()

    private val _isSharing = MutableStateFlow<String?>(null)
    val isSharing: StateFlow<String?> = _isSharing.asStateFlow()

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _showExportOptions = MutableStateFlow<List<Any>?>(null)
    val showExportOptions: StateFlow<List<Any>?> = _showExportOptions.asStateFlow()

    val filters = _state.map { it.startDate to it.endDate }.distinctUntilChanged()

    fun onCategorySelected(category: ReportCategory) {
        _state.value = _state.value.copy(selectedCategory = category, searchQuery = "")
    }

    fun onPeriodTypeSelected(type: ReportPeriodType) {
        _state.value = _state.value.copy(periodType = type, searchQuery = "")
        updateReportRange()
    }

    fun setPeriodType(type: ReportPeriodType) {
        onPeriodTypeSelected(type)
    }

    fun updateSelection(
        month: Int? = null,
        year: Int? = null,
        quarter: Int? = null,
        halfYear: Int? = null,
        fromDate: Long? = null,
        toDate: Long? = null,
        fromMonth: Int? = null,
        fromYear: Int? = null,
        toMonth: Int? = null,
        toYear: Int? = null
    ) {
        _state.value = _state.value.copy(
            selectedMonth = month ?: _state.value.selectedMonth,
            selectedYear = year ?: _state.value.selectedYear,
            selectedQuarter = quarter ?: _state.value.selectedQuarter,
            selectedHalfYear = halfYear ?: _state.value.selectedHalfYear,
            fromDate = fromDate ?: _state.value.fromDate,
            toDate = toDate ?: _state.value.toDate,
            fromMonth = fromMonth ?: _state.value.fromMonth,
            fromYear = fromYear ?: _state.value.fromYear,
            toMonth = toMonth ?: _state.value.toMonth,
            toYear = toYear ?: _state.value.toYear
        )
        updateReportRange()
    }

    fun updateCustomDateRange(start: Long, end: Long) {
        updateSelection(fromDate = start, toDate = end)
    }

    private fun updateReportRange() {
        val s = _state.value
        val range = resolveBusinessReportDateRange(
            s.periodType,
            s.selectedMonth,
            s.selectedQuarter,
            s.selectedHalfYear,
            s.selectedYear,
            s.fromDate,
            s.toDate,
            s.fromMonth,
            s.fromYear,
            s.toMonth,
            s.toYear
        )
        _state.value = s.copy(
            startDate = range.startDate,
            endDate = range.endDate,
            rangeLabel = range.displayLabel
        )
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun onExportClick(data: List<Any>) {
        _showExportOptions.value = data
    }

    fun dismissExportOptions() {
        _showExportOptions.value = null
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
                val range = _state.value.rangeLabel
                val profile = companyProfile.value ?: CompanyProfileEntity()
                val finalReportName = "${reportName.replace("_", " ")} - $range"
                val file = withContext(Dispatchers.IO) {
                    exportService.exportToPdf(context, profile, finalReportName, data)
                }
                if (file != null && file.exists()) {
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfActionManager.sharePdf(context, uri)
                    }
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed. Please check logs.")
                }
            } catch (e: Exception) {
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
                val range = _state.value.rangeLabel
                val activity = context.findActivity()
                if (activity == null) {
                    _exportStatus.emit("FAILED: Unable to open print.")
                    return@launch
                }
                val profile = companyProfile.value ?: CompanyProfileEntity()
                val finalReportName = "${reportName.replace("_", " ")} - $range"
                val file = withContext(Dispatchers.IO) {
                    exportService.exportToPdf(context, profile, finalReportName, data)
                }
                if (file != null && file.exists()) {
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfPrintHelper.print(activity, uri)
                    }
                } else {
                    _exportStatus.emit("FAILED: PDF generation failed. Please check logs.")
                }
            } catch (e: Exception) {
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
                val activity = context.findActivity() ?: return@launch
                val profile = companyProfile.value ?: CompanyProfileEntity()
                val arrivals = items.map { item ->
                    com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity(
                        id = item.id, farmerName = item.farmerName, productName = item.productName, grade = item.grade,
                        quantity = item.quantity, unit = item.unit, purchaseRate = item.rate, ratePerKg = item.rate,
                        grossAmount = item.grossAmount, commissionPercent = item.commissionPercent, commissionAmount = item.commissionAmount,
                        laborCharges = item.laborCharges, transportCharges = item.transportCharges, packingCharges = item.packingCharges,
                        otherDeductions = item.otherDeductions, netAmount = item.netAmount, billNumber = item.billNumber,
                        finalNetWeightKg = item.finalNetWeightKg, date = item.date
                    )
                }
                val deductions = items.filter { it.advanceAmount > 0 }.map { 
                    com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity(
                        entryId = it.id, entryType = "STOCK", billId = it.billNumber, deductionType = "Advance", amount = it.advanceAmount
                    )
                }
                val file = withContext(Dispatchers.IO) { ledgerExportService.exportArrivalToPdf(context, profile, arrivals, deductions) }
                if (file != null && file.exists()) { withContext(Dispatchers.Main) { val uri = PdfGenerator.getUriFromFile(context, file); PdfPrintHelper.print(activity, uri) } }
            } finally { _isPrinting.value = null }
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
                        id = item.id, farmerName = item.farmerName, productName = item.productName, grade = item.grade,
                        quantity = item.quantity, unit = item.unit, purchaseRate = item.rate, ratePerKg = item.rate,
                        grossAmount = item.grossAmount, commissionPercent = item.commissionPercent, commissionAmount = item.commissionAmount,
                        laborCharges = item.laborCharges, transportCharges = item.transportCharges, packingCharges = item.packingCharges,
                        otherDeductions = item.otherDeductions, netAmount = item.netAmount, billNumber = item.billNumber,
                        finalNetWeightKg = item.finalNetWeightKg, date = item.date
                    )
                }
                val deductions = items.filter { it.advanceAmount > 0 }.map { 
                    com.dasariravi145.agrolynch.data.local.entity.EntryDeductionEntity(
                        entryId = it.id, entryType = "STOCK", billId = it.billNumber, deductionType = "Advance", amount = it.advanceAmount
                    )
                }
                val file = withContext(Dispatchers.IO) { ledgerExportService.exportArrivalToPdf(context, profile, arrivals, deductions) }
                if (file != null && file.exists()) { withContext(Dispatchers.Main) { val uri = PdfGenerator.getUriFromFile(context, file); PdfActionManager.sharePdf(context, uri) } }
            } finally { _isSharing.value = null }
        }
    }

    fun printSale(context: Context, items: List<DetailedSaleReportModel>) {
        val first = items.firstOrNull() ?: return
        val billNo = first.billNumber
        viewModelScope.launch {
            try {
                _isPrinting.value = billNo
                val activity = context.findActivity() ?: return@launch
                val profile = companyProfile.value ?: CompanyProfileEntity()
                val sale = com.dasariravi145.agrolynch.data.local.entity.SaleEntity(
                    id = first.saleId, buyerName = first.buyerName, totalAmount = items.sumOf { it.saleAmount },
                    totalNetAmount = items.sumOf { it.totalAmount }, laborCharges = items.sumOf { it.laborCharges },
                    transportCharges = items.sumOf { it.transportCharges }, billNumber = first.billNumber, date = first.date
                )
                val saleItems = items.map { item ->
                    com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity(
                        productName = item.productName, grade = item.grade, quantitySold = item.quantity, inputQuantity = item.inputQuantity,
                        unit = item.unit, saleRate = item.rate, saleAmount = item.saleAmount
                    )
                }
                val file = withContext(Dispatchers.IO) { ledgerExportService.exportSaleToPdf(context, profile, sale, saleItems, emptyList()) }
                if (file != null && file.exists()) { withContext(Dispatchers.Main) { val uri = PdfGenerator.getUriFromFile(context, file); PdfPrintHelper.print(activity, uri) } }
            } finally { _isPrinting.value = null }
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
                    id = first.saleId, buyerName = first.buyerName, totalAmount = items.sumOf { it.saleAmount },
                    totalNetAmount = items.sumOf { it.totalAmount }, laborCharges = items.sumOf { it.laborCharges },
                    transportCharges = items.sumOf { it.transportCharges }, billNumber = first.billNumber, date = first.date
                )
                val saleItems = items.map { item ->
                    com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity(
                        productName = item.productName, grade = item.grade, quantitySold = item.quantity, inputQuantity = item.inputQuantity,
                        unit = item.unit, saleRate = item.rate, saleAmount = item.saleAmount
                    )
                }
                val file = withContext(Dispatchers.IO) { ledgerExportService.exportSaleToPdf(context, profile, sale, saleItems, emptyList()) }
                if (file != null && file.exists()) { withContext(Dispatchers.Main) { val uri = PdfGenerator.getUriFromFile(context, file); PdfActionManager.sharePdf(context, uri) } }
            } finally { _isSharing.value = null }
        }
    }

    fun exportReport(context: Context, format: ExportFormat, reportName: String, data: List<Any>) {
        if (format == ExportFormat.PDF) shareReport(context, reportName, data)
    }

    val summaryTotals: StateFlow<Map<String, Double>> = filters.flatMapLatest { (start, end) ->
        combine(
            reportRepository.getTotalSales(start, end),
            reportRepository.getTotalPurchases(start, end),
            reportRepository.getTotalCommission(start, end),
            reportRepository.getTotalExpenses(start, end),
            reportRepository.getFarmerPayments(start, end),
            reportRepository.getBuyerCollections(start, end),
            reportRepository.getFarmerPendingTotal(),
            reportRepository.getBuyerPendingTotal()
        ) { args ->
            val sales = args[0] ?: 0.0
            val purchases = args[1] ?: 0.0
            val commission = args[2] ?: 0.0
            val expenses = args[3] ?: 0.0
            val farmerPayments = args[4] ?: 0.0
            val buyerCollections = args[5] ?: 0.0
            val farmerPending = args[6] ?: 0.0
            val buyerPending = args[7] ?: 0.0
            mapOf(
                "Total Sales" to sales, "Total Arrivals" to purchases, "Total Commission" to commission, "Total Expenses" to expenses,
                "Farmer Payments" to farmerPayments, "Buyer Collections" to buyerCollections, "Farmer Pending" to farmerPending,
                "Buyer Pending" to buyerPending, "Net Profit/Loss" to (sales - purchases + commission - expenses)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val buyerDetailedReport: StateFlow<List<DetailedSaleReportModel>> = filters.flatMapLatest { (start, end) -> reportRepository.getBuyerDetailedReport(start, end) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val farmerDetailedReport: StateFlow<List<DetailedArrivalReportModel>> = filters.flatMapLatest { (start, end) -> reportRepository.getFarmerDetailedReport(start, end) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stockReport: StateFlow<List<StockReportModel>> = reportRepository.getStockReport().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val productPerformanceReport: StateFlow<List<ProductPerformanceModel>> = reportRepository.getProductPerformanceReport().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val outstandingAgingReport: StateFlow<List<OutstandingAgingModel>> = reportRepository.getOutstandingAgingReport().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentReport: StateFlow<List<PaymentReportModel>> = filters.flatMapLatest { (start, end) -> reportRepository.getPaymentReport(start, end) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenseReport: StateFlow<List<com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity>> = filters.flatMapLatest { (start, end) -> reportRepository.getExpenseReport(start, end) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val commissionReport: StateFlow<List<CommissionReportModel>> = filters.flatMapLatest { (start, end) -> reportRepository.getCommissionReport(start, end) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val salesTrend: StateFlow<List<ChartDataModel>> = reportRepository.getSalesTrend(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolveBusinessReportDateRange(
        periodType: ReportPeriodType, selectedMonth: Int?, selectedQuarter: Int?, selectedHalfYear: Int?, selectedYear: Int?,
        fromDate: Long?, toDate: Long?, fromMonth: Int?, fromYear: Int?, toMonth: Int?, toYear: Int?
    ): DateRange {
        val cal = Calendar.getInstance()
        val year = selectedYear ?: cal.get(Calendar.YEAR)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val monthSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        return when (periodType) {
            ReportPeriodType.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                DateRange(start, cal.timeInMillis, "Today")
            }
            ReportPeriodType.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                DateRange(start, cal.timeInMillis, "Yesterday")
            }
            ReportPeriodType.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.timeInMillis = System.currentTimeMillis()
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                DateRange(start, cal.timeInMillis, "This Week")
            }
            ReportPeriodType.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                DateRange(start, cal.timeInMillis, "This Month")
            }
            ReportPeriodType.MONTHLY -> {
                val month = (selectedMonth ?: (cal.get(Calendar.MONTH) + 1)) - 1
                cal.set(year, month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                DateRange(start, cal.timeInMillis, monthSdf.format(Date(start)))
            }
            ReportPeriodType.QUARTERLY -> {
                val q = selectedQuarter ?: 1
                cal.set(year, (q - 1) * 3, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 2)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                DateRange(start, cal.timeInMillis, "Q$q $year")
            }
            ReportPeriodType.HALF_YEARLY -> {
                val h = selectedHalfYear ?: 1
                cal.set(year, if (h == 1) 0 else 6, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 5)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                DateRange(start, cal.timeInMillis, "H$h $year")
            }
            ReportPeriodType.YEARLY -> {
                cal.set(year, 0, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(year, 11, 31, 23, 59, 59); cal.set(Calendar.MILLISECOND, 999)
                DateRange(start, cal.timeInMillis, "Year $year")
            }
            ReportPeriodType.CUSTOM_DATE -> {
                val start = fromDate ?: System.currentTimeMillis()
                val end = toDate ?: System.currentTimeMillis()
                DateRange(start, end, "${sdf.format(Date(start))} to ${sdf.format(Date(end))}")
            }
            ReportPeriodType.CUSTOM_MONTH -> {
                val fM = (fromMonth ?: 1) - 1; val fY = fromYear ?: cal.get(Calendar.YEAR)
                val tM = (toMonth ?: 1) - 1; val tY = toYear ?: cal.get(Calendar.YEAR)
                cal.set(fY, fM, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(tY, tM, 1, 23, 59, 59)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                DateRange(start, cal.timeInMillis, "${monthSdf.format(Date(start))} to ${monthSdf.format(Date(cal.timeInMillis))}")
            }
        }
    }
}
