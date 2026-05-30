package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import com.dasariravi145.agrolynch.domain.repository.SettingsRepository
import com.dasariravi145.agrolynch.util.PremiumStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val premiumStateManager: PremiumStateManager
) : ViewModel() {

    val languageCode: StateFlow<String> = settingsRepository.languageCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAutoBackupEnabled: StateFlow<Boolean> = settingsRepository.isAutoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isPremium = premiumStateManager.isPremium

    fun updateLanguage(code: String) {
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
}
