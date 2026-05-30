package com.dasariravi145.agrolynch.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import com.dasariravi145.agrolynch.domain.repository.BuyerRepository
import com.dasariravi145.agrolynch.domain.repository.FarmerRepository
import com.dasariravi145.agrolynch.domain.repository.PaymentRepository
import com.dasariravi145.agrolynch.domain.repository.LedgerRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val farmerRepository: FarmerRepository,
    private val buyerRepository: BuyerRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    val buyers = buyerRepository.getBuyers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val farmers = farmerRepository.getFarmers()
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
            else ledgerRepository.getFarmerLedger(id).map { it.balance }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun onTabSelected(tab: Int) {
        _selectedTab.value = tab
        _selectedPartyId.value = null
    }

    fun onPartySelected(id: String) {
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
            val payment = PaymentEntity(
                id = UUID.randomUUID().toString(),
                partyId = partyId,
                partyName = partyName,
                partyType = partyType,
                amount = amount,
                paymentMode = mode,
                referenceNumber = reference,
                notes = notes
            )
            val result = paymentRepository.addPayment(payment)
            if (result is Resource.Error) {
                _error.emit(result.message ?: "Failed to save payment")
            } else {
                _saveSuccess.emit(Unit)
            }
            _isLoading.value = false
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
