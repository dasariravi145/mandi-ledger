package com.dasariravi145.agrolynch.ui.screens.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.util.PremiumStateManager
import com.dasariravi145.agrolynch.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeveloperViewModel @Inject constructor(
    private val premiumStateManager: PremiumStateManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _premiumOverride = MutableStateFlow(premiumStateManager.getPremiumTestingOverride())
    val premiumOverride = _premiumOverride.asStateFlow()

    fun setPremiumOverride(enabled: Boolean?) {
        premiumStateManager.setPremiumTestingOverride(enabled)
        _premiumOverride.value = premiumStateManager.getPremiumTestingOverride()

        if (enabled == true) {
            viewModelScope.launch {
                val profile = userRepository.getUserProfile().first()
                profile?.let {
                    userRepository.saveProfile(it.copy(
                        isPremium = true,
                        premiumPlan = "LIFETIME",
                        premiumExpiryDate = 0L,
                        premiumExpiry = 0L,
                        cloudBackupEnabled = true,
                        multiDeviceSyncEnabled = true,
                        voiceEntryEnabled = true,
                        ocrEnabled = true,
                        ocrCloudStorageEnabled = true,
                        pdfCloudStorageEnabled = true
                    ))
                }
            }
        } else if (enabled == false) {
             viewModelScope.launch {
                val profile = userRepository.getUserProfile().first()
                profile?.let {
                    userRepository.saveProfile(it.copy(
                        isPremium = false,
                        premiumPlan = "",
                        premiumExpiryDate = 0L,
                        premiumExpiry = 0L
                    ))
                }
            }
        }
    }
}
