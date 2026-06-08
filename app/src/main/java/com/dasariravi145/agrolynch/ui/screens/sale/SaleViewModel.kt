package com.dasariravi145.agrolynch.ui.screens.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.data.local.dao.FarmerStockInfo
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

data class SaleItemDraft(
    val id: String = UUID.randomUUID().toString(),
    val arrival: ArrivalEntity,
    val quantity: Double,
    val saleRate: Double,
    val commissionPercent: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val otherCharges: Double = 0.0
) {
    val purchaseAmount: Double get() = quantity * arrival.purchaseRate
    val saleAmount: Double get() = quantity * saleRate
    val commissionAmount: Double get() = (saleAmount * commissionPercent) / 100
    val netAmount: Double get() = saleAmount - laborCharges - transportCharges - otherCharges
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
    val adMobManager: com.dasariravi145.agrolynch.ads.AdMobManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

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
        _saleItems.value = _saleItems.value + item
    }

    fun removeSaleItem(id: String) {
        _saleItems.value = _saleItems.value.filter { it.id != id }
    }

    // --- Farmer Stock Selection Logic ---
    val farmersWithStock = arrivalRepository.getFarmersWithStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAvailableStockByFarmer(farmerId: String): Flow<List<ArrivalEntity>> = 
        arrivalRepository.getAvailableStockByFarmer(farmerId)

    fun createSale(
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
            
            val saleId = UUID.randomUUID().toString()
            val totals = transactionTotal.value
            
            val saleItemEntities = items.map { draft ->
                val calcComm = (draft.saleAmount * draft.commissionPercent) / 100
                Timber.d("COMMISSION_DEBUG: SaleAmt: ${draft.saleAmount}, CommPercent: ${draft.commissionPercent}%, CalcComm: $calcComm")
                
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
                    unit = draft.arrival.unit,
                    purchaseRate = draft.arrival.purchaseRate,
                    saleRate = draft.saleRate,
                    purchaseAmount = draft.purchaseAmount,
                    saleAmount = draft.saleAmount,
                    marginAmount = draft.saleAmount - draft.purchaseAmount,
                    commissionPercent = draft.commissionPercent,
                    commissionAmount = draft.commissionAmount,
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
                otherCharges = totals.totalOther,
                totalNetAmount = totals.totalNetAmount, // Final Collection
                totalMargin = totals.totalSaleAmount - totals.totalPurchaseAmount,
                pendingAmount = totals.totalNetAmount,
                date = System.currentTimeMillis()
            )

            when (val result = saleRepository.createSale(saleEntity, saleItemEntities)) {
                is Resource.Success -> {
                    _saveSuccess.emit(Unit)
                    _saleItems.value = emptyList()
                }
                is Resource.Error -> _error.emit(result.message ?: "Failed to save sale")
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun registerBuyerAndCreateSale(
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
                createSale(buyer = newBuyer)
            }
        }
    }
}
