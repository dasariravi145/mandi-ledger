package com.dasariravi145.agrolynch.ui.screens.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class SaleItemDraft(
    val arrival: ArrivalEntity,
    val quantitySold: Double,
    val purchaseRate: Double
) {
    val purchaseAmount = quantitySold * purchaseRate
}

@HiltViewModel
class SaleViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val buyerRepository: BuyerRepository,
    private val productRepository: ProductRepository,
    private val arrivalRepository: ArrivalRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    val buyers = buyerRepository.getBuyers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products = productRepository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    val selectedProduct: StateFlow<ProductEntity?> = _selectedProduct.asStateFlow()

    private val _selectedGrade = MutableStateFlow<String?>(null)
    val selectedGrade: StateFlow<String?> = _selectedGrade.asStateFlow()

    val availableGrades: StateFlow<List<String>> = _selectedProduct.map { product ->
        product?.availableGrades ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableStocks = combine(_selectedProduct, _selectedGrade) { product, grade ->
        if (product != null && grade != null) {
            arrivalRepository.getAvailableStockByProductAndGrade(product.id, grade)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of Arrival ID to Quantity
    private val _selectedQuantities = MutableStateFlow<Map<String, Double>>(emptyMap())
    val selectedQuantities: StateFlow<Map<String, Double>> = _selectedQuantities.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    fun selectProduct(product: ProductEntity) {
        _selectedProduct.value = product
        _selectedGrade.value = null
        _selectedQuantities.value = emptyMap()
    }

    fun selectGrade(grade: String) {
        _selectedGrade.value = grade
        _selectedQuantities.value = emptyMap()
    }

    fun updateQuantity(arrivalId: String, quantity: Double) {
        val current = _selectedQuantities.value.toMutableMap()
        val arrival = availableStocks.value.find { it.id == arrivalId }
        
        if (arrival == null) return
        
        val validatedQty = if (quantity > arrival.remainingQuantity) {
            arrival.remainingQuantity
        } else if (quantity <= 0) {
            0.0
        } else {
            quantity
        }

        if (validatedQty <= 0) {
            current.remove(arrivalId)
        } else {
            current[arrivalId] = validatedQty
        }
        _selectedQuantities.value = current
    }

    private val _buyerSaveSuccess = MutableSharedFlow<BuyerEntity>()
    val buyerSaveSuccess = _buyerSaveSuccess.asSharedFlow()

    fun registerBuyerAndCreateSale(
        name: String,
        mobile: String,
        address: String,
        gst: String,
        saleRate: Double,
        transportCharges: Double = 0.0,
        otherCharges: Double = 0.0
    ) {
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
                createSale(
                    buyer = newBuyer, 
                    saleRate = saleRate,
                    transportCharges = transportCharges,
                    otherCharges = otherCharges
                )
            }
        }
    }

    fun createSale(
        buyer: BuyerEntity,
        saleRate: Double,
        transportCharges: Double = 0.0,
        otherCharges: Double = 0.0
    ) {
        val selections = _selectedQuantities.value
        if (selections.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            
            val saleId = UUID.randomUUID().toString()
            val stocks = availableStocks.value
            
            var totalPurchaseAmt = 0.0
            var totalQty = 0.0
            
            val saleItems = selections.map { (arrivalId, qty) ->
                val arrival = stocks.find { it.id == arrivalId } ?: throw Exception("Stock not found")
                
                val itemPurchaseAmt = qty * arrival.purchaseRate
                val itemSaleAmt = qty * saleRate
                val itemMargin = itemSaleAmt - itemPurchaseAmt
                
                totalPurchaseAmt += itemPurchaseAmt
                totalQty += qty
                
                SaleItemEntity(
                    id = UUID.randomUUID().toString(),
                    saleId = saleId,
                    arrivalId = arrivalId,
                    farmerId = arrival.farmerId,
                    farmerName = arrival.farmerName,
                    productId = arrival.productId,
                    productName = arrival.productName,
                    quantitySold = qty,
                    unit = arrival.unit,
                    purchaseRate = arrival.purchaseRate,
                    saleRate = saleRate,
                    purchaseAmount = itemPurchaseAmt,
                    saleAmount = itemSaleAmt,
                    marginAmount = itemMargin,
                    date = System.currentTimeMillis()
                )
            }

            val totalSaleAmt = totalQty * saleRate
            val totalMargin = totalSaleAmt - totalPurchaseAmt

            val sale = SaleEntity(
                id = saleId,
                buyerId = buyer.id,
                buyerName = buyer.name,
                productId = _selectedProduct.value?.id ?: "",
                productName = _selectedProduct.value?.name ?: "",
                grade = _selectedGrade.value ?: "General",
                totalQuantity = totalQty,
                totalPurchaseAmount = totalPurchaseAmt,
                totalAmount = totalSaleAmt,
                totalMargin = totalMargin,
                transportCharges = transportCharges,
                otherCharges = otherCharges,
                pendingAmount = totalSaleAmt + transportCharges + otherCharges,
                date = System.currentTimeMillis()
            )
            
            val result = saleRepository.createSale(sale, saleItems)
            if (result is Resource.Error) {
                _error.emit(result.message ?: "Failed to save sale")
            } else {
                _saveSuccess.emit(Unit)
                _selectedQuantities.value = emptyMap()
                _selectedProduct.value = null
                _selectedGrade.value = null
            }
            _isLoading.value = false
        }
    }
}
