package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "transactions",
    indices = [
        androidx.room.Index(value = ["farmerId"]),
        androidx.room.Index(value = ["productId"]),
        androidx.room.Index(value = ["date"]),
        androidx.room.Index(value = ["isDeleted"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val productId: String = "",
    val productName: String = "",
    val fruitType: String = "", // Legacy field, keeping for compatibility
    val quantity: Double = 0.0,
    val grossWeight: Double = 0.0,
    val emptyBoxWeight: Double = 0.0,
    val netWeight: Double = 0.0,
    val boxCount: Int = 0,
    val pricePerUnit: Double = 0.0,
    val totalAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
