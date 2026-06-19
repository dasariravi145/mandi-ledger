package com.dasariravi145.agrolynch.data.repository

import android.app.Activity
import com.dasariravi145.agrolynch.data.local.dao.UserDao
import com.dasariravi145.agrolynch.data.local.entity.UserEntity
import com.dasariravi145.agrolynch.domain.repository.AuthRepository
import com.dasariravi145.agrolynch.util.NetworkMonitor
import com.dasariravi145.agrolynch.util.SecurityManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val securityManager: SecurityManager,
    private val networkMonitor: NetworkMonitor
) : AuthRepository {

    override fun sendOtp(phoneNumber: String, activity: Activity): Flow<Result<String>> = callbackFlow {
        Timber.d("FIREBASE_INITIALIZED")
        Timber.d("OTP_FLOW: Request started for $phoneNumber. Package: ${activity.packageName}")
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Timber.d("OTP_FLOW: verificationCompleted. Auto-verification successful.")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Timber.e(e, "FIRESTORE_ERROR_CODE: ${e.message}")
                Timber.e(e, "OTP_FLOW: verificationFailed. Message: ${e.message}")
                if (e.message?.contains("reCAPTCHA", ignoreCase = true) == true) {
                    Timber.w("OTP_FLOW: reCAPTCHA fallback triggered. This usually means Play Integrity or SHA keys are not configured.")
                }
                trySend(Result.failure(e))
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Timber.d("OTP_FLOW: codeSent. verificationId: $verificationId")
                trySend(Result.success(verificationId))
            }
        }

        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Timber.e(e, "OTP_FLOW: verifyPhoneNumber exception")
            trySend(Result.failure(e))
        }

        awaitClose { Timber.d("OTP_FLOW: callbackFlow closed") }
    }

    override fun verifyOtp(verificationId: String, otp: String): Flow<Result<Unit>> = callbackFlow {
        Timber.d("AUTH_OTP_CLICKED")
        Timber.d("AUTH_OTP_CODE_LENGTH: ${otp.length}")
        Timber.d("AUTH_SIGN_IN_START")
        
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    Timber.d("AUTH_SIGN_IN_SUCCESS")
                    Timber.d("AUTH_UID: $uid")
                    Timber.d("AUTH_CURRENT_USER_UID: $uid")
                    trySend(Result.success(Unit))
                    close()
                } else {
                    val error = task.exception?.message ?: "Verification failed"
                    Timber.e("AUTH_SIGN_IN_FAILED: $error")
                    Timber.e("FIRESTORE_ERROR_MESSAGE: $error")
                    trySend(Result.failure(task.exception ?: Exception(error)))
                    close()
                }
            }
        awaitClose { }
    }

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    override fun logout() {
        auth.signOut()
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun getCurrentUserPhoneNumber(): String? = auth.currentUser?.phoneNumber

    override suspend fun registerUser(user: UserEntity): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Timber.e("AUTH_UID_NULL_BEFORE_FIRESTORE")
                throw Exception("Login session expired. Please login again.")
            }

            // Always ensure local Room database is updated
            userDao.insertUser(user)
            
            // Try Firestore sync with timeout
            val isOnline = networkMonitor.isNetworkAvailable()
            if (isOnline) {
                Timber.d("FIRESTORE_PROFILE_SYNC_START")
                try {
                    withTimeout(5000L) {
                        val userRef = firestore.collection("users").document(uid)
                        
                        val firestoreData = hashMapOf(
                            "userId" to uid,
                            "fullName" to user.name,
                            "marketName" to user.location,
                            "phoneNumber" to (auth.currentUser?.phoneNumber ?: user.phoneNumber),
                            "role" to "AGENT",
                            "pinCreated" to true,
                            "profileCreated" to true,
                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        userRef.set(firestoreData, SetOptions.merge()).await()
                        Timber.d("FIRESTORE_PROFILE_SYNC_SUCCESS")
                        securityManager.setPendingProfileSync(false)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Timber.e("FIRESTORE_PROFILE_SYNC_TIMEOUT")
                    securityManager.setPendingProfileSync(true)
                } catch (e: Exception) {
                    Timber.e(e, "FIRESTORE_PROFILE_SYNC_FAILED")
                    securityManager.setPendingProfileSync(true)
                }
            } else {
                securityManager.setPendingProfileSync(true)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "PROFILE_SAVE_FAILED")
            Result.failure(e)
        }
    }

    override fun saveSession(uid: String, phone: String, name: String, location: String, pin: String) {
        securityManager.saveSession(uid, phone, name, location, pin)
    }

    override fun isProfileCreated(): Boolean = securityManager.isProfileCreated()

    override fun isPinCreated(): Boolean = securityManager.isPinCreated()

    override suspend fun getUserProfile(uid: String): UserEntity? {
        val local = userDao.getUserById(uid)
        if (local != null) return local

        Timber.d("CURRENT_UID: $uid")
        
        try {
            val doc = firestore.collection("users").document(uid).get(com.google.firebase.firestore.Source.CACHE).await()
            if (doc.exists()) {
                val name = doc.getString("fullName") ?: doc.getString("name") ?: ""
                val phone = doc.getString("phoneNumber") ?: doc.getString("phone") ?: ""
                val location = doc.getString("marketName") ?: doc.getString("location") ?: ""
                val user = UserEntity(id = uid, name = name, phoneNumber = phone, location = location)
                userDao.insertUser(user)
                return user
            }
        } catch (e: Exception) {
            Timber.d("FIRESTORE_READ_CACHE_FAILED: ${e.message}")
        }

        if (!networkMonitor.isNetworkAvailable()) {
            return null
        }

        return try {
            Timber.d("FIRESTORE_READ_START")
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                Timber.d("FIRESTORE_READ_SUCCESS")
                val name = doc.getString("fullName") ?: doc.getString("name") ?: ""
                val phone = doc.getString("phoneNumber") ?: doc.getString("phone") ?: ""
                val location = doc.getString("marketName") ?: doc.getString("location") ?: ""
                val user = UserEntity(id = uid, name = name, phoneNumber = phone, location = location)
                userDao.insertUser(user)
                user
            } else null
        } catch (e: Exception) {
            Timber.e(e, "FIRESTORE_READ_FAILED")
            null
        }
    }

    override suspend fun savePin(uid: String, pin: String) {
        securityManager.savePin(pin)
    }

    override suspend fun getSavedPin(): String? = securityManager.getPin()

    override suspend fun hasSavedPin(): Boolean {
        val exists = securityManager.isPinSet()
        Timber.d("PIN_EXISTS: $exists")
        return exists
    }

    override suspend fun updatePin(newPin: String) {
        securityManager.savePin(newPin)
    }

    override fun setPendingProfileSync(pending: Boolean) {
        securityManager.setPendingProfileSync(pending)
    }

    override fun hasPendingProfileSync(): Boolean = securityManager.hasPendingProfileSync()
}
