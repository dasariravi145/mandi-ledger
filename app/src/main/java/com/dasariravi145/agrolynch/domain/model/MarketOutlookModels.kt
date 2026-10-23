package com.dasariravi145.agrolynch.domain.model

enum class ForecastDirection {
    LIKELY_UP,
    LIKELY_DOWN,
    LIKELY_STABLE,
    INSUFFICIENT_DATA
}

enum class DataQualityLevel {
    GOOD,
    MODERATE,
    LIMITED,
    INSUFFICIENT
}

data class MarketPricePoint(
    val date: Long,
    val minRate: Double,
    val maxRate: Double,
    val modalRate: Double,
    val arrivalQuantityKg: Double = 0.0,
    val marketName: String = ""
)

data class NearbyMarketPrice(
    val marketName: String,
    val currentModalPrice: Double,
    val trend: String, // "UP", "DOWN", "STABLE"
    val distanceKm: Int = 0
)

data class DailyForecastPoint(
    val dayLabel: String, // "Today", "Tomorrow", "+2 Days", "+3 Days"
    val date: Long,
    val direction: ForecastDirection,
    val expectedLow: Double,
    val expectedMid: Double,
    val expectedHigh: Double,
    val confidencePercent: Int
)

data class MarketForecastResult(
    val productName: String,
    val varietyName: String = "",
    val gradeName: String = "",
    val marketName: String = "",
    val direction: ForecastDirection = ForecastDirection.INSUFFICIENT_DATA,
    val currentReferencePrice: Double = 0.0,
    val expectedLowTomorrow: Double = 0.0,
    val expectedMidTomorrow: Double = 0.0,
    val expectedHighTomorrow: Double = 0.0,
    val confidencePercent: Int = 0,
    val dataQuality: DataQualityLevel = DataQualityLevel.INSUFFICIENT,
    val observedSignals: List<String> = emptyList(),
    val nearbyMarkets: List<NearbyMarketPrice> = emptyList(),
    val dailyForecasts: List<DailyForecastPoint> = emptyList(),
    val priceHistory: List<MarketPricePoint> = emptyList(),
    val bestSellingWindowAdvice: String = "",
    val aiExplanation: String = "",
    val dataSourceName: String = "AGMARKNET / Mandi Market Network",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val isCachedData: Boolean = false,
    val limitationsDisclaimer: String = "Market forecasts are statistical estimates based on historical arrivals and price trends. Actual market prices may vary due to local supply, weather, quality, and demand."
)
