package com.dasariravi145.agrolynch.ui.screens.marketrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.MarketRateEntity
import com.dasariravi145.agrolynch.data.local.entity.ProductEntity
import com.dasariravi145.agrolynch.domain.repository.MarketRateRepository
import com.dasariravi145.agrolynch.domain.repository.ProductRepository
import com.dasariravi145.agrolynch.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MarketRateViewModel @Inject constructor(
    private val marketRepository: MarketRateRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getCurrentDateStart())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val products: StateFlow<List<ProductEntity>> = productRepository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentRates: StateFlow<List<MarketRateEntity>> = _selectedDate
        .flatMapLatest { date -> marketRepository.getRatesByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSelectedDate(date: Long) {
        _selectedDate.value = getStartOfDay(date)
    }

    fun saveRate(productId: String, productName: String, grade: String, min: Double, max: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            val date = _selectedDate.value
            val id = "${productId}_${grade}_$date"
            val rate = MarketRateEntity(
                id = id,
                productId = productId,
                productName = productName,
                grade = grade,
                minRate = min,
                maxRate = max,
                date = date
            )
            marketRepository.saveRate(rate)
            _isLoading.value = false
        }
    }

    fun updateRate(rate: MarketRateEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            marketRepository.updateRate(rate)
            _isLoading.value = false
        }
    }

    fun deleteRate(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            marketRepository.deleteRate(id)
            _isLoading.value = false
        }
    }

    private fun getCurrentDateStart(): Long {
        return getStartOfDay(System.currentTimeMillis())
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
