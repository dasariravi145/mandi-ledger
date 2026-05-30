package com.dasariravi145.agrolynch.ui.screens.auth

data class AuthState(
    val isLoading: Boolean = false,
    val isOtpSent: Boolean = false,
    val isVerified: Boolean = false,
    val isRegistered: Boolean = false,
    val isPinCorrect: Boolean = false,
    val verificationId: String? = null,
    val phoneNumber: String = "",
    val error: String? = null
)
