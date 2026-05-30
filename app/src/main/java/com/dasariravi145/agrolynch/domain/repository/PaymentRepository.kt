package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun getPayments(): Flow<List<PaymentEntity>>
    suspend fun addPayment(payment: PaymentEntity): Resource<Unit>
    suspend fun updatePayment(payment: PaymentEntity): Resource<Unit>
    suspend fun deletePayment(id: String): Resource<Unit>
    suspend fun syncPayments(): Resource<Unit>
}
