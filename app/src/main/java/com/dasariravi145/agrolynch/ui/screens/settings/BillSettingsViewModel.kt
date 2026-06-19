package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.BillNumberSeriesEntity
import com.dasariravi145.agrolynch.domain.repository.BillNumberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillSettingsViewModel @Inject constructor(
    private val repository: BillNumberRepository
) : ViewModel() {

    val seriesList: StateFlow<List<BillNumberSeriesEntity>> = repository.getAllSeries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSeries(series: BillNumberSeriesEntity) {
        viewModelScope.launch {
            repository.updateSeries(series)
        }
    }
}
