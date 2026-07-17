package com.dasariravi145.agrolynch.domain.repository

import android.app.Activity
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.data.remote.model.FirestoreUserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun sendOtp(phoneNumber: String, activity: Activity): Flow<Result<String>>
    fun verifyOtp(verificationId: String, otp: String): Flow<Result<Unit>>
    fun isUserLoggedIn(): Boolean
    fun logout()
    fun getCurrentUserPhoneNumber(): String?
    fun getCurrentUserId(): String?
    
    suspend fun checkUserExists(uid: String): Result<FirestoreUserProfile?>
    suspend fun registerUser(fullName: String, address: String, pin: String): Result<Unit>
    suspend fun verifyPin(pin: String): Boolean
    suspend fun syncLocalProfile(profile: FirestoreUserProfile)
    suspend fun getLocalUser(): UserEntity?
    fun hashPin(pin: String): String
    suspend fun updatePin(newPin: String): Result<Unit>
    suspend fun updateBiometricEnabled(enabled: Boolean): Result<Unit>
}
