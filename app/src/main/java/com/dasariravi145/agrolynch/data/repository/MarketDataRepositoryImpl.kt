package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.ArrivalDao
import com.dasariravi145.agrolynch.data.local.dao.MarketRateDao
import com.dasariravi145.agrolynch.data.local.dao.SaleDao
import com.dasariravi145.agrolynch.domain.model.MarketForecastResult
import com.dasariravi145.agrolynch.domain.model.MarketPricePoint
import com.dasariravi145.agrolynch.domain.model.NearbyMarketPrice
import com.dasariravi145.agrolynch.domain.repository.MarketDataRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketDataRepositoryImpl @Inject constructor(
    private val marketRateDao: MarketRateDao,
    private val arrivalDao: ArrivalDao,
    private val saleDao: SaleDao,
    private val gson: Gson
) : MarketDataRepository {

    // Simple in-memory cache fallback if cache table is not enabled
    private val inMemoryCache = mutableMapOf<String, Pair<MarketForecastResult, Long>>()

    override suspend fun getHistoricalPrices(
        productName: String,
        varietyName: String,
        gradeName: String,
        marketName: String
    ): List<MarketPricePoint> = withContext(Dispatchers.IO) {
        val points = mutableListOf<MarketPricePoint>()

        // 1. Collect from MarketRateDao
        val marketRates = marketRateDao.getMarketRatesList()
            .filter { it.productName.equals(productName, ignoreCase = true) }
            .sortedBy { it.date }

        marketRates.forEach { rate ->
            val modal = (rate.minRate + rate.maxRate) / 2.0
            points.add(
                MarketPricePoint(
                    date = rate.date,
                    minRate = rate.minRate,
                    maxRate = rate.maxRate,
                    modalRate = if (modal > 0) modal else rate.minRate,
                    marketName = marketName
                )
            )
        }

        // 2. Collect from ArrivalDao for actual observed quantities & rates
        val arrivals = arrivalDao.getAllArrivalsList()
            .filter {
                it.productName.equals(productName, ignoreCase = true) &&
                        (varietyName.isBlank() || it.productType.equals(varietyName, ignoreCase = true)) &&
                        (gradeName.isBlank() || it.grade.equals(gradeName, ignoreCase = true))
            }

        val groupedArrivalsByDate = arrivals.groupBy { truncateToDay(it.date) }
        groupedArrivalsByDate.forEach { (dayDate, dayArrivals) ->
            val totalQty = dayArrivals.sumOf { it.finalNetWeightKg }
            val avgRate = if (dayArrivals.isNotEmpty()) dayArrivals.map { if (it.ratePerKg > 0) it.ratePerKg else it.purchaseRate }.average() else 0.0

            if (avgRate > 0) {
                val existingIndex = points.indexOfFirst { truncateToDay(it.date) == dayDate }
                if (existingIndex != -1) {
                    val existing = points[existingIndex]
                    points[existingIndex] = existing.copy(arrivalQuantityKg = existing.arrivalQuantityKg + totalQty)
                } else {
                    points.add(
                        MarketPricePoint(
                            date = dayDate,
                            minRate = avgRate * 0.95,
                            maxRate = avgRate * 1.05,
                            modalRate = avgRate,
                            arrivalQuantityKg = totalQty,
                            marketName = marketName
                        )
                    )
                }
            }
        }

        points.sortedBy { it.date }
    }

    override suspend fun getNearbyMarketPrices(
        productName: String,
        varietyName: String,
        gradeName: String,
        selectedMarket: String
    ): List<NearbyMarketPrice> = withContext(Dispatchers.IO) {
        val regionalMandis = listOf(
            Triple("Chittoor Mandi", 38.0, 15),
            Triple("Madanapalle Market", 40.0, 45),
            Triple("Tirupati Mandi", 36.5, 30),
            Triple("Kolar Market", 41.0, 65),
            Triple("Bengaluru APMC", 42.5, 120),
            Triple("Anantapur Mandi", 37.0, 90)
        )

        val referencePrices = getHistoricalPrices(productName, varietyName, gradeName, selectedMarket)
        val basePrice = referencePrices.lastOrNull()?.modalRate ?: 38.0

        regionalMandis
            .filter { !it.first.equals(selectedMarket, ignoreCase = true) }
            .take(4)
            .map { (name, rateFactor, dist) ->
                val estimatedRate = basePrice + ((name.hashCode() % 5) - 2) * 0.8
                val trend = if (estimatedRate > basePrice) "UP" else if (estimatedRate < basePrice) "DOWN" else "STABLE"
                NearbyMarketPrice(
                    marketName = name,
                    currentModalPrice = Math.max(1.0, estimatedRate),
                    trend = trend,
                    distanceKm = dist
                )
            }
    }

    override suspend fun getCachedForecast(
        marketName: String,
        productName: String,
        varietyName: String,
        gradeName: String
    ): MarketForecastResult? = withContext(Dispatchers.IO) {
        val key = "${marketName}_${productName}_${varietyName}_${gradeName}".lowercase()
        val cached = inMemoryCache[key]
        if (cached != null) {
            val (result, timestamp) = cached
            val ageMs = System.currentTimeMillis() - timestamp
            if (ageMs < TimeUnit.HOURS.toMillis(4)) {
                return@withContext result.copy(isCachedData = true)
            }
        }
        null
    }

    override suspend fun saveCachedForecast(result: MarketForecastResult) {
        withContext(Dispatchers.IO) {
            val key = "${result.marketName}_${result.productName}_${result.varietyName}_${result.gradeName}".lowercase()
            inMemoryCache[key] = Pair(result, System.currentTimeMillis())
        }
    }

    private fun truncateToDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
