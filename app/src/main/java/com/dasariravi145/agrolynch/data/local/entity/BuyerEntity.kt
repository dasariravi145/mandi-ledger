package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "buyers",
    indices = [
        androidx.room.Index(value = ["name"]),
        androidx.room.Index(value = ["mobileNumber"]),
        androidx.room.Index(value = ["isDeleted"])
    ]
)
data class BuyerEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val gstNumber: String = "",
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val totalPurchase: Double = 0.0,
    val totalPaid: Double = 0.0,
    val pendingAmount: Double = 0.0
)
