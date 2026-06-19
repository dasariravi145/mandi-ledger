package com.dasariravi145.agrolynch.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.repository.*
import com.dasariravi145.agrolynch.ui.screens.auth.AuthViewModel
import com.dasariravi145.agrolynch.util.*
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val companyRepository: CompanyRepository,
    private val reportRepository: ReportRepository,
    private val syncRepository: SyncRepository,
    private val premiumStateManager: PremiumStateManager,
    private val authRepository: AuthRepository
) : BaseViewModel<DashboardState>(DashboardState()) {

    val isPremium = premiumStateManager.isPremium
    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    private val _showPremiumPopup = MutableStateFlow(false)
    val showPremiumPopup = _showPremiumPopup.asStateFlow()

    init {
        Timber.i("DashboardViewModel: Initialized")
        observeDashboardData()
        checkPremiumPopup()
    }

    private fun checkPremiumPopup() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (premiumStateManager.shouldShowPremiumPopup(userId)) {
                _showPremiumPopup.value = true
            }
        }
    }

    fun onPremiumPopupDismiss(doNotShowAgain: Boolean) {
        _showPremiumPopup.value = false
        premiumStateManager.markPopupSkipped()
        if (doNotShowAgain) {
            authRepository.getCurrentUserId()?.let { userId ->
                premiumStateManager.setPopupDisabledForUser(userId, true)
            }
        }
    }

    fun onPremiumUpgradeClick() {
        _showPremiumPopup.value = false
    }

    fun markPopupSeenAfterRegistration() {
        premiumStateManager.setHasSeenPopupAfterRegistration(true)
    }

    fun shouldShowPopupAfterRegistration(): Boolean {
        return !premiumStateManager.getCachedPremiumStatus() && !premiumStateManager.hasSeenPopupAfterRegistration()
    }

    fun setPremiumPopupEnabled(enabled: Boolean) {
        authRepository.getCurrentUserId()?.let { userId ->
            premiumStateManager.setPopupDisabledForUser(userId, !enabled)
        }
    }

    fun isPremiumPopupEnabled(): Boolean {
        val userId = authRepository.getCurrentUserId() ?: return true
        return !premiumStateManager.isPopupDisabledForUser(userId)
    }

    private fun observeDashboardData() {
        repository.getDashboardSummary()
            .onStart { updateState { it.copy(isLoading = true) } }
            .onEach { summary -> updateState { it.copy(isLoading = false, summary = summary, error = null) } }
            .catch { e ->
                updateState { it.copy(isLoading = false, error = e.message) }
                handleError(e)
            }
            .launchIn(viewModelScope)
    }

    fun exportSummary(context: Context) {
        viewModelScope.launch {
            if (!premiumStateManager.getCachedPremiumStatus()) {
                _exportStatus.emit("PREMIUM_REQUIRED")
                return@launch
            }

            try {
                updateState { it.copy(isLoading = true) }
                val profile = companyRepository.getProfile().first() ?: CompanyProfileEntity()
                val summary = uiState.value.summary
                val stock = reportRepository.getStockReport().first()
                
                val file = PdfGenerator.generateDashboardSummary(context, profile, summary, stock)
                if (file != null && file.exists()) {
                    syncRepository.uploadFile(file, "summaries/Dashboard")
                    _exportStatus.emit("SUCCESS:${file.absolutePath}")
                } else {
                    _exportStatus.emit("FAILED: Export failed")
                }
            } catch (e: Exception) {
                _exportStatus.emit("FAILED: ${e.message}")
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    fun onLogout() {
        authRepository.logout()
    }
}
