package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpenses(): Flow<List<ExpenseEntity>>
    suspend fun addExpense(expense: ExpenseEntity): Resource<Unit>
    suspend fun updateExpense(expense: ExpenseEntity): Resource<Unit>
    suspend fun deleteExpense(id: String): Resource<Unit>
    suspend fun syncExpenses(): Resource<Unit>
}
