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
    private val productRepository: ProductRepository
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

    fun saveArrival(
        farmerName: String,
        farmerPhone: String,
        farmerVillage: String,
        productName: String,
        productCategory: String,
        grade: String,
        unit: String,
        quantity: Double,
        rate: Double,
        grossAmount: Double,
        commissionPercent: Double,
        commissionAmount: Double,
        netAmount: Double
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Handle Farmer (Find existing or create new)
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
                    availableGrades = listOf(grade)
                )
                productRepository.addProduct(newProduct, null)
                newId
            } else {
                // If it's an existing product, we might want to add the grade if it's new to this product
                if (!selectedProduct.availableGrades.contains(grade)) {
                    val updatedGrades = selectedProduct.availableGrades + grade
                    productRepository.addProduct(selectedProduct.copy(availableGrades = updatedGrades), null)
                }
                selectedProduct.id
            }

            // 3. Save Arrival
            val arrival = ArrivalEntity(
                id = UUID.randomUUID().toString(),
                farmerId = farmerId,
                farmerName = farmerName,
                productId = productId,
                productName = productName,
                productCategory = productCategory,
                grade = grade,
                quantity = quantity,
                remainingQuantity = quantity,
                unit = unit,
                purchaseRate = rate,
                grossAmount = grossAmount,
                commissionPercent = commissionPercent,
                commissionAmount = commissionAmount,
                netAmount = netAmount,
                date = System.currentTimeMillis()
            )

            when (val result = arrivalRepository.addArrival(arrival)) {
                is Resource.Success -> _saveSuccess.emit(Unit)
                is Resource.Error -> _error.emit(result.message ?: "Failed to save arrival")
                else -> {}
            }
            _isLoading.value = false
        }
    }
}
