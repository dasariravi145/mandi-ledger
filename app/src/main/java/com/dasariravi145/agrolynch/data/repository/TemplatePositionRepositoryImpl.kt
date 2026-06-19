package com.dasariravi145.agrolynch.data.repository

import com.dasariravi145.agrolynch.data.local.dao.InvoiceLayoutDao
import com.dasariravi145.agrolynch.data.local.dao.InvoiceWizardDao
import com.dasariravi145.agrolynch.data.local.dao.TemplatePositionDao
import com.dasariravi145.agrolynch.data.local.entity.InvoiceLayoutEntity
import com.dasariravi145.agrolynch.data.local.entity.InvoiceTemplatePositionEntity
import com.dasariravi145.agrolynch.data.local.entity.InvoiceWizardConfigEntity
import com.dasariravi145.agrolynch.domain.model.InvoiceLayout
import com.dasariravi145.agrolynch.domain.model.InvoiceWizardConfig
import com.dasariravi145.agrolynch.domain.repository.TemplatePositionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import javax.inject.Inject

class TemplatePositionRepositoryImpl @Inject constructor(
    private val dao: TemplatePositionDao,
    private val layoutDao: InvoiceLayoutDao,
    private val wizardDao: InvoiceWizardDao,
    private val gson: Gson
) : TemplatePositionRepository {

    override fun getPositionsByTemplate(type: String): Flow<List<InvoiceTemplatePositionEntity>> =
        dao.getPositionsByTemplate(type)

    override suspend fun savePositions(type: String, positions: List<InvoiceTemplatePositionEntity>) {
        dao.updatePositions(type, positions)
    }

    override suspend fun resetToDefault(type: String) {
        dao.deletePositionsByTemplate(type)
        dao.insertPositions(getDefaultPositions(type))
        layoutDao.deleteLayout(type)
    }

    override fun getLayout(templateId: String): Flow<InvoiceLayout?> {
        return layoutDao.getLayout(templateId).map { entity ->
            entity?.let { gson.fromJson(it.layoutJson, InvoiceLayout::class.java) }
        }
    }

    override suspend fun getLayoutSync(templateId: String): InvoiceLayout? {
        return layoutDao.getLayoutSync(templateId)?.let {
            gson.fromJson(it.layoutJson, InvoiceLayout::class.java)
        }
    }

    override suspend fun saveLayout(layout: InvoiceLayout) {
        val json = gson.toJson(layout)
        layoutDao.saveLayout(InvoiceLayoutEntity(layout.templateId, json))
    }

    override suspend fun getWizardConfig(templateId: String): InvoiceWizardConfig? {
        return wizardDao.getConfig(templateId)?.let {
            gson.fromJson(it.configJson, InvoiceWizardConfig::class.java)
        }
    }

    override suspend fun saveWizardConfig(templateId: String, config: InvoiceWizardConfig) {
        val json = gson.toJson(config)
        wizardDao.saveConfig(InvoiceWizardConfigEntity(templateId, json))
    }

    private fun getDefaultPositions(type: String): List<InvoiceTemplatePositionEntity> {
        val common = listOf(
            InvoiceTemplatePositionEntity(templateType = type, fieldKey = "BILL_NO", x = 145f, y = 368f),
            InvoiceTemplatePositionEntity(templateType = type, fieldKey = "DATE", x = 145f, y = 403f),
            InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_NAME", x = 430f, y = 368f),
            InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_MOBILE", x = 430f, y = 403f),
            InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TABLE_START", x = 72f, y = 490f),
            InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TOTALS_BOX", x = 575f, y = 612f),
            InvoiceTemplatePositionEntity(templateType = type, fieldKey = "QR_CODE", x = 345f, y = 680f, width = 70f, height = 70f),
            InvoiceTemplatePositionEntity(templateType = type, fieldKey = "SIGNATURE", x = 80f, y = 680f, width = 100f, height = 50f)
        )
        return when(type) {
            "GK_FRUITS_CLASSIC" -> common
            "ROYAL_HERITAGE_MANDI" -> listOf(
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "BILL_NO", x = 150f, y = 230f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "DATE", x = 320f, y = 230f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_NAME", x = 480f, y = 230f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TABLE_START", x = 50f, y = 300f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TOTALS_BOX", x = 540f, y = 630f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "QR_CODE", x = 50f, y = 720f, width = 80f, height = 80f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "SIGNATURE", x = 445f, y = 720f, width = 100f, height = 60f)
            )
            "DIAMOND_BUSINESS_ELITE" -> listOf(
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "BILL_NO", x = 120f, y = 265f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "DATE", x = 120f, y = 285f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_NAME", x = 400f, y = 265f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_MOBILE", x = 400f, y = 285f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TABLE_START", x = 110f, y = 350f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TOTALS_BOX", x = 570f, y = 650f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "SIGNATURE", x = 445f, y = 780f, width = 100f, height = 40f)
            )
            "PREMIUM_FRUIT_GALLERY" -> listOf(
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "BILL_NO", x = 100f, y = 160f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "DATE", x = 100f, y = 180f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_NAME", x = 420f, y = 160f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_MOBILE", x = 420f, y = 180f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TABLE_START", x = 90f, y = 250f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TOTALS_BOX", x = 570f, y = 710f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "SIGNATURE", x = 445f, y = 785f, width = 100f, height = 40f)
            )
            "EXECUTIVE_GLASS_STYLE" -> listOf(
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "BILL_NO", x = 55f, y = 225f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "DATE", x = 55f, y = 240f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_NAME", x = 330f, y = 225f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "PARTY_MOBILE", x = 330f, y = 240f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TABLE_START", x = 100f, y = 315f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "TOTALS_BOX", x = 570f, y = 680f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "QR_CODE", x = 40f, y = 750f, width = 90f, height = 70f),
                InvoiceTemplatePositionEntity(templateType = type, fieldKey = "SIGNATURE", x = 445f, y = 750f, width = 110f, height = 50f)
            )
            else -> common
        }
    }
}
