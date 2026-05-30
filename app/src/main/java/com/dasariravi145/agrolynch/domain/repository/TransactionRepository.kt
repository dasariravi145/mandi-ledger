package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.TransactionEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(): Flow<List<TransactionEntity>>
    suspend fun addTransaction(transaction: TransactionEntity): Resource<Unit>
    suspend fun updateTransaction(transaction: TransactionEntity): Resource<Unit>
    suspend fun deleteTransaction(id: String): Resource<Unit>
    suspend fun syncTransactions(): Resource<Unit>
}
