package com.dasariravi145.agrolynch.ui.screens.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.ArrivalEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import com.dasariravi145.agrolynch.domain.repository.ArrivalRepository
import com.dasariravi145.agrolynch.domain.repository.FarmerRepository
import com.dasariravi145.agrolynch.domain.repository.ProductRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ArrivalViewModel @Inject constructor(
    private val arrivalRepository: ArrivalRepository,
    private val farmerRepository: FarmerRepository,
    private val productRepository: ProductRepository,
    val adMobManager: com.dasariravi145.agrolynch.ads.AdMobManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // 1. Farmer selection state
    val farmers = farmerRepository.getFarmers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Product search logic
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery = _productSearchQuery.asStateFlow()

    private val _matchedProduct = MutableStateFlow<ProductEntity?>(null)
    val matchedProduct = _matchedProduct.asStateFlow()

    private val _isNewProduct = MutableStateFlow(false)
    val isNewProduct = _isNewProduct.asStateFlow()

    // Cache to avoid repeated DB hits for the same name
    private val productCache = mutableMapOf<String, ProductEntity?>()

    init {
        setupProductSearch()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupProductSearch() {
        _productSearchQuery
            .debounce(300L) // Wait for user to stop typing
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) {
                    _matchedProduct.value = null
                    _isNewProduct.value = false
                    return@onEach
                }

                // Check cache first
                val cached = productCache[query.lowercase()]
                if (cached != null) {
                    _matchedProduct.value = cached
                    _isNewProduct.value = false
                    return@onEach
                }

                _isLoading.value = true
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val product = productRepository.getProductByName(query)
                        
                        if (product != null) {
                            productCache[query.lowercase()] = product
                            _matchedProduct.value = product
                            _isNewProduct.value = false
                        } else {
                            _matchedProduct.value = null
                            _isNewProduct.value = true
                        }
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error searching product")
                    } finally {
                        _isLoading.value = false
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onProductQueryChange(query: String) {
        _productSearchQuery.value = query
    }

    fun saveArrivalBatch(
        farmerName: String,
        farmerPhone: String,
        farmerVillage: String,
        productName: String,
        productCategory: String,
        unit: String,
        commissionPercent: Double,
        laborCharges: Double,
        transportCharges: Double,
        packingCharges: Double,
        otherDeductions: Double,
        gradeEntries: List<GradeEntry>
    ) {
        // Validations
        if (gradeEntries.any { it.quantity <= 0 }) {
            viewModelScope.launch { _error.emit("Quantity must be greater than 0") }
            return
        }
        if (gradeEntries.any { it.spoilage < 0 }) {
            viewModelScope.launch { _error.emit("Spoilage cannot be negative") }
            return
        }
        if (gradeEntries.any { it.rate <= 0 }) {
            viewModelScope.launch { _error.emit("Rate must be greater than 0") }
            return
        }
        if (gradeEntries.any { it.spoilage >= it.quantity }) {
            viewModelScope.launch { _error.emit("Spoilage cannot be greater than or equal to total quantity") }
            return
        }

        // Phone validation for new farmer
        val existingFarmer = farmers.value.find { 
            it.name.equals(farmerName, ignoreCase = true) && 
            (farmerPhone.isEmpty() || it.mobileNumber == farmerPhone)
        }
        if (existingFarmer == null && farmerPhone.isNotBlank() && farmerPhone.length != 10) {
            viewModelScope.launch { _error.emit("Please enter a valid 10-digit mobile number") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Handle Farmer
            val selectedFarmer = farmers.value.find { 
                it.name.equals(farmerName, ignoreCase = true) && 
                (farmerPhone.isEmpty() || it.mobileNumber == farmerPhone)
            }
            
            val farmerId = if (selectedFarmer == null) {
                val newId = UUID.randomUUID().toString()
                val newFarmer = FarmerEntity(
                    id = newId,
                    name = farmerName,
                    mobileNumber = farmerPhone,
                    village = farmerVillage,
                    pendingAmount = 0.0,
                    advanceAmount = 0.0
                )
                farmerRepository.addFarmer(newFarmer)
                newId
            } else {
                selectedFarmer.id
            }

            // 2. Handle Product
            val selectedProduct = productRepository.getProductByName(productName)
            val productId = if (selectedProduct == null) {
                val newId = UUID.randomUUID().toString()
                val newProduct = ProductEntity(
                    id = newId,
                    name = productName,
                    category = productCategory,
                    availableGrades = gradeEntries.map { it.grade }.distinct()
                )
                productRepository.addProduct(newProduct, null)
                newId
            } else {
                val existingGrades = selectedProduct.availableGrades.toMutableSet()
                val newGrades = gradeEntries.map { it.grade }
                if (existingGrades.addAll(newGrades)) {
                    productRepository.addProduct(selectedProduct.copy(availableGrades = existingGrades.toList()), null)
                }
                selectedProduct.id
            }

            // 3. Save Arrivals (One per Grade)
            val arrivals = gradeEntries.mapIndexed { index, entry ->
                val itemGrossAmount = entry.grossAmount
                val itemCommissionAmount = (itemGrossAmount * commissionPercent) / 100
                
                // For simplified accounting, we can split charges or put them on the first entry.
                // The prompt says "Farmer Ledger must display: Product, Grade, Quantity, Rate, Amount".
                // This implies each grade is a row.
                // Net Amount = Total Gross - Charges. 
                // To keep it simple, we can deduct charges from the total or distribute them.
                // However, if we save them as separate arrivals, the "Net Amount" per arrival might be misleading if we don't distribute charges.
                // But the prompt says "Total Gross Amount - Commission Amount - Charges".
                
                val itemLabor = if (index == 0) laborCharges else 0.0
                val itemTransport = if (index == 0) transportCharges else 0.0
                val itemPacking = if (index == 0) packingCharges else 0.0
                val itemOther = if (index == 0) otherDeductions else 0.0
                
                val itemNetAmount = itemGrossAmount - itemCommissionAmount - itemLabor - itemTransport - itemPacking - itemOther

                ArrivalEntity(
                    id = UUID.randomUUID().toString(),
                    farmerId = farmerId,
                    farmerName = farmerName,
                    productId = productId,
                    productName = productName,
                    productCategory = productCategory,
                    grade = entry.grade,
                    quantity = entry.quantity,
                    unit = unit,
                    boxCount = entry.boxCount,
                    tareWeight = entry.tareWeight,
                    spoilageQuantity = entry.spoilage,
                    netQuantity = entry.netQuantity,
                    remainingQuantity = entry.netQuantity,
                    purchaseRate = entry.rate,
                    grossAmount = itemGrossAmount,
                    commissionPercent = commissionPercent,
                    commissionAmount = itemCommissionAmount,
                    laborCharges = itemLabor,
                    transportCharges = itemTransport,
                    packingCharges = itemPacking,
                    otherDeductions = itemOther,
                    netAmount = itemNetAmount,
                    date = System.currentTimeMillis()
                )
            }

            when (val result = arrivalRepository.addArrivalBatch(arrivals)) {
                is Resource.Success -> _saveSuccess.emit(Unit)
                is Resource.Error -> _error.emit(result.message ?: "Failed to save arrival batch")
                else -> {}
            }

            _isLoading.value = false
        }
    }

    data class GradeEntry(
        val grade: String,
        val quantity: Double,
        val spoilage: Double,
        val rate: Double = 0.0,
        val boxCount: Int = 0,
        val tareWeight: Double = 0.0
    ) {
        val netQuantity: Double 
            get() = (quantity - spoilage - (boxCount * tareWeight)).coerceAtLeast(0.0)
            
        val grossAmount: Double
            get() = netQuantity * rate
    }
}
