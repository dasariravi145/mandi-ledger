package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleEntity
import com.dasariravi145.agrolynch.data.local.entity.SaleItemEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    fun getSales(): Flow<List<SaleEntity>>
    suspend fun createSale(sale: SaleEntity, items: List<SaleItemEntity>): Resource<Unit>
    suspend fun updateSale(sale: SaleEntity): Resource<Unit>
    suspend fun deleteSale(id: String): Resource<Unit>
    suspend fun addPaymentToSale(saleId: String, payment: PaymentEntity): Resource<Unit>
    suspend fun syncSales(): Resource<Unit>
}
