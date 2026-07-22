package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE farmerId = :farmerId AND isDeleted = 0")
    suspend fun getTransactionsByFarmer(farmerId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteTransaction(id: String)

    @Query("UPDATE transactions SET isDeleted = 1, isSynced = 0 WHERE id IN (:ids)")
    suspend fun softDeleteTransactions(ids: List<String>)

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE transactions SET farmerId = :newId WHERE farmerId = :oldId")
    suspend fun updateFarmerId(oldId: String, newId: String)

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE farmerId = :farmerId AND isDeleted = 0")
    suspend fun getSumTotalAmountForFarmer(farmerId: String): Double?
}
