package com.dasariravi145.agrolynch.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SendOtp -> sendOtp(event.phoneNumber, event.activity)
            is AuthEvent.VerifyOtp -> verifyOtp(event.otp)
            is AuthEvent.Logout -> logout()
            is AuthEvent.RegisterUser -> registerUser(event.name, event.location, event.pin)
            is AuthEvent.VerifyPin -> verifyPin(event.pin)
        }
    }

    private fun registerUser(name: String, location: String, pin: String) {
        val uid = repository.getCurrentUserId() ?: return
        val phone = repository.getCurrentUserPhoneNumber() ?: ""
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val user = UserEntity(id = uid, name = name, phoneNumber = phone, location = location)
            val result = repository.registerUser(user)
            if (result.isSuccess) {
                repository.savePin(uid, pin)
                _state.update { it.copy(isLoading = false, isRegistered = true) }
            } else {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    private fun sendOtp(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, phoneNumber = phoneNumber) }
            repository.sendOtp(phoneNumber, activity).collect { result ->
                result.onSuccess { verificationId ->
                    _state.update { it.copy(isLoading = false, verificationId = verificationId, isOtpSent = true) }
                }.onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    private fun verifyOtp(otp: String) {
        val verificationId = _state.value.verificationId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.verifyOtp(verificationId, otp).collect { result ->
                result.onSuccess {
                    val uid = repository.getCurrentUserId() ?: ""
                    val profile = repository.getUserProfile(uid)
                    _state.update { it.copy(isLoading = false, isVerified = true, isRegistered = profile != null) }
                }.onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val savedPin = repository.getSavedPin()
            if (savedPin == pin) {
                _state.update { it.copy(isLoading = false, isPinCorrect = true) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Incorrect PIN") }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.update { AuthState() }
        }
    }

    fun isUserLoggedIn() = repository.isUserLoggedIn()

    fun getCurrentUserPhoneNumber() = repository.getCurrentUserPhoneNumber()

    suspend fun hasSavedPin() = repository.hasSavedPin()
}

sealed class AuthEvent {
    data class SendOtp(val phoneNumber: String, val activity: Activity) : AuthEvent()
    data class VerifyOtp(val otp: String) : AuthEvent()
    data class RegisterUser(val name: String, val location: String, val pin: String) : AuthEvent()
    data class VerifyPin(val pin: String) : AuthEvent()
    object Logout : AuthEvent()
}
