package com.dasariravi145.agrolynch.ui.screens.dashboard

import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.repository.DashboardRepository
import com.dasariravi145.agrolynch.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : BaseViewModel<DashboardState>(DashboardState()) {

    init {
        Timber.i("DashboardViewModel: Initialized")
        observeDashboardData()
        refreshDashboardData()
    }

    private fun observeDashboardData() {
        repository.getDashboardSummary()
            .onStart { 
                Timber.d("DashboardViewModel: Started observing data")
                updateState { it.copy(isLoading = true) } 
            }
            .onEach { summary ->
                Timber.d("DashboardViewModel: Received data update")
                updateState { it.copy(isLoading = false, summary = summary, error = null) }
            }
            .catch { e ->
                Timber.e(e, "DashboardViewModel: Error observing data")
                updateState { it.copy(isLoading = false, error = e.message) }
                handleError(e)
            }
            .launchIn(viewModelScope)
    }

    fun refreshDashboardData() {
        launchSafe {
            Timber.d("DashboardViewModel: Refreshing data...")
            repository.refreshSummary()
            Timber.d("DashboardViewModel: Data refreshed successfully")
        }
    }
}
