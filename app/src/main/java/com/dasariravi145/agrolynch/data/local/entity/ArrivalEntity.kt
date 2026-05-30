package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arrivals")
data class ArrivalEntity(
    @PrimaryKey val id: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val productId: String = "",
    val productName: String = "",
    val productCategory: String = "General",
    val grade: String = "",
    val quantity: Double = 0.0,
    val remainingQuantity: Double = 0.0,
    val unit: String = "KG",
    val purchaseRate: Double = 0.0,
    val grossAmount: Double = 0.0,
    val commissionPercent: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val farmerPendingAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
