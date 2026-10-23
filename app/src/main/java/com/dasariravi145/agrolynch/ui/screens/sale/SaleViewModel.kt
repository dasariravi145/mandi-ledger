package com.dasariravi145.agrolynch.ui.screens.sale

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.*
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.Constants
import com.dasariravi145.agrolynch.util.PhoneNumberUtils
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SaleViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val buyerRepository: BuyerRepository,
    private val farmerRepository: FarmerRepository,
    private val productRepository: ProductRepository,
    private val arrivalRepository: ArrivalRepository,
    private val companyRepository: CompanyRepository,
    private val billNumberRepository: BillNumberRepository,
    val adMobManager: com.dasariravi145.agrolynch.ads.AdMobManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess: SharedFlow<Unit> = _saveSuccess.asSharedFlow()

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus: SharedFlow<String> = _exportStatus.asSharedFlow()

    private val _billNumber = MutableStateFlow("")
    val billNumber: StateFlow<String> = _billNumber.asStateFlow()

    // Sale Quantity State
    private val _saleQuantityInput = MutableStateFlow("")
    val saleQuantityInput: StateFlow<String> = _saleQuantityInput.asStateFlow()

    private val _saleQuantityUnit = MutableStateFlow("Quintal")
    val saleQuantityUnit: StateFlow<String> = _saleQuantityUnit.asStateFlow()

    private val _showGrossTareMode = MutableStateFlow(false)
    val showGrossTareMode: StateFlow<Boolean> = _showGrossTareMode.asStateFlow()

    private val _buyerGrossWeightInput = MutableStateFlow("")
    val buyerGrossWeightInput: StateFlow<String> = _buyerGrossWeightInput.asStateFlow()

    private val _buyerTareWeightInput = MutableStateFlow("")
    val buyerTareWeightInput: StateFlow<String> = _buyerTareWeightInput.asStateFlow()

    private val _overallLaborPercent = MutableStateFlow("")
    val overallLaborPercent: StateFlow<String> = _overallLaborPercent.asStateFlow()

    private val _overallTransport = MutableStateFlow("")
    val overallTransport: StateFlow<String> = _overallTransport.asStateFlow()

    private val _deductions = MutableStateFlow<List<EntryDeductionEntity>>(emptyList())
    val deductions: StateFlow<List<EntryDeductionEntity>> = _deductions.asStateFlow()

    val totalDeductions = _deductions.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Master Data
    val buyers = buyerRepository.getBuyers()
        .map { list -> list.distinctBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val farmers = farmerRepository.getFarmers()
        .map { list -> list.distinctBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts = productRepository.getProducts()
        .map { list -> list.distinctBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Produce Groups State
    private val _produceGroups = MutableStateFlow<List<ProduceGroupDraft>>(emptyList())
    val produceGroups: StateFlow<List<ProduceGroupDraft>> = _produceGroups.asStateFlow()

    val buyerSaleTotal = combine(
        _produceGroups,
        _saleQuantityInput,
        _saleQuantityUnit,
        _showGrossTareMode,
        _buyerGrossWeightInput,
        _buyerTareWeightInput,
        _overallLaborPercent,
        _overallTransport,
        totalDeductions
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val groups = flows[0] as List<ProduceGroupDraft>
        val qtyInputStr = flows[1] as String
        val unitStr = flows[2] as String
        val isGrossTareMode = flows[3] as Boolean
        val grossInputStr = flows[4] as String
        val tareInputStr = flows[5] as String
        val laborPercentStr = flows[6] as String
        val transportStr = flows[7] as String
        val otherDeductionsTotal = flows[8] as Double

        val factor = getUnitConversionFactor(unitStr)

        val (rawSaleQty, buyerGrossKg, buyerNetKg) = if (isGrossTareMode) {
            val grossVal = grossInputStr.toDoubleOrNull() ?: 0.0
            val tareVal = tareInputStr.toDoubleOrNull() ?: 0.0
            val netVal = (grossVal - tareVal).coerceAtLeast(0.0)
            Triple(netVal, grossVal * factor, netVal * factor)
        } else {
            val qtyVal = qtyInputStr.toDoubleOrNull() ?: 0.0
            Triple(qtyVal, qtyVal * factor, qtyVal * factor)
        }

        val farmerGrossKg = groups.sumOf { it.totalGrossWeightKg }
        val farmerNetKg = groups.sumOf { it.totalNetWeightKg }

        val diff = buyerNetKg - farmerNetKg

        val matchStatus = when {
            buyerNetKg <= 0.0 -> WeightMatchStatus.NO_WEIGHT_ENTERED
            Math.abs(diff) < 0.001 -> WeightMatchStatus.MATCHED
            diff >= 0.001 -> WeightMatchStatus.UNDER_ALLOCATED
            else -> WeightMatchStatus.OVER_ALLOCATED
        }

        val farmerPurchaseTotal = groups.sumOf { it.totalFarmerPurchaseAmount }
        val buyerSubtotal = groups.sumOf { it.totalBuyerSaleAmount }

        val laborPercent = laborPercentStr.toDoubleOrNull() ?: 0.0
        val laborEquivalentKg = buyerNetKg * (laborPercent / 100.0)
        val laborCharges = buyerSubtotal * (laborPercent / 100.0)

        val transport = transportStr.toDoubleOrNull() ?: 0.0
        val finalCollection = buyerSubtotal + laborCharges + transport + otherDeductionsTotal

        val canSave = matchStatus == WeightMatchStatus.MATCHED &&
                buyerNetKg > 0.0 &&
                groups.isNotEmpty() &&
                groups.any { it.farmers.isNotEmpty() } &&
                finalCollection > 0.0

        BuyerSaleTransactionTotal(
            saleQuantityInput = rawSaleQty,
            saleQuantityUnit = unitStr,
            buyerGrossWeightKg = buyerGrossKg,
            buyerNetWeightKg = buyerNetKg,
            totalFarmerGrossWeightKg = farmerGrossKg,
            totalFarmerNetWeightKg = farmerNetKg,
            weightDifferenceKg = diff,
            weightMatchStatus = matchStatus,
            totalFarmerPurchaseAmount = farmerPurchaseTotal,
            totalBuyerSaleSubtotal = buyerSubtotal,
            laborPercentage = laborPercent,
            laborEquivalentKg = laborEquivalentKg,
            totalLaborCharges = laborCharges,
            totalTransportCharges = transport,
            totalOtherDeductions = otherDeductionsTotal,
            totalFinalCollection = finalCollection,
            isSaveEnabled = canSave
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BuyerSaleTransactionTotal())

    init {
        generatePreviewBillNumber()
    }

    private fun generatePreviewBillNumber() {
        viewModelScope.launch {
            _billNumber.value = billNumberRepository.generateNextBillNumber(Constants.SeriesType.SALE)
        }
    }

    fun onSaleQuantityChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d+(\\.\\d*)?$"))) {
            _saleQuantityInput.value = value
        }
    }

    fun onSaleQuantityUnitChange(unit: String) {
        _saleQuantityUnit.value = unit
    }

    fun toggleGrossTareMode(enabled: Boolean) {
        _showGrossTareMode.value = enabled
    }

    fun onBuyerGrossWeightChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d+(\\.\\d*)?$"))) {
            _buyerGrossWeightInput.value = value
        }
    }

    fun onBuyerTareWeightChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d+(\\.\\d*)?$"))) {
            _buyerTareWeightInput.value = value
        }
    }

    fun onOverallLaborPercentChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d+(\\.\\d*)?$"))) {
            _overallLaborPercent.value = value
        }
    }

    fun onOverallTransportChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d+(\\.\\d*)?$"))) {
            _overallTransport.value = value
        }
    }

    fun addDeduction(type: String, amount: Double, customName: String = "") {
        val newDeduction = EntryDeductionEntity(
            entryId = "",
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

    // Produce Group Management
    fun addProduceGroup(productName: String, productType: String, category: String) {
        if (productName.isBlank()) return
        val newGroup = ProduceGroupDraft(
            productName = productName.trim(),
            productType = productType.trim(),
            category = category.ifBlank { "Fruit" }
        )
        _produceGroups.value = _produceGroups.value + newGroup
    }

    fun removeProduceGroup(groupId: String) {
        _produceGroups.value = _produceGroups.value.filter { it.id != groupId }
    }

    fun addFarmerToGroup(groupId: String, farmerData: FarmerArrivalData) {
        val currentGroups = _produceGroups.value.toMutableList()
        val index = currentGroups.indexOfFirst { it.id == groupId }
        if (index != -1) {
            val group = currentGroups[index]
            val updatedFarmers = group.farmers + farmerData
            currentGroups[index] = group.copy(farmers = updatedFarmers)
            _produceGroups.value = currentGroups
        }
    }

    fun updateFarmerInGroup(groupId: String, farmerData: FarmerArrivalData) {
        val currentGroups = _produceGroups.value.toMutableList()
        val groupIndex = currentGroups.indexOfFirst { it.id == groupId }
        if (groupIndex != -1) {
            val group = currentGroups[groupIndex]
            val farmerIndex = group.farmers.indexOfFirst { it.id == farmerData.id }
            if (farmerIndex != -1) {
                val updatedFarmers = group.farmers.toMutableList()
                updatedFarmers[farmerIndex] = farmerData
                currentGroups[groupIndex] = group.copy(farmers = updatedFarmers)
                _produceGroups.value = currentGroups
            }
        }
    }

    fun removeFarmerFromGroup(groupId: String, farmerDataId: String) {
        val currentGroups = _produceGroups.value.toMutableList()
        val groupIndex = currentGroups.indexOfFirst { it.id == groupId }
        if (groupIndex != -1) {
            val group = currentGroups[groupIndex]
            val updatedFarmers = group.farmers.filter { it.id != farmerDataId }
            currentGroups[groupIndex] = group.copy(farmers = updatedFarmers)
            _produceGroups.value = currentGroups
        }
    }

    fun createBuyerSaleWithFarmerArrivals(
        context: Context,
        buyer: BuyerEntity
    ) {
        val groups = _produceGroups.value
        val allFarmers = groups.flatMap { it.farmers }
        if (groups.isEmpty() || allFarmers.isEmpty()) {
            viewModelScope.launch {
                _error.emit("Please add at least one Produce Group and Farmer Arrival entry.")
            }
            return
        }

        val totals = buyerSaleTotal.value
        if (totals.weightMatchStatus != WeightMatchStatus.MATCHED) {
            viewModelScope.launch {
                when (totals.weightMatchStatus) {
                    WeightMatchStatus.NO_WEIGHT_ENTERED -> _error.emit("Please enter Sale Quantity.")
                    WeightMatchStatus.UNDER_ALLOCATED -> _error.emit("Weight not matched! ${String.format("%.2f", totals.weightDifferenceKg)} KG is still unallocated.")
                    WeightMatchStatus.OVER_ALLOCATED -> _error.emit("Weight exceeds sale quantity! Farmer allocation exceeds net sale weight by ${String.format("%.2f", Math.abs(totals.weightDifferenceKg))} KG.")
                    else -> _error.emit("Weight reconciliation failed.")
                }
            }
            return
        }

        if (_isLoading.value) return // Single submission protection

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val saleId = UUID.randomUUID().toString()
                val currentBillNumber = _billNumber.value

                val farmerArrivalsList = mutableListOf<Triple<ArrivalEntity, SaleItemEntity, List<EntryDeductionEntity>>>()

                for (group in groups) {
                    for (farmerData in group.farmers) {
                        val farmerBillNo = farmerData.billNumber.ifEmpty { currentBillNumber }

                        for (gradeEntry in farmerData.gradeEntries) {
                            val arrivalId = UUID.randomUUID().toString()
                            val grossAmount = gradeEntry.grossAmount
                            val commissionAmount = (grossAmount * farmerData.commissionPercent) / 100.0
                            val netPayable = grossAmount - commissionAmount - farmerData.laborCharges - farmerData.transportCharges - farmerData.packingCharges - farmerData.farmerTotalDeductions

                            val arrival = ArrivalEntity(
                                id = arrivalId,
                                farmerId = farmerData.farmerId,
                                farmerName = farmerData.farmerName,
                                productId = group.productName.lowercase().replace(" ", "_"),
                                productName = group.productName,
                                productType = group.productType,
                                productCategory = group.category,
                                grade = gradeEntry.grade,
                                quantity = gradeEntry.quantity,
                                unit = gradeEntry.unit,
                                boxCount = gradeEntry.totalBoxes,
                                tareWeight = gradeEntry.totalTareWeightKg,
                                spoilageQuantity = gradeEntry.calculatedTotalSpoilageKg,
                                netQuantity = gradeEntry.netQuantity,
                                remainingQuantity = 0.0, // Fully sold to buyer in this transaction
                                purchaseRate = gradeEntry.rate,
                                grossAmount = grossAmount,
                                commissionPercent = farmerData.commissionPercent,
                                commissionAmount = commissionAmount,
                                laborCharges = farmerData.laborCharges,
                                transportCharges = farmerData.transportCharges,
                                packingCharges = farmerData.packingCharges,
                                otherDeductions = farmerData.farmerTotalDeductions,
                                netAmount = netPayable,
                                billNumber = farmerBillNo,
                                totalKg = gradeEntry.totalGrossWeightKg,
                                spoilagePerTon = if (gradeEntry.unit == "Ton") gradeEntry.spoilage else 0.0,
                                totalSpoilageKg = gradeEntry.calculatedTotalSpoilageKg,
                                otherCharges = farmerData.packingCharges + farmerData.farmerTotalDeductions,
                                netPayable = netPayable,
                                boxWeightMode = "AVERAGE",
                                numberOfBoxes = gradeEntry.boxCount,
                                totalWeightTon = if (gradeEntry.unit == "Boxes" || gradeEntry.unit == "Ton") gradeEntry.quantity else 0.0,
                                emptyBoxWeightPerBox = if (gradeEntry.unit == "Boxes") gradeEntry.avgGrossWeight else 0.0,
                                totalEmptyBoxWeightKg = if (gradeEntry.unit == "Boxes") gradeEntry.totalTareWeightKg else 0.0,
                                spoilagePercentage = if (gradeEntry.unit == "Boxes" || gradeEntry.unit == "KG") gradeEntry.spoilage else 0.0,
                                spoilageKg = gradeEntry.calculatedTotalSpoilageKg,
                                grossWeightKg = gradeEntry.totalGrossWeightKg,
                                weightAfterEmptyBoxesKg = gradeEntry.balanceKgBeforeSpoilage,
                                finalNetWeightKg = gradeEntry.totalNetWeightKg,
                                ratePerKg = gradeEntry.rate,
                                date = System.currentTimeMillis()
                            )

                            val saleItem = SaleItemEntity(
                                id = UUID.randomUUID().toString(),
                                saleId = saleId,
                                arrivalId = arrivalId,
                                farmerId = farmerData.farmerId,
                                farmerName = farmerData.farmerName,
                                productId = arrival.productId,
                                productName = group.productName,
                                productType = group.productType,
                                productCategory = group.category,
                                grade = gradeEntry.grade,
                                quantitySold = gradeEntry.totalNetWeightKg,
                                inputQuantity = gradeEntry.quantity,
                                unit = gradeEntry.unit,
                                purchaseRate = gradeEntry.rate,
                                saleRate = farmerData.buyerSaleRate,
                                purchaseAmount = grossAmount,
                                saleAmount = gradeEntry.totalNetWeightKg * farmerData.buyerSaleRate,
                                marginAmount = (gradeEntry.totalNetWeightKg * farmerData.buyerSaleRate) - grossAmount,
                                commissionPercent = 0.0,
                                commissionAmount = 0.0,
                                laborCharges = 0.0,
                                transportCharges = 0.0,
                                otherCharges = 0.0,
                                netAmount = gradeEntry.totalNetWeightKg * farmerData.buyerSaleRate,
                                date = System.currentTimeMillis()
                            )

                            val farmerDeductionsToSave = farmerData.deductions.map {
                                it.copy(entryId = arrivalId, billId = farmerBillNo)
                            }

                            farmerArrivalsList.add(Triple(arrival, saleItem, farmerDeductionsToSave))
                        }
                    }
                }

                val allDistinctFarmerNames = allFarmers.map { it.farmerName }.distinct()
                val distinctProducts = groups.map { it.productName }.distinct()

                val saleEntity = SaleEntity(
                    id = saleId,
                    buyerId = buyer.id,
                    buyerName = buyer.name,
                    farmerName = allDistinctFarmerNames.joinToString(", "),
                    productId = if (distinctProducts.size > 1) "Multiple" else distinctProducts.first().lowercase().replace(" ", "_"),
                    productName = if (distinctProducts.size > 1) "Multiple Products" else distinctProducts.first(),
                    productType = if (groups.map { it.productType }.distinct().size > 1) "Multiple" else groups.first().productType,
                    grade = "Multiple",
                    totalQuantity = totals.buyerNetWeightKg,
                    totalPurchaseAmount = totals.totalFarmerPurchaseAmount,
                    totalAmount = totals.totalBuyerSaleSubtotal,
                    totalCommission = 0.0,
                    laborPercentage = totals.laborPercentage,
                    laborCharges = totals.totalLaborCharges,
                    transportCharges = totals.totalTransportCharges,
                    packingCharges = 0.0,
                    otherCharges = totals.totalOtherDeductions,
                    billNumber = currentBillNumber,
                    totalNetAmount = totals.totalFinalCollection,
                    totalMargin = totals.totalBuyerSaleSubtotal - totals.totalFarmerPurchaseAmount,
                    pendingAmount = totals.totalFinalCollection,
                    date = System.currentTimeMillis()
                )

                val buyerDeductionsToSave = _deductions.value.map {
                    it.copy(entryId = saleId, billId = currentBillNumber)
                }

                when (val result = saleRepository.createBuyerSaleWithFarmerArrivals(
                    sale = saleEntity,
                    farmerArrivals = farmerArrivalsList,
                    buyerDeductions = buyerDeductionsToSave
                )) {
                    is Resource.Success -> {
                        billNumberRepository.incrementBillNumber(Constants.SeriesType.SALE)
                        billNumberRepository.incrementBillNumber(Constants.SeriesType.STOCK)

                        _saveSuccess.emit(Unit)
                        _produceGroups.value = emptyList()
                        _saleQuantityInput.value = ""
                        _buyerGrossWeightInput.value = ""
                        _buyerTareWeightInput.value = ""
                        _overallLaborPercent.value = ""
                        _overallTransport.value = ""
                        _deductions.value = emptyList()
                    }
                    is Resource.Error -> {
                        _error.emit(result.message ?: "Failed to save Sale Entry")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "CREATE_BUYER_SALE_WITH_ARRIVALS_FAILED")
                _error.emit("An error occurred while saving: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerBuyerAndCreateSaleWithFarmerArrivals(
        context: Context,
        name: String,
        mobile: String,
        address: String,
        gst: String
    ) {
        if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            viewModelScope.launch {
                _error.emit("Please enter a valid 10-digit mobile number")
            }
            return
        }

        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            val normalizedMobile = PhoneNumberUtils.normalize(mobile)
            val existingBuyer = buyers.value.find {
                it.name.equals(name, ignoreCase = true) &&
                        PhoneNumberUtils.normalize(it.mobileNumber) == normalizedMobile
            }

            if (existingBuyer != null) {
                _isLoading.value = false
                createBuyerSaleWithFarmerArrivals(context = context, buyer = existingBuyer)
                return@launch
            }

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
                _isLoading.value = false
                createBuyerSaleWithFarmerArrivals(context = context, buyer = newBuyer)
            }
        }
    }

    companion object {
        fun getUnitConversionFactor(unit: String): Double {
            return when (unit.uppercase(Locale.getDefault())) {
                "QUINTAL", "QTL" -> 100.0
                "TON", "METRIC TON", "MT" -> 1000.0
                "BAGS", "BAG" -> 50.0
                else -> 1.0 // KG
            }
        }
    }
}
