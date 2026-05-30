package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey val id: String = "",
    val saleId: String = "",
    val arrivalId: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantitySold: Double = 0.0,
    val unit: String = "KG",
    val purchaseRate: Double = 0.0,
    val saleRate: Double = 0.0,
    val purchaseAmount: Double = 0.0,
    val saleAmount: Double = 0.0,
    val marginAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis()
)
