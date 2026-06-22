package com.dasariravi145.agrolynch.ui.screens.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.data.local.dao.FarmerStockInfo
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.*
import com.dasariravi145.agrolynch.util.Constants
import com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

data class SaleItemDraft(
    val id: String = UUID.randomUUID().toString(),
    val arrival: ArrivalEntity,
    val inputQuantity: Double, // Quantity in original unit (KG/Ton/Boxes)
    val saleRate: Double, // Rate per KG
    val commissionPercent: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val otherCharges: Double = 0.0,
    val rawInputQuantity: String = "" // Added to handle decimal input precisely
) {
    val quantity: Double get() {
        val calculated = when(arrival.unit) {
            "Ton" -> inputQuantity * 1000.0
            "Boxes" -> {
                val kgPerBox = if (arrival.numberOfBoxes > 0) arrival.finalNetWeightKg / arrival.numberOfBoxes else 0.0
                inputQuantity * kgPerBox
            }
            else -> inputQuantity
        }
        
        // Log Sale Item Details
        val kgPerBox = if (arrival.numberOfBoxes > 0) arrival.finalNetWeightKg / arrival.numberOfBoxes else 0.0
        val availableKg = arrival.remainingQuantity * (when(arrival.unit) {
            "Ton" -> 1000.0
            "Boxes" -> kgPerBox
            else -> 1.0
        })
        timber.log.Timber.d("SALE_QTY_RAW_INPUT: $rawInputQuantity")
        timber.log.Timber.d("SALE_QTY_PARSED_UNIT: ${arrival.unit}")
        timber.log.Timber.d("SALE_ENTERED_QUANTITY: $inputQuantity")
        timber.log.Timber.d("SALE_QTY_KG_CALCULATED: $calculated")
        timber.log.Timber.d("SALE_AVAILABLE_KG: $availableKg")
        timber.log.Timber.d("SALE_RATE_PER_KG: $saleRate")
        
        val isValid = calculated > 0 && calculated <= (availableKg + 0.001) && saleRate > 0
        timber.log.Timber.d("SALE_VALIDATION_RESULT: $isValid")
        
        return calculated
    }

    val purchaseAmount: Double get() = when(arrival.unit) {
        "Ton" -> {
            if (arrival.ratePerKg > 0) quantity * arrival.ratePerKg
            else (quantity / 1000.0) * arrival.purchaseRate
        }
        "Boxes", "KG" -> {
            if (arrival.ratePerKg > 0) quantity * arrival.ratePerKg
            else quantity * arrival.purchaseRate
        }
        else -> quantity * arrival.purchaseRate
    }
    
    val saleAmount: Double get() {
        val amount = quantity * saleRate
        timber.log.Timber.d("SALE_AMOUNT_CALCULATED: $amount")
        return amount
    }
    val commissionAmount: Double get() = 0.0
    val netAmount: Double get() = saleAmount + laborCharges + transportCharges + otherCharges
}

data class TransactionTotal(
    val totalQuantity: Double = 0.0,
    val totalPurchaseAmount: Double = 0.0,
    val totalSaleAmount: Double = 0.0,
    val totalCommission: Double = 0.0,
    val totalLabor: Double = 0.0,
    val totalTransport: Double = 0.0,
    val totalOther: Double = 0.0,
    val totalNetAmount: Double = 0.0
)

@HiltViewModel
class SaleViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val buyerRepository: BuyerRepository,
    private val farmerRepository: FarmerRepository,
    private val productRepository: ProductRepository,
    private val arrivalRepository: ArrivalRepository,
    private val companyRepository: CompanyRepository,
    private val billNumberRepository: BillNumberRepository,
    private val pdfService: TemplateInvoicePdfService,
    val adMobManager: com.dasariravi145.agrolynch.ads.AdMobManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()
    
    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _billNumber = MutableStateFlow("")
    val billNumber: StateFlow<String> = _billNumber.asStateFlow()

    private val _deductions = MutableStateFlow<List<EntryDeductionEntity>>(emptyList())
    val deductions: StateFlow<List<EntryDeductionEntity>> = _deductions.asStateFlow()

    val totalDeductions = _deductions.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        generatePreviewBillNumber()
    }

    private fun generatePreviewBillNumber() {
        viewModelScope.launch {
            _billNumber.value = billNumberRepository.generateNextBillNumber(Constants.SeriesType.SALE)
        }
    }

    fun addDeduction(type: String, amount: Double, customName: String = "") {
        val newDeduction = EntryDeductionEntity(
            entryId = "", // Will be filled on save
            entryType = Constants.EntryType.SALE,
            billId = _billNumber.value,
            deductionType = type,
            customName = customName,
            amount = amount
        )
        _deductions.value = _deductions.value + newDeduction
    }

    fun removeDeduction(index: Int) {
        val currentList = _deductions.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _deductions.value = currentList
        }
    }

    // --- Buyer Section ---
    val buyers = buyerRepository.getBuyers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Sale Items Section ---
    private val _saleItems = MutableStateFlow<List<SaleItemDraft>>(emptyList())
    val saleItems = _saleItems.asStateFlow()

    private val defaultCommission = companyRepository.getProfile()
        .map { 5.0 } // Default to 5.0% if profile doesn't have a specific field
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5.0)

    val transactionTotal = _saleItems.map { items ->
        TransactionTotal(
            totalQuantity = items.sumOf { it.quantity },
            totalPurchaseAmount = items.sumOf { it.purchaseAmount },
            totalSaleAmount = items.sumOf { it.saleAmount },
            totalCommission = items.sumOf { it.commissionAmount },
            totalLabor = items.sumOf { it.laborCharges },
            totalTransport = items.sumOf { it.transportCharges },
            totalOther = items.sumOf { it.otherCharges },
            totalNetAmount = items.sumOf { it.netAmount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionTotal())

    fun addSaleItem(item: SaleItemDraft) {
        if (item.arrival.unit == "Boxes" && item.arrival.numberOfBoxes <= 0) {
            viewModelScope.launch { _error.emit("Box weight missing. Please update stock entry.") }
            return
        }
        if (_saleItems.value.any { 
            it.arrival.id == item.arrival.id
        }) {
            viewModelScope.launch { _error.emit("This stock entry is already added.") }
            return
        }
        _saleItems.value = _saleItems.value + item
    }

    fun updateSaleItem(updatedItem: SaleItemDraft) {
        val currentList = _saleItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            currentList[index] = updatedItem
            _saleItems.value = currentList
        }
    }

    fun removeSaleItem(id: String) {
        _saleItems.value = _saleItems.value.filter { it.id != id }
    }

    // --- Farmer Stock Selection Logic ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val farmersWithStock = arrivalRepository.getFarmersWithStock()
        .flatMapLatest { farmers ->
            arrivalRepository.getArrivals().map { allArrivals ->
                val selectedStockIds = _saleItems.value.map { it.arrival.id }.toSet()
                farmers.filter { farmer ->
                    allArrivals.any { it.farmerId == farmer.farmerId && it.remainingQuantity > 0 && it.id !in selectedStockIds }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAvailableStockByFarmer(farmerId: String): Flow<List<ArrivalEntity>> = 
        arrivalRepository.getAvailableStockByFarmer(farmerId).map { arrivals ->
            val selectedStockIds = _saleItems.value.map { it.arrival.id }.toSet()
            arrivals.filter { it.id !in selectedStockIds }
        }

    fun createSale(
        context: android.content.Context,
        buyer: BuyerEntity,
        // The following parameters are kept for compatibility with the existing createSale call in SaleScreen, 
        // but the actual items are now managed in _saleItems
        unusedSaleRate: Double = 0.0,
        unusedTransport: Double = 0.0,
        unusedOther: Double = 0.0,
        unusedLabor: Double = 0.0,
        unusedPacking: Double = 0.0
    ) {
        val items = _saleItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val saleId = UUID.randomUUID().toString()
                val totals = transactionTotal.value
                val currentBillNumber = _billNumber.value
                val currentDeductionsTotal = totalDeductions.value
                val currentDeductionList = _deductions.value
                
                val saleItemEntities = items.map { draft ->
                    SaleItemEntity(
                        id = UUID.randomUUID().toString(),
                        saleId = saleId,
                        arrivalId = draft.arrival.id,
                        farmerId = draft.arrival.farmerId,
                        farmerName = draft.arrival.farmerName,
                        productId = draft.arrival.productId,
                        productName = draft.arrival.productName,
                        productCategory = draft.arrival.productCategory,
                        grade = draft.arrival.grade,
                        quantitySold = draft.quantity,
                        inputQuantity = draft.inputQuantity,
                        unit = draft.arrival.unit,
                        purchaseRate = draft.arrival.purchaseRate,
                        saleRate = draft.saleRate,
                        purchaseAmount = draft.purchaseAmount,
                        saleAmount = draft.saleAmount,
                        marginAmount = draft.saleAmount - draft.purchaseAmount,
                        commissionPercent = 0.0,
                        commissionAmount = 0.0,
                        laborCharges = draft.laborCharges,
                        transportCharges = draft.transportCharges,
                        otherCharges = draft.otherCharges,
                        netAmount = draft.netAmount,
                        date = System.currentTimeMillis()
                    )
                }

                Timber.d("COMMISSION_DEBUG: Total Earnings for Sale: ${totals.totalCommission}")

                val saleEntity = SaleEntity(
                    id = saleId,
                    buyerId = buyer.id,
                    buyerName = buyer.name,
                    farmerName = items.map { it.arrival.farmerName }.distinct().joinToString(", "),
                    productId = if (items.distinctBy { it.arrival.productId }.size > 1) "Multiple" else items.first().arrival.productId,
                    productName = if (items.distinctBy { it.arrival.productId }.size > 1) "Multiple Products" else items.first().arrival.productName,
                    grade = if (items.distinctBy { it.arrival.grade }.size > 1) "Multiple" else items.first().arrival.grade,
                    totalQuantity = totals.totalQuantity,
                    totalPurchaseAmount = totals.totalPurchaseAmount,
                    totalAmount = totals.totalSaleAmount, // Sub-total
                    totalCommission = totals.totalCommission,
                    laborCharges = totals.totalLabor,
                    transportCharges = totals.totalTransport,
                    packingCharges = 0.0,
                    otherCharges = totals.totalOther + currentDeductionsTotal,
                    billNumber = currentBillNumber,
                    totalNetAmount = totals.totalNetAmount + currentDeductionsTotal, // Final Collection
                    totalMargin = totals.totalSaleAmount - totals.totalPurchaseAmount,
                    pendingAmount = totals.totalNetAmount + currentDeductionsTotal,
                    date = System.currentTimeMillis()
                )

                when (val result = saleRepository.createSale(saleEntity, saleItemEntities)) {
                    is Resource.Success -> {
                        // Save deductions
                        val deductionsToSave = currentDeductionList.map { 
                            it.copy(entryId = saleId, billId = currentBillNumber) 
                        }
                        billNumberRepository.saveDeductions(deductionsToSave)
                        
                        // Finalize bill number
                        billNumberRepository.incrementBillNumber(Constants.SeriesType.SALE)

                        _saveSuccess.emit(Unit)
                        _saleItems.value = emptyList()
                    }
                    is Resource.Error -> _error.emit(result.message ?: "Failed to save sale")
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "CREATE_SALE_FAILED")
                _error.emit("An error occurred while saving: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerBuyerAndCreateSale(
        context: android.content.Context,
        name: String,
        mobile: String,
        address: String,
        gst: String,
        unusedSaleRate: Double = 0.0,
        unusedTransport: Double = 0.0,
        unusedOther: Double = 0.0,
        unusedLabor: Double = 0.0,
        unusedPacking: Double = 0.0
    ) {
        if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            viewModelScope.launch {
                _error.emit("Please enter a valid 10-digit mobile number")
            }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val newBuyer = BuyerEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                mobileNumber = mobile,
                address = address,
                gstNumber = gst,
                lastUpdated = System.currentTimeMillis()
            )
            val result = buyerRepository.addBuyer(newBuyer)
            if (result is Resource.Error) {
                _error.emit(result.message ?: "Failed to add buyer")
                _isLoading.value = false
            } else {
                createSale(context = context, buyer = newBuyer)
            }
        }
    }
}
