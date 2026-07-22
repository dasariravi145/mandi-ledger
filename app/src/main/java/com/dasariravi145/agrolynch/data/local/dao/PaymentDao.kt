package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsList(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeletePayment(id: String)

    @Query("UPDATE payments SET isDeleted = 1, isSynced = 0 WHERE id IN (:ids)")
    suspend fun softDeletePayments(ids: List<String>)

    @Query("SELECT * FROM payments WHERE isSynced = 0")
    suspend fun getUnsyncedPayments(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE partyId = :partyId AND partyType = :partyType AND isDeleted = 0")
    suspend fun getPaymentsByParty(partyId: String, partyType: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE partyId = :partyId AND partyType = :partyType AND isDeleted = 0 ORDER BY date DESC")
    fun getPaymentsByPartyFlow(partyId: String, partyType: String): Flow<List<PaymentEntity>>

    @Query("UPDATE payments SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE payments SET partyId = :newId WHERE partyId = :oldId")
    suspend fun updatePartyId(oldId: String, newId: String)

    @Query("SELECT SUM(amount) FROM payments WHERE partyId = :partyId AND partyType = :partyType AND isDeleted = 0")
    suspend fun getSumAmountForParty(partyId: String, partyType: String): Double?
}
