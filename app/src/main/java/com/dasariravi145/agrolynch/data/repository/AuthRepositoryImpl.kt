package com.dasariravi145.agrolynch.data.repository

import android.app.Activity
import com.dasariravi145.agrolynch.data.local.dao.UserDao
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import com.dasariravi145.agrolynch.util.SessionManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : AuthRepository {

    override fun sendOtp(phoneNumber: String, activity: Activity): Flow<Result<String>> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification or instant verification
                // In a real app, you might want to sign in automatically here if possible
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(Result.failure(e))
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                trySend(Result.success(verificationId))
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
        awaitClose {}
    }

    override fun verifyOtp(verificationId: String, otp: String): Flow<Result<Unit>> = callbackFlow {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(Result.success(Unit))
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Verification failed")))
                }
            }
        awaitClose {}
    }

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    override fun logout() = auth.signOut()

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun getCurrentUserPhoneNumber(): String? = auth.currentUser?.phoneNumber

    override suspend fun registerUser(user: UserEntity): Result<Unit> {
        return try {
            userDao.insertUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(uid: String): UserEntity? = userDao.getUserById(uid)

    override suspend fun savePin(uid: String, pin: String) {
        sessionManager.saveSession(uid, pin)
    }

    override suspend fun getSavedPin(): String? = sessionManager.userPin.firstOrNull()

    override suspend fun hasSavedPin(): Boolean = !sessionManager.userPin.firstOrNull().isNullOrEmpty()

    override suspend fun updatePin(newPin: String) {
        auth.currentUser?.uid?.let { uid ->
            sessionManager.saveSession(uid, newPin)
        }
    }
}
