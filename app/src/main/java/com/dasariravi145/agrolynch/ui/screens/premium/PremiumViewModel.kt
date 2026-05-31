package com.dasariravi145.agrolynch.ui.screens.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.billing.BillingManager
import com.dasariravi145.agrolynch.data.local.dao.SubscriptionDao
import com.dasariravi145.agrolynch.data.local.entity.SubscriptionEntity
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val premiumStateManager: PremiumStateManager,
    private val subscriptionDao: SubscriptionDao,
    private val userRepository: com.dasariravi145.agrolynch.domain.repository.UserRepository
) : ViewModel() {

    val isPremium = premiumStateManager.isPremium
    val productDetails = billingManager.productDetails
    
    val subscriptionHistory = subscriptionDao.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<PremiumUiState>(PremiumUiState.Idle)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            billingManager.billingError.collect { error ->
                _uiState.value = PremiumUiState.Error(error)
            }
        }
        viewModelScope.launch {
            billingManager.purchaseSuccess.collect {
                syncPremiumStatus()
                _uiState.value = PremiumUiState.Success
            }
        }
    }

    private fun syncPremiumStatus() {
        viewModelScope.launch {
            val user = userRepository.getUserProfile().first()
            user?.let { 
                userRepository.saveProfile(it.copy(isPremium = true))
            }
        }
    }

    fun subscribe(activity: Activity) {
        _uiState.value = PremiumUiState.Loading
        billingManager.launchBillingFlow(activity)
    }

    fun restorePurchases() {
        _uiState.value = PremiumUiState.Loading
        billingManager.refreshSubscriptionStatus()
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            if (premiumStateManager.getCachedPremiumStatus()) {
                _uiState.value = PremiumUiState.Success
            } else {
                _uiState.value = PremiumUiState.Error("No active subscription found")
            }
        }
    }

    fun resetState() {
        _uiState.value = PremiumUiState.Idle
    }
}

sealed class PremiumUiState {
    object Idle : PremiumUiState()
    object Loading : PremiumUiState()
    object Success : PremiumUiState()
    data class Error(val message: String) : PremiumUiState()
}
