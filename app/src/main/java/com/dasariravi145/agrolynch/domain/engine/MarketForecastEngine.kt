package com.dasariravi145.agrolynch.domain.engine

import com.dasariravi145.agrolynch.domain.model.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object MarketForecastEngine {

    fun generateForecast(
        productName: String,
        varietyName: String,
        gradeName: String,
        marketName: String,
        historicalPrices: List<MarketPricePoint>,
        nearbyMarkets: List<NearbyMarketPrice> = emptyList()
    ): MarketForecastResult {
        val sortedHistory = historicalPrices.sortedBy { it.date }

        if (sortedHistory.size < 3) {
            return MarketForecastResult(
                productName = productName,
                varietyName = varietyName,
                gradeName = gradeName,
                marketName = marketName,
                direction = ForecastDirection.INSUFFICIENT_DATA,
                dataQuality = DataQualityLevel.INSUFFICIENT,
                observedSignals = listOf("Fewer than 3 market price observations available for $productName ($varietyName $gradeName)."),
                limitationsDisclaimer = "Forecast disabled due to insufficient market observations. Please record or import at least 3 daily rates."
            )
        }

        val currentPoint = sortedHistory.last()
        val currentRefPrice = currentPoint.modalRate

        // 1. Moving Averages & Trend Percentage
        val recent3 = sortedHistory.takeLast(3)
        val recent7 = sortedHistory.takeLast(7)

        val avg3 = recent3.map { it.modalRate }.average()
        val avg7 = recent7.map { it.modalRate }.average()

        val priceTrend3DayPct = if (avg3 > 0) ((currentRefPrice - avg3) / avg3) * 100.0 else 0.0
        val priceTrend7DayPct = if (avg7 > 0) ((currentRefPrice - avg7) / avg7) * 100.0 else 0.0

        // 2. Arrival Trend Analysis
        val recentArrivalsAvg = recent3.map { it.arrivalQuantityKg }.average()
        val previousArrivalsAvg = if (sortedHistory.size >= 6) {
            sortedHistory.dropLast(3).takeLast(3).map { it.arrivalQuantityKg }.average()
        } else recentArrivalsAvg

        val arrivalTrendPct = if (previousArrivalsAvg > 0) {
            ((recentArrivalsAvg - previousArrivalsAvg) / previousArrivalsAvg) * 100.0
        } else 0.0

        // 3. Volatility Calculation (Standard Deviation)
        val meanPrice = sortedHistory.map { it.modalRate }.average()
        val variance = sortedHistory.map { (it.modalRate - meanPrice) * (it.modalRate - meanPrice) }.average()
        val stdDev = Math.sqrt(variance)
        val volatilityPct = if (meanPrice > 0) (stdDev / meanPrice) * 100.0 else 0.0

        // 4. Observed Signals List
        val signals = mutableListOf<String>()

        if (priceTrend7DayPct > 1.5) {
            signals.add("7-day modal price trend is upward (+${String.format("%.1f", priceTrend7DayPct)}%)")
        } else if (priceTrend7DayPct < -1.5) {
            signals.add("7-day modal price trend is downward (${String.format("%.1f", priceTrend7DayPct)}%)")
        } else {
            signals.add("7-day modal price is relatively stable")
        }

        if (arrivalTrendPct < -2.0 && priceTrend3DayPct > 0) {
            signals.add("Market arrivals declining while prices rising (bullish supply constraint)")
        } else if (arrivalTrendPct > 5.0 && priceTrend3DayPct < 0) {
            signals.add("Increased market arrivals creating short-term price pressure")
        }

        if (nearbyMarkets.isNotEmpty()) {
            val avgNearby = nearbyMarkets.map { it.currentModalPrice }.average()
            val diff = avgNearby - currentRefPrice
            if (diff > 1.0) {
                signals.add("Nearby regional mandis average ₹${String.format("%.1f", avgNearby)}/kg (higher than local rate)")
            } else if (diff < -1.0) {
                signals.add("Nearby regional mandis average ₹${String.format("%.1f", avgNearby)}/kg (lower than local rate)")
            }
        }

        // 5. Direction Determination
        val combinedTrend = (priceTrend3DayPct * 0.6) + (priceTrend7DayPct * 0.4)
        val direction = when {
            combinedTrend > 1.0 -> ForecastDirection.LIKELY_UP
            combinedTrend < -1.0 -> ForecastDirection.LIKELY_DOWN
            else -> ForecastDirection.LIKELY_STABLE
        }

        // 6. Expected Tomorrow Range
        val trendAdjustment = (combinedTrend / 100.0).coerceIn(-0.08, 0.08)
        val expectedMidTomorrow = currentRefPrice * (1.0 + trendAdjustment)
        val marginBuffer = Math.max(1.0, currentRefPrice * (volatilityPct / 100.0).coerceIn(0.03, 0.10))

        val expectedLowTomorrow = Math.max(1.0, expectedMidTomorrow - marginBuffer)
        val expectedHighTomorrow = expectedMidTomorrow + marginBuffer

        // 7. Backtesting & Confidence Calculation
        // Compare past days predicted vs actual to measure forecast error (MAPE)
        var totalErrorPct = 0.0
        var backtestCount = 0
        for (i in 2 until sortedHistory.size) {
            val pastActual = sortedHistory[i].modalRate
            val pastPrevAvg = sortedHistory.subList(0, i).map { it.modalRate }.average()
            if (pastActual > 0 && pastPrevAvg > 0) {
                val error = abs(pastActual - pastPrevAvg) / pastActual
                totalErrorPct += error
                backtestCount++
            }
        }

        val mapePct = if (backtestCount > 0) (totalErrorPct / backtestCount) * 100.0 else 15.0
        val rawConfidence = (100.0 - (mapePct * 1.5)).coerceIn(40.0, 92.0)

        val dataQuality = when {
            sortedHistory.size >= 14 && volatilityPct < 15.0 -> DataQualityLevel.GOOD
            sortedHistory.size >= 7 -> DataQualityLevel.MODERATE
            else -> DataQualityLevel.LIMITED
        }

        val confidencePercent = when (dataQuality) {
            DataQualityLevel.GOOD -> rawConfidence.toInt()
            DataQualityLevel.MODERATE -> (rawConfidence * 0.85).toInt()
            DataQualityLevel.LIMITED -> (rawConfidence * 0.70).toInt()
            DataQualityLevel.INSUFFICIENT -> 0
        }

        // 8. Multi-day Forecast Horizon
        val oneDayMs = TimeUnit.DAYS.toMillis(1)
        val dailyForecasts = listOf(
            DailyForecastPoint("Today", currentPoint.date, ForecastDirection.LIKELY_STABLE, currentPoint.minRate, currentRefPrice, currentPoint.maxRate, 100),
            DailyForecastPoint("Tomorrow", currentPoint.date + oneDayMs, direction, expectedLowTomorrow, expectedMidTomorrow, expectedHighTomorrow, confidencePercent),
            DailyForecastPoint("+2 Days", currentPoint.date + (oneDayMs * 2), direction, Math.max(1.0, expectedMidTomorrow - (marginBuffer * 1.3)), expectedMidTomorrow * (1 + trendAdjustment * 0.8), expectedHighTomorrow + (marginBuffer * 1.3), (confidencePercent * 0.9).toInt()),
            DailyForecastPoint("+3 Days", currentPoint.date + (oneDayMs * 3), ForecastDirection.LIKELY_STABLE, Math.max(1.0, expectedMidTomorrow - (marginBuffer * 1.6)), expectedMidTomorrow, expectedHighTomorrow + (marginBuffer * 1.6), (confidencePercent * 0.8).toInt())
        )

        // 9. Selling Window Advice
        val sellingAdvice = when (direction) {
            ForecastDirection.LIKELY_UP -> "Based on available short-term supply trends, Tomorrow has a stronger price outlook than today. Consider holding inventory if local storage permits."
            ForecastDirection.LIKELY_DOWN -> "Based on rising market arrivals and price pressure, Today's current rate (₹${String.format("%.1f", currentRefPrice)}) appears relatively stronger than tomorrow's forecast. Selling sooner is advised."
            ForecastDirection.LIKELY_STABLE -> "Market prices are expected to remain steady over the next 2-3 days. Normal selling schedule is recommended."
            ForecastDirection.INSUFFICIENT_DATA -> "Insufficient market observations to provide a selling window recommendation."
        }

        return MarketForecastResult(
            productName = productName,
            varietyName = varietyName,
            gradeName = gradeName,
            marketName = marketName,
            direction = direction,
            currentReferencePrice = currentRefPrice,
            expectedLowTomorrow = expectedLowTomorrow,
            expectedMidTomorrow = expectedMidTomorrow,
            expectedHighTomorrow = expectedHighTomorrow,
            confidencePercent = confidencePercent,
            dataQuality = dataQuality,
            observedSignals = signals,
            nearbyMarkets = nearbyMarkets,
            dailyForecasts = dailyForecasts,
            priceHistory = sortedHistory,
            bestSellingWindowAdvice = sellingAdvice,
            dataSourceName = "AGMARKNET / Mandi Market Network",
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }
}
