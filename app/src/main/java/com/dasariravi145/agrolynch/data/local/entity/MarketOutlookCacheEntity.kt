package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_outlook_cache")
data class MarketOutlookCacheEntity(
    @PrimaryKey val id: String = "",
    val marketName: String = "",
    val productName: String = "",
    val varietyName: String = "",
    val gradeName: String = "",
    val resultJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
