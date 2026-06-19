package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import com.dasariravi145.agrolynch.domain.repository.SettingsRepository
import com.dasariravi145.agrolynch.domain.repository.UserRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val premiumStateManager: PremiumStateManager,
    private val userRepository: UserRepository
) : ViewModel() {

    val languageCode: StateFlow<String> = settingsRepository.languageCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAutoBackupEnabled: StateFlow<Boolean> = settingsRepository.isAutoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isPremium = premiumStateManager.isPremium

    private val _isPremiumPopupEnabled = MutableStateFlow(true)
    val isPremiumPopupEnabled = _isPremiumPopupEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                _isPremiumPopupEnabled.value = !premiumStateManager.isPopupDisabledForUser(userId)
            }
        }
    }

    fun togglePremiumPopup(enabled: Boolean) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                premiumStateManager.setPopupDisabledForUser(userId, !enabled)
                _isPremiumPopupEnabled.value = enabled
            }
        }
    }

    fun updateLanguage(code: String) {
        timber.log.Timber.i("SettingsViewModel: Updating language to: $code")
        viewModelScope.launch {
            settingsRepository.updateLanguage(code)
        }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTheme(isDark)
        }
    }

    fun toggleAutoBackup(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoBackup(isEnabled)
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun updatePin(newPin: String) {
        viewModelScope.launch {
            authRepository.updatePin(newPin)
        }
    }

    fun togglePremiumTesting() {
        val current = premiumStateManager.getCachedPremiumStatus()
        val newState = !current
        premiumStateManager.setPremiumTestingOverride(newState)
        
        viewModelScope.launch {
            val profile = userRepository.getUserProfile().first()
            profile?.let {
                if (newState) {
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
                } else {
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
