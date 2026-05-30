package com.dasariravi145.agrolynch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_summary")
data class DashboardSummaryEntity(
    @PrimaryKey val id: Int = 1,
    val todaySales: Double = 0.0,
    val todayCommission: Double = 0.0,
    val totalCommission: Double = 0.0,
    val buyerPending: Double = 0.0,
    val farmerPending: Double = 0.0,
    val netBalance: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
