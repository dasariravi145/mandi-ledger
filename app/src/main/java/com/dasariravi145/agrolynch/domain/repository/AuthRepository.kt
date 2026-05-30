package com.dasariravi145.agrolynch.domain.repository

import android.app.Activity
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun sendOtp(phoneNumber: String, activity: Activity): Flow<Result<String>>
    fun verifyOtp(verificationId: String, otp: String): Flow<Result<Unit>>
    fun isUserLoggedIn(): Boolean
    fun logout()
    fun getCurrentUserId(): String?
    fun getCurrentUserPhoneNumber(): String?
    suspend fun registerUser(user: UserEntity): Result<Unit>
    suspend fun getUserProfile(uid: String): UserEntity?
    suspend fun savePin(uid: String, pin: String)
    suspend fun getSavedPin(): String?
    suspend fun hasSavedPin(): Boolean
    suspend fun updatePin(newPin: String)
}
