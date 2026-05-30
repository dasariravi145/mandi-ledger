package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_rates")
data class MarketRateEntity(
    @PrimaryKey val id: String = "", // productID_grade_date
    val productId: String = "",
    val productName: String = "",
    val grade: String = "",
    val minRate: Double = 0.0,
    val maxRate: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
