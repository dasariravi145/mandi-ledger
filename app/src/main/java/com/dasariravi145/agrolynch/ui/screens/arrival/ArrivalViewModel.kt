package com.dasariravi145.agrolynch.ui.screens.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.*
import com.dasariravi145.agrolynch.util.PhoneNumberUtils
import com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ArrivalViewModel @Inject constructor(
    private val arrivalRepository: ArrivalRepository,
    private val farmerRepository: FarmerRepository,
    private val productRepository: ProductRepository,
    private val billNumberRepository: BillNumberRepository,
    private val companyRepository: CompanyRepository,
    private val pdfService: TemplateInvoicePdfService,
    val adMobManager: com.dasariravi145.agrolynch.ads.AdMobManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()
    
    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _billNumber = MutableStateFlow("")
    val billNumber: StateFlow<String> = _billNumber.asStateFlow()

    private val _deductions = MutableStateFlow<List<EntryDeductionEntity>>(emptyList())
    val deductions: StateFlow<List<EntryDeductionEntity>> = _deductions.asStateFlow()

    val totalDeductions = _deductions.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val farmers = farmerRepository.getFarmers()
        .map { list -> list.distinctBy { it.id } } // Ensure ID uniqueness in dropdown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery = _productSearchQuery.asStateFlow()

    private val _productTypeSearchQuery = MutableStateFlow("")
    val productTypeSearchQuery = _productTypeSearchQuery.asStateFlow()

    private val _matchedProduct = MutableStateFlow<ProductEntity?>(null)
    val matchedProduct = _matchedProduct.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val productTypes = _matchedProduct.flatMapLatest { product ->
        if (product != null) {
            productRepository.getProductTypes(product.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isNewProduct = MutableStateFlow(false)
    val isNewProduct = _isNewProduct.asStateFlow()

    private val productCache = mutableMapOf<String, ProductEntity?>()

    init {
        setupProductSearch()
        generatePreviewBillNumber()
    }

    private fun generatePreviewBillNumber() {
        viewModelScope.launch {
            _billNumber.value = billNumberRepository.generateNextBillNumber(Constants.SeriesType.STOCK)
        }
    }

    fun addDeduction(type: String, amount: Double, customName: String = "") {
        val newDeduction = EntryDeductionEntity(
            entryId = "", // Will be filled on save
            entryType = Constants.EntryType.STOCK,
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

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupProductSearch() {
        _productSearchQuery
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) {
                    _matchedProduct.value = null
                    _isNewProduct.value = false
                    return@onEach
                }
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
                    } finally {
                        _isLoading.value = false
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onProductQueryChange(query: String) {
        _productSearchQuery.value = query
        _productTypeSearchQuery.value = "" // Clear variety when product changes
    }

    fun onProductTypeQueryChange(query: String) {
        _productTypeSearchQuery.value = query
    }

    fun saveArrivalBatch(
        context: android.content.Context,
        farmerName: String,
        farmerPhone: String,
        farmerVillage: String,
        productName: String,
        productType: String,
        productCategory: String,
        commissionPercent: Double,
        laborCharges: Double,
        transportCharges: Double,
        packingCharges: Double,
        otherDeductionsUnused: Double, // Kept for compatibility
        billNumberUnused: String = "", // Kept for compatibility
        gradeEntries: List<GradeEntry>
    ) {
        if (_isLoading.value) return // Single-submission protection

        val currentDeductionsTotal = totalDeductions.value
        val currentBillNumber = _billNumber.value

        Timber.d("SAVE_ARRIVAL: Starting validation for Batch. Bill=$currentBillNumber, Farmer=$farmerName, Grades=${gradeEntries.size}")

        gradeEntries.forEachIndexed { index, entry ->
            Timber.d("Grade[$index]: Name=${entry.grade}, Unit=${entry.unit}, Qty=${entry.quantity}, Rate=${entry.rate}, Spoilage=${entry.spoilage}")
            
            if (entry.quantity <= 0) {
                val label = if (entry.unit == "Boxes") "Total Weight (KG)" else "Total Quantity (${entry.unit})"
                viewModelScope.launch { _error.emit("$label must be greater than 0 for ${entry.grade}") }
                Timber.w("SAVE_ARRIVAL_VALIDATION_FAILED: Quantity <= 0 for ${entry.grade}")
                return
            }
            if (entry.rate <= 0) {
                viewModelScope.launch { _error.emit("Rate must be greater than 0 for ${entry.grade}") }
                Timber.w("SAVE_ARRIVAL_VALIDATION_FAILED: Rate <= 0 for ${entry.grade}")
                return
            }

            if (entry.unit == "Boxes") {
                if (entry.boxCount <= 0) {
                    viewModelScope.launch { _error.emit("Number of Boxes must be greater than 0 for ${entry.grade}") }
                    Timber.w("SAVE_ARRIVAL_VALIDATION_FAILED: Box count <= 0 for ${entry.grade}")
                    return
                }
                if (entry.totalTareWeightKg >= (entry.quantity * 1000)) {
                    viewModelScope.launch { _error.emit("Empty Box Weight cannot exceed Total Weight for ${entry.grade}") }
                    Timber.w("SAVE_ARRIVAL_VALIDATION_FAILED: Tare weight exceeds gross weight for ${entry.grade}")
                    return
                }
                if (entry.spoilage < 0 || entry.spoilage >= 100) {
                    viewModelScope.launch { _error.emit("Spoilage percentage must be between 0 and 99 for ${entry.grade}") }
                    Timber.w("SAVE_ARRIVAL_VALIDATION_FAILED: Invalid spoilage for ${entry.grade}")
                    return
                }
            }
            if (entry.unit == "KG" || entry.unit == "Ton") {
                if (entry.spoilage < 0 || entry.spoilage > 100) {
                    viewModelScope.launch { _error.emit("Waste percentage cannot be more than 100 for ${entry.grade}") }
                    Timber.w("SAVE_ARRIVAL_VALIDATION_FAILED: Invalid waste percentage for ${entry.grade}")
                    return
                }
            }
            
            if (entry.totalNetWeightKg <= 0) {
                viewModelScope.launch { _error.emit("Net weight must be greater than 0 for ${entry.grade}. Check weight and spoilage.") }
                Timber.w("SAVE_ARRIVAL_VALIDATION_FAILED: Net weight <= 0 for ${entry.grade}")
                return
            }
        }

        val normalizedPhoneInput = if (farmerPhone.isNotBlank()) PhoneNumberUtils.normalize(farmerPhone) else ""
        val existingFarmer = farmers.value.find { 
            it.name.equals(farmerName, ignoreCase = true) && 
            (normalizedPhoneInput.isEmpty() || PhoneNumberUtils.normalize(it.mobileNumber) == normalizedPhoneInput)
        }
        if (existingFarmer == null && farmerPhone.isNotBlank() && farmerPhone.length != 10) {
            viewModelScope.launch { _error.emit("Please enter a valid 10-digit mobile number") }
            Timber.w("SAVE_ARRIVAL_VALIDATION_FAILED: Invalid mobile number")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                Timber.d("SAVE_ARRIVAL_EXECUTION: Starting database operations")
                val farmerId = if (existingFarmer == null) {
                    val newId = UUID.randomUUID().toString()
                    val newFarmer = FarmerEntity(id = newId, name = farmerName, mobileNumber = farmerPhone, village = farmerVillage)
                    farmerRepository.addFarmer(newFarmer)
                    Timber.d("SAVE_ARRIVAL_EXECUTION: Created new farmer ID=$newId")
                    newId
                } else {
                    existingFarmer.id
                }

                val selectedProduct = productRepository.getProductByName(productName)
                val productId = if (selectedProduct == null) {
                    val newId = UUID.randomUUID().toString()
                    val newProduct = ProductEntity(id = newId, name = productName, category = productCategory, availableGrades = gradeEntries.map { it.grade }.distinct())
                    productRepository.addProduct(newProduct, null)
                    Timber.d("SAVE_ARRIVAL_EXECUTION: Created new product ID=$newId")
                    newId
                } else {
                    val existingGrades = selectedProduct.availableGrades.toMutableSet()
                    if (existingGrades.addAll(gradeEntries.map { it.grade })) {
                        productRepository.addProduct(selectedProduct.copy(availableGrades = existingGrades.toList()), null)
                    }
                    selectedProduct.id
                }

                // Save product type if new
                if (productType.isNotBlank()) {
                    val existingType = productRepository.getProductTypeByName(productId, productType)
                    if (existingType == null) {
                        productRepository.addProductType(
                            ProductTypeEntity(
                                id = UUID.randomUUID().toString(),
                                productId = productId,
                                productName = productName,
                                productTypeName = productType
                            )
                        )
                        Timber.d("SAVE_ARRIVAL_EXECUTION: Added new product type $productType")
                    }
                }

            if (currentBillNumber.isBlank() || currentBillNumber == "N/A") {
                throw IllegalStateException("billNumber must be generated before ledger insert")
            }

            val arrivals = gradeEntries.mapIndexed { index, entry ->
                val arrivalId = UUID.randomUUID().toString()
                val itemGrossAmount = entry.grossAmount
                val itemCommissionAmount = (itemGrossAmount * commissionPercent) / 100
                
                // Fixed charges should only be applied once to the entire batch total.
                // We store the full charges on the first item and 0 on others to avoid double-counting in reports.
                val itemLabor = if (index == 0) laborCharges else 0.0
                val itemTransport = if (index == 0) transportCharges else 0.0
                val itemPacking = if (index == 0) packingCharges else 0.0
                val itemOtherDeductions = if (index == 0) currentDeductionsTotal else 0.0
                
                val itemNetAmount = itemGrossAmount - itemCommissionAmount - itemLabor - itemTransport - itemPacking - itemOtherDeductions

                ArrivalEntity(
                    id = arrivalId,
                    farmerId = farmerId,
                    farmerName = farmerName,
                    productId = productId,
                    productName = productName,
                    productType = productType,
                    productCategory = productCategory,
                    grade = entry.grade,
                    quantity = entry.quantity, // Still store Ton value in quantity
                    unit = entry.unit,
                    boxCount = entry.totalBoxes,
                    tareWeight = entry.totalTareWeightKg,
                    spoilageQuantity = entry.calculatedTotalSpoilageKg,
                    netQuantity = entry.netQuantity,
                    remainingQuantity = entry.netQuantity,
                    purchaseRate = entry.rate,
                    grossAmount = itemGrossAmount,
                    commissionPercent = commissionPercent,
                    commissionAmount = itemCommissionAmount,
                    laborCharges = itemLabor,
                    transportCharges = itemTransport,
                    packingCharges = itemPacking,
                    otherDeductions = itemOtherDeductions,
                    netAmount = itemNetAmount,
                    billNumber = currentBillNumber,
                    totalKg = entry.totalGrossWeightKg,
                    spoilagePerTon = if (entry.unit == "Ton") entry.spoilage else 0.0,
                    totalSpoilageKg = entry.calculatedTotalSpoilageKg,
                    otherCharges = itemPacking + itemOtherDeductions,
                    netPayable = itemNetAmount,
                    boxWeightMode = "AVERAGE",
                    numberOfBoxes = entry.boxCount,
                    totalWeightTon = if(entry.unit == "Boxes" || entry.unit == "Ton") entry.quantity else 0.0,
                    emptyBoxWeightPerBox = if(entry.unit == "Boxes") entry.avgGrossWeight else 0.0,
                    totalEmptyBoxWeightKg = if(entry.unit == "Boxes") entry.totalTareWeightKg else 0.0,
                    spoilagePercentage = if (entry.unit == "Boxes" || entry.unit == "KG") entry.spoilage else 0.0,
                    spoilageKg = entry.calculatedTotalSpoilageKg,
                    grossWeightKg = entry.totalGrossWeightKg,
                    weightAfterEmptyBoxesKg = entry.balanceKgBeforeSpoilage,
                    finalNetWeightKg = entry.totalNetWeightKg,
                    ratePerKg = entry.rate,
                    date = System.currentTimeMillis()
                )
            }

                Timber.d("SAVE_ARRIVAL_EXECUTION: Attempting batch insert of ${arrivals.size} arrivals")
                when (val result = arrivalRepository.addArrivalBatch(arrivals)) {
                    is Resource.Success -> {
                        Timber.d("SAVE_ARRIVAL_EXECUTION: Batch insert successful")
                        // Save deductions
                        val currentDeductions = _deductions.value
                        if (arrivals.isNotEmpty()) {
                            val mainArrivalId = arrivals.first().id
                            val deductionsToSave = currentDeductions.map { 
                                it.copy(entryId = mainArrivalId, billId = currentBillNumber) 
                            }
                            billNumberRepository.saveDeductions(deductionsToSave)
                        }
                        
                        // Finalize bill number
                        billNumberRepository.incrementBillNumber(Constants.SeriesType.STOCK)
                        _saveSuccess.emit(Unit)
                    }
                    is Resource.Error -> {
                        Timber.e("SAVE_ARRIVAL_EXECUTION: Repository error: ${result.message}")
                        _error.emit(result.message ?: "Failed to save arrival batch")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "SAVE_ARRIVAL_EXECUTION_FAILED")
                _error.emit("An error occurred while saving: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    data class BoxWeightDraft(val grossWeight: Double = 0.0, val tareWeight: Double = 0.0, val spoilage: Double = 0.0)

    data class GradeEntry(
        val grade: String,
        val quantity: Double = 0.0, // Total Weight (Ton) for internal storage
        val boxCount: Int = 0, // Number of Boxes
        val avgGrossWeight: Double = 0.0, // Empty Box Weight Per Box (KG)
        val rate: Double = 0.0, // Rate per KG
        val spoilage: Double = 0.0, // Spoilage % for Boxes, KG for Ton/KG
        val tareWeight: Double = 0.0, // Not used in latest logic but kept for KG compatibility
        val unit: String = "KG"
    ) {
        val totalBoxes: Int get() = if (unit == "Boxes") boxCount else 0
        val totalGrossWeightKg: Double get() = when(unit) {
            "Ton", "Boxes" -> quantity * 1000
            else -> quantity
        }
        val totalTareWeightKg: Double get() = if (unit == "Boxes") boxCount * avgGrossWeight else boxCount * tareWeight
        val balanceKgBeforeSpoilage: Double get() = (totalGrossWeightKg - totalTareWeightKg).coerceAtLeast(0.0)
        val calculatedTotalSpoilageKg: Double get() = when(unit) {
            "Ton" -> totalGrossWeightKg * spoilage / 100
            "Boxes" -> balanceKgBeforeSpoilage * spoilage / 100
            "KG" -> quantity * spoilage / 100
            else -> spoilage
        }
        val totalNetWeightKg: Double get() = (balanceKgBeforeSpoilage - calculatedTotalSpoilageKg).coerceAtLeast(0.0)
        val netQuantity: Double get() = when (unit) {
            "Ton", "Boxes" -> totalNetWeightKg / 1000
            else -> totalNetWeightKg
        }
        val grossAmount: Double get() = totalNetWeightKg * rate
    }
}
