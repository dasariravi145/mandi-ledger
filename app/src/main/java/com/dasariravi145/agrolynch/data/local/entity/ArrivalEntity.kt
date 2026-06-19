package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "arrivals",
    indices = [
        androidx.room.Index(value = ["farmerId"]),
        androidx.room.Index(value = ["productId"]),
        androidx.room.Index(value = ["date"]),
        androidx.room.Index(value = ["isDeleted"]),
        androidx.room.Index(value = ["remainingQuantity"])
    ]
)
data class ArrivalEntity(
    @PrimaryKey val id: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val productId: String = "",
    val productName: String = "",
    val productCategory: String = "General",
    val grade: String = "",
    val quantity: Double = 0.0,
    val unit: String = "KG",
    val boxCount: Int = 0,
    val tareWeight: Double = 0.0,
    val spoilageQuantity: Double = 0.0,
    val netQuantity: Double = 0.0,
    val remainingQuantity: Double = 0.0,
    val purchaseRate: Double = 0.0,
    val grossAmount: Double = 0.0,
    val commissionPercent: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val packingCharges: Double = 0.0,
    val otherDeductions: Double = 0.0,
    val netAmount: Double = 0.0,
    val billNumber: String = "",
    val farmerPendingAmount: Double = 0.0,
    // Ton mode specific fields
    val totalKg: Double = 0.0,
    val spoilagePerTon: Double = 0.0,
    val totalSpoilageKg: Double = 0.0,
    val otherCharges: Double = 0.0,
    val netPayable: Double = 0.0,
    // Boxes mode specific fields
    val boxWeightMode: String = "AVERAGE", 
    val numberOfBoxes: Int = 0,
    val totalWeightTon: Double = 0.0,
    val emptyBoxWeightPerBox: Double = 0.0,
    val totalEmptyBoxWeightKg: Double = 0.0,
    val spoilagePercentage: Double = 0.0,
    val spoilageKg: Double = 0.0,
    val grossWeightKg: Double = 0.0,
    val weightAfterEmptyBoxesKg: Double = 0.0,
    val finalNetWeightKg: Double = 0.0,
    val ratePerKg: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
