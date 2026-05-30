package com.dasariravi145.agrolynch.domain.repository

import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getFCMToken(): String?
    suspend fun updateTokenInFirestore(token: String)
    suspend fun subscribeToTopic(topic: String)
    suspend fun unsubscribeFromTopic(topic: String)
}
