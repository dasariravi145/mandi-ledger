package com.dasariravi145.agrolynch.data.local.dao

import androidx.room.*
import com.dasariravi145.agrolynch.data.local.entity.InvoiceWizardConfigEntity

@Dao
interface InvoiceWizardDao {
    @Query("SELECT * FROM invoice_wizard_configs WHERE templateId = :templateId")
    suspend fun getConfig(templateId: String): InvoiceWizardConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: InvoiceWizardConfigEntity)
}
