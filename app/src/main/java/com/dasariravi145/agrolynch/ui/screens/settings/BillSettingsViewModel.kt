package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.BillNumberSeriesEntity
import com.dasariravi145.agrolynch.domain.repository.BillNumberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BillSettingsViewModel @Inject constructor(
    private val repository: BillNumberRepository
) : ViewModel() {

    init {
        Timber.tag("BillSeries").d("BillSettingsViewModel initialized")
        initializeSeriesIfEmpty()
    }

    val seriesList: StateFlow<List<BillNumberSeriesEntity>> = repository.getAllSeries()
        .onEach { list ->
            Timber.tag("BillSeries").d("Loaded %d bill series", list.size)
            if (list.isEmpty()) {
                Timber.tag("BillSeries").w("Bill series list is EMPTY in database")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun initializeSeriesIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Timber.tag("BillSeries").d("Checking if bill series initialization is needed")
                // Use a direct check instead of waiting for the flow first() which might hang if empty
                val types = listOf("STOCK", "SALE", "PAYMENT")
                for (type in types) {
                    val series = repository.getSeriesByType(type)
                    if (series == null) {
                        Timber.tag("BillSeries").d("Series %s missing, initializing...", type)
                        repository.generateNextBillNumber(type)
                    }
                }
                Timber.tag("BillSeries").d("Initialization check complete")
            } catch (e: Exception) {
                Timber.tag("BillSeries").e(e, "Failed to initialize bill series")
            }
        }
    }

    fun updateSeries(series: BillNumberSeriesEntity) {
        Timber.tag("BillSeries").d("Updating series: %s", series.seriesType)
        viewModelScope.launch {
            try {
                repository.updateSeries(series)
                Timber.tag("BillSeries").d("Series updated successfully")
            } catch (e: Exception) {
                Timber.tag("BillSeries").e(e, "Failed to update series")
            }
        }
    }
}
