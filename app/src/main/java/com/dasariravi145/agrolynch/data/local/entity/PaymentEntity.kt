package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        androidx.room.Index(value = ["partyId"]),
        androidx.room.Index(value = ["partyType"]),
        androidx.room.Index(value = ["date"]),
        androidx.room.Index(value = ["isDeleted"])
    ]
)
data class PaymentEntity(
    @PrimaryKey val id: String = "",
    val partyId: String = "",
    val partyName: String = "",
    val partyType: String = "", // "FARMER" or "BUYER"
    val amount: Double = 0.0,
    val paymentMode: String = "CASH",
    val referenceNumber: String = "",
    val remainingBalance: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
