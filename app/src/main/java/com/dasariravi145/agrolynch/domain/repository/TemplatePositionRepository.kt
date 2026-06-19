package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.InvoiceTemplatePositionEntity
import kotlinx.coroutines.flow.Flow

interface TemplatePositionRepository {
    fun getPositionsByTemplate(type: String): Flow<List<InvoiceTemplatePositionEntity>>
    suspend fun savePositions(type: String, positions: List<InvoiceTemplatePositionEntity>)
    suspend fun resetToDefault(type: String)

    fun getLayout(templateId: String): Flow<com.dasariravi145.agrolynch.domain.model.InvoiceLayout?>
    suspend fun getLayoutSync(templateId: String): com.dasariravi145.agrolynch.domain.model.InvoiceLayout?
    suspend fun saveLayout(layout: com.dasariravi145.agrolynch.domain.model.InvoiceLayout)

    suspend fun getWizardConfig(templateId: String): com.dasariravi145.agrolynch.domain.model.InvoiceWizardConfig?
    suspend fun saveWizardConfig(templateId: String, config: com.dasariravi145.agrolynch.domain.model.InvoiceWizardConfig)
}
