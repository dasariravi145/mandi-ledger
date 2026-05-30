package com.dasariravi145.agrolynch.ui.screens.buyer

import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.BuyerEntity
import com.dasariravi145.agrolynch.domain.repository.BuyerRepository
import com.dasariravi145.agrolynch.util.BaseViewModel
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

data class BuyerListState(
    val buyers: List<BuyerEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BuyerViewModel @Inject constructor(
    private val repository: BuyerRepository
) : BaseViewModel<BuyerListState>(BuyerListState()) {

    init {
        observeBuyers()
    }

    private fun observeBuyers() {
        repository.getBuyers()
            .onStart { updateState { it.copy(isLoading = true) } }
            .onEach { list ->
                Timber.d("BuyerViewModel: Loaded ${list.size} buyers")
                updateState { it.copy(buyers = list, isLoading = false) }
            }
            .catch { e ->
                Timber.e(e, "BuyerViewModel: Error loading buyers")
                handleError(e)
                updateState { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        Timber.v("BuyerViewModel: Search query changed to: $query")
        updateState { it.copy(searchQuery = query) }
    }

    val filteredBuyers = uiState.map { state ->
        if (state.searchQuery.isBlank()) {
            state.buyers
        } else {
            state.buyers.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) || 
                it.address.contains(state.searchQuery, ignoreCase = true) ||
                it.mobileNumber.contains(state.searchQuery) ||
                it.gstNumber.contains(state.searchQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    fun addBuyer(name: String, mobile: String, address: String, gst: String, notes: String) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val buyer = BuyerEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                mobileNumber = mobile,
                address = address,
                gstNumber = gst,
                notes = notes
            )
            val result = repository.addBuyer(buyer)
            if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            } else {
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun updateBuyer(buyer: BuyerEntity) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val result = repository.updateBuyer(buyer)
            if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            } else {
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun deleteBuyer(id: String) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val result = repository.deleteBuyer(id)
            if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            } else {
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }
}
