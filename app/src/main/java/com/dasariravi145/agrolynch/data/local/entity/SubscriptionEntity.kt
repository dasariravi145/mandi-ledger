package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val transactionId: String,
    val userId: String,
    val userName: String,
    val planName: String,
    val amount: String,
    val status: String,
    val purchaseDate: Long,
    val expiryDate: Long,
    val accountReceived: String = "Google Play Store",
    val orderId: String = "",
    val productId: String = ""
)
