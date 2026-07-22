package com.dasariravi145.agrolynch.ui.screens.auth

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import com.dasariravi145.agrolynch.util.BiometricAuth
import com.dasariravi145.agrolynch.util.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val securityManager: SecurityManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        val savedId: String? = savedStateHandle["verificationId"]
        val isForgot: Boolean = savedStateHandle["isForgotPin"] ?: false
        _state.update { 
            it.copy(
                isBiometricEnabled = securityManager.isBiometricEnabled(),
                verificationId = savedId,
                isForgotPinFlow = isForgot
            )
        }
        Timber.tag("FirebaseInit").d("AuthViewModel initialized. verificationId: $savedId, isForgot: $isForgot")
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val localUser = repository.getLocalUser()
            _state.update { it.copy(user = localUser) }
        }
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SendOtp -> sendOtp(event.phoneNumber, event.activity, event.isForgotPin)
            is AuthEvent.VerifyOtp -> verifyOtp(event.otp)
            is AuthEvent.RegisterUser -> registerUser(event.name, event.location, event.pin)
            is AuthEvent.VerifyPin -> verifyPin(event.pin)
            is AuthEvent.Logout -> logout()
            is AuthEvent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun sendOtp(phoneNumber: String, activity: Activity, isForgotPin: Boolean = false) {
        Timber.tag("OtpFlow").d("Send OTP started for: $phoneNumber, isForgotPin: $isForgotPin")
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Sending OTP...",
                    error = null,
                    phoneNumber = phoneNumber,
                    isForgotPinFlow = isForgotPin
                )
            }
            repository.sendOtp(phoneNumber, activity).collect { result ->
                result.onSuccess { id ->
                    Timber.tag("OtpFlow").d("OTP code sent successfully. Verification ID: $id")
                    savedStateHandle["verificationId"] = id
                    _state.update { it.copy(isLoading = false, verificationId = id, isOtpSent = true) }
                }.onFailure { e ->
                    Timber.tag("OtpFlow").e(e, "OTP send failed")
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to send OTP") }
                }
            }
        }
    }

    private fun verifyOtp(otp: String) {
        val verificationId = _state.value.verificationId ?: return
        Timber.tag("OtpFlow").d("Verify OTP clicked")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Verifying OTP...", error = null) }
            repository.verifyOtp(verificationId, otp).collect { result ->
                result.onSuccess {
                    Timber.tag("ProfileCheck").d("OTP verified")
                    handleUserFlow()
                }.onFailure { e ->
                    Timber.tag("OtpFlow").e(e, "OTP verification failed")
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Verification failed") }
                }
            }
        }
    }

    private suspend fun handleUserFlow() {
        val uid = repository.getCurrentUserId()
        if (uid == null) {
            Timber.tag("ProfileCheck").e("Verification succeeded but UID is null!")
            _state.update { it.copy(isLoading = false, error = "Authentication error: UID not found") }
            return
        }
        
        Timber.tag("ProfileCheck").d("UID: $uid")
        Timber.tag("ProfileCheck").d("Checking users/$uid")
        
        repository.checkUserExists(uid).onSuccess { profile ->
            if (profile != null && profile.isProfileCompleted) {
                Timber.tag("ProfileCheck").d("Profile exists and is completed")
                repository.syncLocalProfile(profile)
                
                // Fetch local user and update state to ensure navigation has data
                val localUser = repository.getLocalUser()
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        isVerified = true, 
                        isRegistered = true,
                        user = localUser,
                        loadingMessage = null,
                        isBiometricEnabled = securityManager.isBiometricEnabled()
                    ) 
                }
                Timber.tag("ProfileCheck").d("Navigate Dashboard")
            } else {
                Timber.tag("ProfileCheck").d("Profile missing or incomplete")
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        isVerified = true, 
                        isRegistered = false,
                        loadingMessage = null
                    ) 
                }
                Timber.tag("ProfileCheck").d("Navigate Create Profile")
            }
        }.onFailure { e ->
            Timber.tag("ProfileCheck").e(e, "Failed to check user profile")
            _state.update { it.copy(isLoading = false, error = e.message ?: "Network error. Please try again.") }
        }
    }

    private fun registerUser(name: String, address: String, pin: String) {
        Timber.tag("RegistrationFlow").d("Registration button clicked")
        
        if (name.isBlank() || address.isBlank() || pin.length != 4) {
            _state.update { it.copy(error = "Please fill all fields correctly.") }
            return
        }

        Timber.tag("RegistrationFlow").d("Validation success")

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Creating account...", error = null) }
            try {
                Timber.tag("RegistrationFlow").d("Saving profile...")
                val result = repository.registerUser(name, address, pin)
                if (result.isSuccess) {
                    Timber.tag("BiometricFlow").d("Profile saved. Checking biometric availability.")
                    if (_state.value.isBiometricAvailable) {
                        Timber.tag("BiometricFlow").d("Biometric available. Showing setup dialog.")
                        _state.update { it.copy(showBiometricSetupDialog = true) }
                    } else {
                        Timber.tag("BiometricFlow").d("Biometric unavailable. Skipping setup.")
                        _state.update { it.copy(isRegistered = true) }
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Timber.tag("RegistrationFlow").e(error, "Save failed")
                    _state.update { it.copy(error = error?.message ?: "Registration failed") }
                }
            } catch (e: Exception) {
                Timber.tag("RegistrationFlow").e(e, "Unexpected error in registration")
                _state.update { it.copy(error = "Unexpected error: ${e.message}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun enableBiometric(enable: Boolean, activity: FragmentActivity? = null) {
        viewModelScope.launch {
            Timber.tag("BiometricFlow").d("enableBiometric called: $enable")
            if (enable && activity != null) {
                Timber.tag("BiometricFlow").d("Prompt launched for setup")
                BiometricAuth.showBiometricPrompt(
                    activity = activity,
                    title = "Enable Biometric",
                    subtitle = "Verify identity to enable biometric login",
                    negativeButtonText = "Cancel",
                    errorAuthFailed = "Authentication failed",
                    onSuccess = {
                        viewModelScope.launch {
                            Timber.tag("BiometricFlow").d("Authentication succeeded. Persisting preference: true")
                            repository.updateBiometricEnabled(true)
                            _state.update { it.copy(showBiometricSetupDialog = false, isBiometricEnabled = true, isRegistered = true) }
                        }
                    },
                    onError = { error ->
                        Timber.tag("BiometricFlow").e("Authentication failed: $error")
                        // If it fails, we keep the setup dialog open or close it and don't enable
                        _state.update { it.copy(error = error) }
                    }
                )
            } else {
                Timber.tag("BiometricFlow").d("Persisting preference: false")
                repository.updateBiometricEnabled(false)
                _state.update { it.copy(showBiometricSetupDialog = false, isBiometricEnabled = false, isRegistered = true) }
            }
        }
    }

    fun resetPin(newPin: String) {
        Timber.tag("ForgotPinFlow").d("resetPin called")
        if (newPin.length != 4) {
            _state.update { it.copy(error = "PIN must be exactly 4 digits.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Updating PIN...", error = null) }
            try {
                val result = repository.updatePin(newPin)
                if (result.isSuccess) {
                    Timber.tag("ForgotPinFlow").d("PIN updated successfully")
                    // We can reuse a flag or add a new one for navigation
                    _state.update { it.copy(isPinCorrect = true) } 
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to update PIN"
                    Timber.tag("ForgotPinFlow").e("PIN update failed: $error")
                    _state.update { it.copy(error = error) }
                }
            } catch (e: Exception) {
                Timber.tag("ForgotPinFlow").e(e, "Error resetting PIN")
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val isCorrect = repository.verifyPin(pin)
            if (isCorrect) {
                handleAuthSuccess()
            } else {
                _state.update { it.copy(isLoading = false, error = "Incorrect PIN") }
            }
        }
    }

    private fun handleAuthSuccess() {
        _state.update { it.copy(isLoading = false, isPinCorrect = true) }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            handleAuthSuccess()
        }
    }

    fun onBiometricFailure(error: String) {
        _state.update { it.copy(error = error) }
    }

    fun checkBiometricAvailability(context: Context) {
        val available = BiometricAuth.isBiometricAvailable(context)
        _state.update { it.copy(isBiometricAvailable = available) }
    }

    fun isUserLoggedIn() = repository.isUserLoggedIn()
    fun isProfileCreated() = securityManager.isProfileCreated()
    fun isPinCreated() = securityManager.isPinCreated()
    fun getCurrentUserPhoneNumber() = repository.getCurrentUserPhoneNumber()
    fun hasSavedPin() = securityManager.isPinSet()
    suspend fun getLocalUser() = repository.getLocalUser()

    fun fetchUserProfile(mobile: String) {
        viewModelScope.launch {
            val user = repository.getLocalUser()
            _state.update { it.copy(user = user) }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.update { AuthState() }
        }
    }

    suspend fun checkAndRestoreProfile(uid: String): Boolean {
        return repository.checkUserExists(uid).getOrNull()?.let { profile ->
            if (profile.isProfileCompleted) {
                repository.syncLocalProfile(profile)
                true
            } else false
        } ?: false
    }
}

sealed class AuthEvent {
    data class SendOtp(val phoneNumber: String, val activity: Activity, val isForgotPin: Boolean = false) : AuthEvent()
    data class VerifyOtp(val otp: String) : AuthEvent()
    data class RegisterUser(val name: String, val location: String, val pin: String) : AuthEvent()
    data class VerifyPin(val pin: String) : AuthEvent()
    object Logout : AuthEvent()
    object ClearError : AuthEvent()
}
