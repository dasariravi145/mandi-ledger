package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    indices = [
        androidx.room.Index(value = ["saleId"]),
        androidx.room.Index(value = ["arrivalId"]),
        androidx.room.Index(value = ["farmerId"]),
        androidx.room.Index(value = ["productId"]),
        androidx.room.Index(value = ["date"])
    ]
)
data class SaleItemEntity(
    @PrimaryKey val id: String = "",
    val saleId: String = "",
    val arrivalId: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val productId: String = "",
    val productName: String = "",
    val productCategory: String = "",
    val grade: String = "",
    val quantitySold: Double = 0.0, // Always stores Net KG
    val inputQuantity: Double = 0.0, // Stores user entered value (Ton/Boxes/KG)
    val unit: String = "KG",
    val purchaseRate: Double = 0.0,
    val saleRate: Double = 0.0,
    val purchaseAmount: Double = 0.0,
    val saleAmount: Double = 0.0,
    val marginAmount: Double = 0.0,
    val commissionPercent: Double = 0.0, // Storing percent used
    val commissionAmount: Double = 0.0,
    val laborCharges: Double = 0.0,
    val transportCharges: Double = 0.0,
    val otherCharges: Double = 0.0,
    val netAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis()
)
