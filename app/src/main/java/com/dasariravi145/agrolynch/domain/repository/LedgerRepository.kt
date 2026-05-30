package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.domain.model.LedgerSummary
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun getFarmerLedger(farmerId: String): Flow<LedgerSummary>
    fun getBuyerLedger(buyerId: String): Flow<LedgerSummary>
    fun getAllFarmerSummaries(): Flow<List<LedgerSummary>>
    fun getAllBuyerSummaries(): Flow<List<LedgerSummary>>
}
