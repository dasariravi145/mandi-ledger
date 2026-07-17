package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import com.dasariravi145.agrolynch.domain.repository.SettingsRepository
import com.dasariravi145.agrolynch.domain.repository.SyncRepository
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
    private val syncRepository: SyncRepository,
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

    val userPhone = flow {
        emit(authRepository.getCurrentUserPhoneNumber() ?: "Unknown")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val lastBackupDate = userRepository.getUserProfile().map { user ->
        if (user == null || user.lastUpdatedAt == 0L) "Never"
        else com.dasariravi145.agrolynch.util.Formatter.formatDate(user.lastUpdatedAt)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Never")

    private val _isPremiumPopupEnabled = MutableStateFlow(true)
    val isPremiumPopupEnabled = _isPremiumPopupEnabled.asStateFlow()

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage = _syncMessage.asSharedFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (val result = syncRepository.syncAllData()) {
                is com.dasariravi145.agrolynch.util.Resource.Success -> _syncMessage.emit("Cloud backup successful")
                is com.dasariravi145.agrolynch.util.Resource.Error -> _syncMessage.emit(result.message ?: "Sync failed")
                else -> {}
            }
            _isSyncing.value = false
        }
    }

    fun restoreNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (val result = syncRepository.restoreAllData()) {
                is com.dasariravi145.agrolynch.util.Resource.Success -> _syncMessage.emit("Restore successful")
                is com.dasariravi145.agrolynch.util.Resource.Error -> _syncMessage.emit(result.message ?: "Restore failed")
                else -> {}
            }
            _isSyncing.value = false
        }
    }

    fun togglePremiumPopup(enabled: Boolean) {
        viewModelScope.launch {
            val phone = authRepository.getCurrentUserPhoneNumber()
            if (phone != null) {
                premiumStateManager.setPopupDisabledForUser(phone, !enabled)
                _isPremiumPopupEnabled.value = enabled
            }
        }
    }

    fun updateLanguage(code: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(code)
            onComplete()
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
                userRepository.saveProfile(it.copy(
                    isPremium = newState,
                    premiumPlan = if (newState) "LIFETIME" else "",
                    cloudBackupEnabled = newState
                ))
            }
        }
    }
}
