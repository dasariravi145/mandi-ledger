package com.dasariravi145.agrolynch.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.util.*
import android.app.Activity
import android.content.Context
import android.util.Log
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val farmerRepository: FarmerRepository,
    private val buyerRepository: BuyerRepository,
    private val ledgerRepository: LedgerRepository,
    private val billNumberRepository: BillNumberRepository,
    private val companyRepository: CompanyRepository,
    private val exportService: LedgerExportService,
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    private val companyProfile = companyRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPrinting = MutableStateFlow<String?>(null) // Stores billNo or ID being printed
    val isPrinting: StateFlow<String?> = _isPrinting.asStateFlow()

    private val _isSharing = MutableStateFlow<String?>(null) // Stores billNo or ID being shared
    val isSharing: StateFlow<String?> = _isSharing.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _billNumber = MutableStateFlow("")
    val billNumber: StateFlow<String> = _billNumber.asStateFlow()

    init {
        generatePreviewBillNumber()
    }

    private fun generatePreviewBillNumber() {
        viewModelScope.launch {
            _billNumber.value = billNumberRepository.generateNextBillNumber(Constants.SeriesType.PAYMENT)
        }
    }

    val buyers = buyerRepository.getBuyers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val farmers = farmerRepository.getFarmers()
        .map { list ->
            Timber.tag("PAY_FARMER").d("PAY_FARMER_LIST_LOADED: ${list.size} total farmers")
            val filtered = list.filter { it.pendingAmount > 0 && !it.isDeleted }
                .sortedByDescending { it.pendingAmount }
            
            if (filtered.isEmpty()) {
                Timber.tag("PAY_FARMER").d("PAY_FARMER_NO_PENDING_FOUND")
            } else {
                Timber.tag("PAY_FARMER").d("PAY_FARMER_PENDING_FILTER_APPLIED: ${filtered.size} pending farmers")
                val settledCount = list.size - filtered.size
                if (settledCount > 0) {
                    Timber.tag("PAY_FARMER").d("PAY_FARMER_FULLY_SETTLED_REMOVED: $settledCount settled farmers excluded")
                }
            }
            filtered
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(0) // 0 for Buyer, 1 for Farmer
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val payments = combine(paymentRepository.getPayments(), _selectedTab) { list, tab ->
        val type = if (tab == 0) "BUYER" else "FARMER"
        list.filter { it.partyType == type && !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPartyId = MutableStateFlow<String?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pendingAmount: StateFlow<Double> = combine(_selectedPartyId, _selectedTab) { id, tab ->
        id to tab
    }.flatMapLatest { (id, tab) ->
        if (id == null) flowOf(0.0)
        else {
            if (tab == 0) ledgerRepository.getBuyerLedger(id).map { it.balance }
            else ledgerRepository.getFarmerLedger(id).map { summary ->
                val farmerPending = summary.totalDebit - summary.totalCredit
                Timber.tag("PAY_FARMER").d("selectedFarmer: id=$id, name=${summary.partyName}, totalDebit=${summary.totalDebit}, totalCredit=${summary.totalCredit}, pending=$farmerPending")
                farmerPending
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun onTabSelected(tab: Int) {
        _selectedTab.value = tab
        _selectedPartyId.value = null
    }

    fun onPartySelected(id: String) {
        Timber.tag("PAY_FARMER").d("onPartySelected: selectedPartyId=$id, type=${if(selectedTab.value == 0) "BUYER" else "FARMER"}")
        _selectedPartyId.value = id
    }

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    fun addPayment(
        partyId: String,
        partyName: String,
        partyType: String,
        amount: Double,
        mode: String,
        reference: String,
        notes: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentBillNumber = _billNumber.value
                if (currentBillNumber.isBlank() || currentBillNumber == "N/A") {
                    throw IllegalStateException("billNumber must be generated before ledger insert")
                }
                Timber.tag("BillRef").d("Payment billNumber=$currentBillNumber billId=$currentBillNumber paymentId=NEW")

                val payment = PaymentEntity(
                    id = UUID.randomUUID().toString(),
                    partyId = partyId,
                    partyName = partyName,
                    partyType = partyType,
                    amount = amount,
                    paymentMode = mode,
                    referenceNumber = reference,
                    billNumber = currentBillNumber,
                    notes = notes
                )
                val result = paymentRepository.addPayment(payment)
                if (result is Resource.Error) {
                    _error.emit(result.message ?: "Failed to save payment")
                } else {
                    Timber.tag("BillRef").d("Saved ledger transaction id=${payment.id} billNumber=${payment.billNumber} billId=${payment.billNumber} referenceId=${payment.id}")
                    // Finalize bill number
                    billNumberRepository.incrementBillNumber(Constants.SeriesType.PAYMENT)
                    _saveSuccess.emit(Unit)
                }
            } catch (e: Exception) {
                Timber.e(e, "SAVE_PAYMENT_FAILED")
                _error.emit("An error occurred while saving: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun printPayment(context: Context, payment: PaymentEntity) {
        val billNo = payment.billNumber.ifEmpty { payment.id }
        Log.d("PaymentBill", "Print clicked paymentId=${payment.id}")
        viewModelScope.launch {
            try {
                _isPrinting.value = billNo
                Log.d("PaymentBill", "Preparing payment bill")

                // Task 6: Payment data check
                if (payment.id.isBlank() || payment.partyName.isBlank() || payment.amount <= 0 || payment.paymentMode.isBlank()) {
                    Log.e("PaymentBill", "Payment validation failed: id=${payment.id}, party=${payment.partyName}, amount=${payment.amount}")
                    _error.emit("Cannot generate bill: Missing payment details")
                    return@launch
                }

                if (!premiumStateManager.getCachedPremiumStatus()) {
                    _exportStatus.emit("PREMIUM_REQUIRED")
                    return@launch
                }

                val activity = context.findActivity()
                if (activity == null) {
                    _exportStatus.emit("FAILED: Unable to open print. Please try again.")
                    return@launch
                }

                val profile = companyProfile.value ?: CompanyProfileEntity()
                val file = withContext(Dispatchers.IO) {
                    exportService.exportPaymentToPdf(context, profile, payment, payment.partyType)
                }

                if (file != null && file.exists() && file.length() > 0) {
                    Log.d("PaymentBill", "Generated file path=${file.absolutePath}")
                    Log.d("PaymentBill", "File exists=${file.exists()}, length=${file.length()}")
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfPrintHelper.print(activity, uri)
                    }
                } else {
                    Log.e("PaymentBill", "Bill file not generated or empty")
                    _error.emit("Bill file not generated")
                }
            } catch (e: Exception) {
                Log.e("PaymentBill", "Payment bill generation failed", e)
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isPrinting.value = null
            }
        }
    }

    fun sharePayment(context: Context, payment: PaymentEntity) {
        val billNo = payment.billNumber.ifEmpty { payment.id }
        Log.d("PaymentBill", "Share clicked paymentId=${payment.id}")
        viewModelScope.launch {
            try {
                _isSharing.value = billNo
                Log.d("PaymentBill", "Preparing payment bill")

                // Task 6: Payment data check
                if (payment.id.isBlank() || payment.partyName.isBlank() || payment.amount <= 0 || payment.paymentMode.isBlank()) {
                    Log.e("PaymentBill", "Payment validation failed: id=${payment.id}, party=${payment.partyName}, amount=${payment.amount}")
                    _error.emit("Cannot generate bill: Missing payment details")
                    return@launch
                }

                if (!premiumStateManager.getCachedPremiumStatus()) {
                    _exportStatus.emit("PREMIUM_REQUIRED")
                    return@launch
                }

                val profile = companyProfile.value ?: CompanyProfileEntity()
                val file = withContext(Dispatchers.IO) {
                    exportService.exportPaymentToPdf(context, profile, payment, payment.partyType)
                }

                if (file != null && file.exists() && file.length() > 0) {
                    Log.d("PaymentBill", "Generated file path=${file.absolutePath}")
                    Log.d("PaymentBill", "File exists=${file.exists()}, length=${file.length()}")
                    withContext(Dispatchers.Main) {
                        val uri = PdfGenerator.getUriFromFile(context, file)
                        PdfActionManager.sharePdf(context, uri)
                    }
                } else {
                    Log.e("PaymentBill", "Bill file not generated or empty")
                    _error.emit("Bill file not generated")
                }
            } catch (e: Exception) {
                Log.e("PaymentBill", "Payment bill generation failed", e)
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                _isSharing.value = null
            }
        }
    }

    fun updatePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = paymentRepository.updatePayment(payment)
            if (result is Resource.Error) {
                _error.emit(result.message ?: "Failed to update payment")
            } else {
                _saveSuccess.emit(Unit)
            }
            _isLoading.value = false
        }
    }

    fun deletePayment(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = paymentRepository.deletePayment(id)
            if (result is Resource.Error) {
                _error.emit(result.message ?: "Failed to delete payment")
            } else {
                _saveSuccess.emit(Unit)
            }
            _isLoading.value = false
        }
    }
}
