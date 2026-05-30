package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        androidx.room.Index(value = ["buyerId"]),
        androidx.room.Index(value = ["date"]),
        androidx.room.Index(value = ["isDeleted"])
    ]
)
data class SaleEntity(
    @PrimaryKey val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val productId: String = "",
    val productName: String = "",
    val grade: String = "",
    val totalQuantity: Double = 0.0,
    val totalPurchaseAmount: Double = 0.0,
    val totalAmount: Double = 0.0, // Sale Amount
    val totalMargin: Double = 0.0,
    val transportCharges: Double = 0.0,
    val otherCharges: Double = 0.0,
    val paidAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
