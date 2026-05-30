package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardSummary(): Flow<DashboardSummary>
    suspend fun refreshSummary()
}
