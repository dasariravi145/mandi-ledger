package com.dasariravi145.agrolynch.ui.screens.farmer

import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.FarmerEntity
import com.dasariravi145.agrolynch.domain.repository.FarmerRepository
import com.dasariravi145.agrolynch.util.BaseViewModel
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

data class FarmerListState(
    val farmers: List<FarmerEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FarmerViewModel @Inject constructor(
    private val repository: FarmerRepository
) : BaseViewModel<FarmerListState>(FarmerListState()) {

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    init {
        observeFarmers()
    }

    private fun observeFarmers() {
        repository.getFarmers()
            .onStart { updateState { it.copy(isLoading = true) } }
            .onEach { list ->
                Timber.d("FarmerViewModel: Loaded ${list.size} farmers")
                updateState { it.copy(farmers = list, isLoading = false) }
            }
            .catch { e ->
                Timber.e(e, "FarmerViewModel: Error loading farmers")
                handleError(e)
                updateState { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        Timber.v("FarmerViewModel: Search query changed to: $query")
        updateState { it.copy(searchQuery = query) }
    }

    val filteredFarmers = uiState.map { state ->
        if (state.searchQuery.isBlank()) {
            state.farmers
        } else {
            state.farmers.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) || 
                it.village.contains(state.searchQuery, ignoreCase = true) ||
                it.mobileNumber.contains(state.searchQuery)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFarmer(name: String, mobile: String, village: String, notes: String) {
        if (mobile.length != 10) {
            updateState { it.copy(error = "Please enter a valid 10-digit mobile number") }
            return
        }
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val farmer = FarmerEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                mobileNumber = mobile,
                village = village,
                notes = notes
            )
            val result = repository.addFarmer(farmer)
            if (result is Resource.Error) {
                Timber.e("FarmerViewModel: Failed to add farmer: ${result.message}")
                updateState { it.copy(error = result.message) }
            } else {
                Timber.i("FarmerViewModel: Farmer added successfully")
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun updateFarmer(farmer: FarmerEntity) {
        if (farmer.mobileNumber.length != 10) {
            updateState { it.copy(error = "Please enter a valid 10-digit mobile number") }
            return
        }
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val result = repository.updateFarmer(farmer)
            if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            } else {
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun deleteFarmer(id: String) {
        launchSafe {
            updateState { it.copy(isLoading = true) }
            val result = repository.deleteFarmer(id)
            if (result is Resource.Error) {
                updateState { it.copy(error = result.message) }
            } else {
                _saveSuccess.emit(Unit)
            }
            updateState { it.copy(isLoading = false) }
        }
    }
}
