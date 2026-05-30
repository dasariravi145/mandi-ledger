package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.ExpenseDao
import com.dasariravi145.agrolynch.data.local.entity.ExpenseEntity
import com.dasariravi145.agrolynch.domain.repository.ExpenseRepository
import com.dasariravi145.agrolynch.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ExpenseRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    override suspend fun addExpense(expense: ExpenseEntity): Resource<Unit> {
        return try {
            expenseDao.insertExpense(expense)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("expenses").document(expense.id).set(expense).await()
                        expenseDao.markAsSynced(expense.id)
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun updateExpense(expense: ExpenseEntity): Resource<Unit> {
        return try {
            val updated = expense.copy(isSynced = false, lastUpdated = System.currentTimeMillis())
            expenseDao.updateExpense(updated)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("expenses").document(expense.id).set(updated).await()
                        expenseDao.markAsSynced(expense.id)
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override suspend fun deleteExpense(id: String): Resource<Unit> {
        return try {
            expenseDao.softDeleteExpense(id)
            userId?.let { uid ->
                repositoryScope.launch {
                    try {
                        firestore.collection("users").document(uid).collection("expenses").document(id).update("isDeleted", true).await()
                    } catch (e: Exception) {
                        // Will be synced later
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed")
        }
    }

    override suspend fun syncExpenses(): Resource<Unit> {
        val uid = userId ?: return Resource.Error("User not logged in")
        return try {
            val unsynced = expenseDao.getUnsyncedExpenses()
            for (expense in unsynced) {
                firestore.collection("users").document(uid).collection("expenses").document(expense.id).set(expense).await()
                expenseDao.markAsSynced(expense.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
