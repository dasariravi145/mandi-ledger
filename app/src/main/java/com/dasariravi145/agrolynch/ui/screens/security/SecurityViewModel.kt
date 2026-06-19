package com.dasariravi145.agrolynch.ui.screens.security

import androidx.lifecycle.ViewModel
import com.dasariravi145.agrolynch.util.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityState())
    val state: StateFlow<SecurityState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isPinSet = securityManager.isPinSet(), isBiometricEnabled = securityManager.isBiometricEnabled()) }
    }

    fun onPinEntered(pin: String) {
        val savedPin = securityManager.getPin()
        if (pin == savedPin) {
            _state.update { it.copy(isAuthenticated = true, error = null) }
            securityManager.updateLastActivityTime()
        } else {
            _state.update { it.copy(error = "invalid_pin") }
        }
    }

    fun setPin(pin: String) {
        securityManager.savePin(pin)
        _state.update { it.copy(isPinSet = true, isAuthenticated = true) }
        securityManager.updateLastActivityTime()
    }

    fun onBiometricAuthenticated() {
        _state.update { it.copy(isAuthenticated = true) }
        securityManager.updateLastActivityTime()
    }

    fun logout() {
        securityManager.clearSession()
        _state.update { it.copy(isAuthenticated = false) }
    }
    
    fun checkSession() {
        if (securityManager.isSessionExpired()) {
            _state.update { it.copy(isAuthenticated = false) }
        }
    }
}

data class SecurityState(
    val isPinSet: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val error: String? = null
)
