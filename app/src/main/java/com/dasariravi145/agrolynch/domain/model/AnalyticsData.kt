package com.dasariravi145.agrolynch.domain.model

data class AnalyticsSummary(
    val todaySales: Double = 0.0,
    val commissionEarned: Double = 0.0,
    val buyerPending: Double = 0.0,
    val farmerPending: Double = 0.0,
    val netBalance: Double = 0.0,
    val cashReceived: Double = 0.0,
    val salesTrend: List<ChartPoint> = emptyList(),
    val profitTrend: List<ChartPoint> = emptyList(),
    val topFarmers: List<TopEntity> = emptyList(),
    val topBuyers: List<TopEntity> = emptyList(),
    val productDistribution: List<PieChartData> = emptyList()
)

data class ChartPoint(
    val label: String,
    val value: Float
)

data class TopEntity(
    val name: String,
    val totalValue: Double,
    val transactionCount: Int
)

data class PieChartData(
    val label: String,
    val value: Float,
    val color: Long
)
