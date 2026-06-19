package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.InvoiceTemplatePositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplatePositionDao {
    @Query("SELECT * FROM template_positions WHERE templateType = :type")
    fun getPositionsByTemplate(type: String): Flow<List<InvoiceTemplatePositionEntity>>

    @Query("SELECT * FROM template_positions WHERE templateType = :type")
    suspend fun getPositionsForTemplate(type: String): List<InvoiceTemplatePositionEntity>

    @Query("SELECT * FROM template_positions")
    suspend fun getAllPositionsList(): List<InvoiceTemplatePositionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: InvoiceTemplatePositionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPositions(positions: List<InvoiceTemplatePositionEntity>)

    @Query("DELETE FROM template_positions WHERE templateType = :type")
    suspend fun deletePositionsByTemplate(type: String)

    @Transaction
    suspend fun updatePositions(type: String, positions: List<InvoiceTemplatePositionEntity>) {
        deletePositionsByTemplate(type)
        insertPositions(positions)
    }
}
