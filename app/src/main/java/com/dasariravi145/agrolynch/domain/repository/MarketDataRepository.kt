package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.domain.model.MarketForecastResult
import com.dasariravi145.agrolynch.domain.model.MarketPricePoint
import com.dasariravi145.agrolynch.domain.model.NearbyMarketPrice

interface MarketDataRepository {
    suspend fun getHistoricalPrices(
        productName: String,
        varietyName: String,
        gradeName: String,
        marketName: String
    ): List<MarketPricePoint>

    suspend fun getNearbyMarketPrices(
        productName: String,
        varietyName: String,
        gradeName: String,
        selectedMarket: String
    ): List<NearbyMarketPrice>

    suspend fun getCachedForecast(
        marketName: String,
        productName: String,
        varietyName: String,
        gradeName: String
    ): MarketForecastResult?

    suspend fun saveCachedForecast(result: MarketForecastResult)
}
