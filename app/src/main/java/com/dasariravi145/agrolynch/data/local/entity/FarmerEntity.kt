package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "farmers",
    indices = [
        androidx.room.Index(value = ["mobileNumber"]),
        androidx.room.Index(value = ["village"]),
        androidx.room.Index(value = ["isDeleted"])
    ]
)
data class FarmerEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val village: String = "",
    val notes: String = "",
    val totalArrivals: Double = 0.0,
    val totalPayments: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val advanceAmount: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
