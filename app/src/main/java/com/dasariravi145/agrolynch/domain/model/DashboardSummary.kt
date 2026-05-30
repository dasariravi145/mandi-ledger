package com.dasariravi145.agrolynch.domain.model

import com.dasariravi145.agrolynch.data.local.entity.TransactionEntity

data class DashboardSummary(
    val todaySales: Double = 0.0,
    val todayCommission: Double = 0.0,
    val commissionEarned: Double = 0.0,
    val buyerPending: Double = 0.0,
    val farmerPending: Double = 0.0,
    val netBalance: Double = 0.0,
    val cashReceived: Double = 0.0,
    val totalFarmers: Int = 0,
    val totalBuyers: Int = 0,
    val todayExpenses: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList()
)
