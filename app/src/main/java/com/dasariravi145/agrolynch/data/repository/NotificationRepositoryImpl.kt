package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging
) : NotificationRepository {

    override suspend fun getFCMToken(): String? {
        return try {
            messaging.token.await()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateTokenInFirestore(token: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            // Handle error (e.g., if document doesn't exist yet, use set)
            firestore.collection("users")
                .document(userId)
                .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                .await()
        }
    }

    override suspend fun subscribeToTopic(topic: String) {
        messaging.subscribeToTopic(topic).await()
    }

    override suspend fun unsubscribeFromTopic(topic: String) {
        messaging.unsubscribeFromTopic(topic).await()
    }
}
