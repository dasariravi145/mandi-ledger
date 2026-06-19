package com.dasariravi145.agrolynch.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
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
        _state.update { 
            it.copy(
                isBiometricEnabled = securityManager.isBiometricEnabled(),
                verificationId = savedId
            )
        }
        syncPendingProfile()
    }

    private fun syncPendingProfile() {
        if (repository.hasPendingProfileSync() && repository.isUserLoggedIn()) {
            val uid = repository.getCurrentUserId() ?: return
            viewModelScope.launch {
                val user = repository.getUserProfile(uid)
                if (user != null) {
                    repository.registerUser(user)
                }
            }
        }
    }

    fun checkBiometricAvailability(context: android.content.Context) {
        val available = com.dasariravi145.agrolynch.util.BiometricAuth.isBiometricAvailable(context)
        Timber.d("BIOMETRIC_AVAILABLE: $available")
        _state.update { it.copy(isBiometricAvailable = available) }
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SendOtp -> sendOtp(event.phoneNumber, event.activity, event.isForgotPin)
            is AuthEvent.VerifyOtp -> verifyOtp(event.otp)
            is AuthEvent.Logout -> logout()
            is AuthEvent.RegisterUser -> registerUser(event.name, event.location, event.pin)
            is AuthEvent.VerifyPin -> verifyPin(event.pin)
            is AuthEvent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun registerUser(name: String, location: String, pin: String) {
        Timber.d("PROFILE_SAVE_CLICKED")
        
        // 1. Validation
        if (name.isBlank() || location.isBlank() || pin.length != 4) {
            Timber.e("PROFILE_VALIDATE_FAILED")
            _state.update { it.copy(error = "Please fill all fields correctly.") }
            return
        }
        Timber.d("PROFILE_VALIDATE_SUCCESS")

        val uid = repository.getCurrentUserId()
        if (uid == null) {
            Timber.e("AUTH_UID_NULL_BEFORE_FIRESTORE")
            _state.update { it.copy(error = "Login session expired. Please login again.") }
            return
        }
        
        val phone = repository.getCurrentUserPhoneNumber() ?: ""
        
        // 2. Local Session & PIN Save FIRST
        Timber.d("LOCAL_SESSION_SAVE_START")
        repository.saveSession(uid, phone, name, location, pin)
        Timber.d("LOCAL_SESSION_SAVE_SUCCESS")

        // 3. Navigate Home IMMEDIATELY after local save
        Timber.d("NAVIGATE_HOME_NOW")
        val available = com.dasariravi145.agrolynch.util.BiometricAuth.isBiometricAvailable(securityManager.context)
        if (available) {
            _state.update { it.copy(showBiometricPrompt = true) }
        } else {
            _state.update { it.copy(isRegistered = true) }
        }

        // 4. Firestore sync in BACKGROUND
        viewModelScope.launch {
            Timber.d("FIRESTORE_PROFILE_SYNC_START")
            val user = UserEntity(id = uid, name = name, phoneNumber = phone, location = location)
            val result = repository.registerUser(user)
            if (result.isSuccess) {
                if (!repository.hasPendingProfileSync()) {
                    Timber.d("FIRESTORE_PROFILE_SYNC_SUCCESS")
                } else {
                    // This means it was marked as pending due to timeout or error but handled internally
                }
            } else {
                Timber.e("FIRESTORE_PROFILE_SYNC_FAILED")
            }
        }
    }

    private fun sendOtp(phoneNumber: String, activity: Activity, isForgotPin: Boolean) {
        Timber.d("AUTH_REGISTER_CLICKED")
        Timber.d("AUTH_SEND_OTP_STARTED")
        Timber.d("AUTH_PHONE_NUMBER: $phoneNumber")
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
            
            // Show robot verification message after a small delay if still loading
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                if (_state.value.isLoading) {
                    Timber.d("AUTH_RECAPTCHA_STARTED")
                    _state.update { 
                        it.copy(loadingMessage = "Verifying phone number...\nPlease complete verification if prompted.") 
                    }
                }
            }

            repository.sendOtp(phoneNumber, activity).collect { result ->
                result.onSuccess { verificationId ->
                    Timber.d("AUTH_RECAPTCHA_COMPLETED")
                    Timber.d("AUTH_CODE_SENT")
                    Timber.d("AUTH_VERIFICATION_ID: $verificationId")
                    savedStateHandle["verificationId"] = verificationId
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            loadingMessage = null,
                            verificationId = verificationId, 
                            isOtpSent = true 
                        ) 
                    }
                }.onFailure { e ->
                    Timber.e("AUTH_SEND_OTP_FAILED: ${e.message}")
                    val errorMessage = if (e.message?.contains("reCAPTCHA", ignoreCase = true) == true) {
                        "Phone verification was cancelled. Please try again."
                    } else {
                        e.message ?: "Failed to send OTP"
                    }
                    _state.update { it.copy(isLoading = false, loadingMessage = null, error = errorMessage) }
                }
            }
        }
    }

    private fun verifyOtp(otp: String) {
        Timber.d("AUTH_OTP_CLICKED")
        Timber.d("AUTH_OTP_CODE_LENGTH: ${otp.length}")
        val verificationId = _state.value.verificationId
        if (verificationId == null) {
            _state.update { it.copy(error = "OTP session expired. Please resend OTP.") }
            return
        }
        
        viewModelScope.launch {
            Timber.d("AUTH_SIGN_IN_START")
            _state.update { it.copy(isLoading = true, loadingMessage = "Verifying...", error = null) }
            repository.verifyOtp(verificationId, otp).collect { result ->
                result.onSuccess {
                    val uid = repository.getCurrentUserId() ?: ""
                    Timber.d("AUTH_SIGN_IN_SUCCESS")
                    Timber.d("AUTH_UID: $uid")
                    Timber.d("AUTH_CURRENT_USER_UID: $uid")
                    
                    val pinExists = repository.hasSavedPin()
                    Timber.d("AUTH_PIN_EXISTS: $pinExists")
                    
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            loadingMessage = null,
                            isVerified = true, 
                            isRegistered = pinExists 
                        ) 
                    }
                    Timber.d("AUTH_NAVIGATION")
                }.onFailure { e ->
                    Timber.e("AUTH_SIGN_IN_FAILED: ${e.message}")
                    _state.update { it.copy(isLoading = false, loadingMessage = null, error = e.message) }
                }
            }
        }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val savedPin = repository.getSavedPin()
            if (savedPin == pin) {
                Timber.d("PIN_LOGIN_SUCCESS")
                _state.update { it.copy(isLoading = false, isPinCorrect = true) }
            } else {
                Timber.e("PIN_LOGIN_FAILED")
                _state.update { it.copy(isLoading = false, error = "Incorrect PIN") }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        Timber.d("BIOMETRIC_ENABLED: $enabled")
    }

    fun isBiometricEnabled() = securityManager.isBiometricEnabled()

    fun onBiometricSuccess() {
        Timber.d("BIOMETRIC_AUTH_SUCCESS")
        _state.update { it.copy(isPinCorrect = true) }
    }

    fun onBiometricFailure(error: String) {
        Timber.e("BIOMETRIC_AUTH_FAILED: $error")
        _state.update { it.copy(error = error) }
    }

    fun dismissBiometricPrompt() {
        _state.update { it.copy(showBiometricPrompt = false, isRegistered = true) }
    }

    private fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.update { AuthState() }
        }
    }

    fun isUserLoggedIn() = repository.isUserLoggedIn()

    fun isProfileCreated() = repository.isProfileCreated()

    fun isPinCreated() = repository.isPinCreated()

    fun getCurrentUserId() = repository.getCurrentUserId()

    fun getCurrentUserPhoneNumber() = repository.getCurrentUserPhoneNumber()

    fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isInitialLoading = true, error = null) }
            val user = repository.getUserProfile(uid)
            _state.update { it.copy(isInitialLoading = false, user = user) }
        }
    }

    suspend fun hasSavedPin() = repository.hasSavedPin()
}

sealed class AuthEvent {
    data class SendOtp(val phoneNumber: String, val activity: Activity, val isForgotPin: Boolean = false) : AuthEvent()
    data class VerifyOtp(val otp: String) : AuthEvent()
    data class RegisterUser(val name: String, val location: String, val pin: String) : AuthEvent()
    data class VerifyPin(val pin: String) : AuthEvent()
    object Logout : AuthEvent()
    object ClearError : AuthEvent()
}
