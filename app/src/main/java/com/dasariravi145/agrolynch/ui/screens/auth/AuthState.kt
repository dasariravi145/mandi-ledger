package com.dasariravi145.agrolynch.ui.screens.auth

data class AuthState(
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val loadingMessage: String? = null,
    val isOtpSent: Boolean = false,
    val isVerified: Boolean = false,
    val isRegistered: Boolean = false,
    val isPinCorrect: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val showBiometricPrompt: Boolean = false,
    val verificationId: String? = null,
    val phoneNumber: String = "",
    val isForgotPinFlow: Boolean = false,
    val user: com.dasariravi145.agrolynch.data.local.entity.UserEntity? = null,
    val error: String? = null
)
