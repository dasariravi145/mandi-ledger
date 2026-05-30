package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.domain.model.AnalyticsSummary
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun getAnalyticsSummary(): Flow<AnalyticsSummary>
}
