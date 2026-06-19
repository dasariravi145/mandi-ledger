package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.InvoiceLayoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceLayoutDao {
    @Query("SELECT * FROM invoice_layouts WHERE templateId = :templateId")
    fun getLayout(templateId: String): Flow<InvoiceLayoutEntity?>

    @Query("SELECT * FROM invoice_layouts WHERE templateId = :templateId")
    suspend fun getLayoutSync(templateId: String): InvoiceLayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLayout(layout: InvoiceLayoutEntity)

    @Query("DELETE FROM invoice_layouts WHERE templateId = :templateId")
    suspend fun deleteLayout(templateId: String)
}
