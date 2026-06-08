package com.dasariravi145.agrolynch.util

import android.content.Context
import com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity
import com.dasariravi145.agrolynch.domain.model.LedgerSummary
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerExportService @Inject constructor() {

    fun exportLedgerToPdf(context: Context, profile: CompanyProfileEntity, summary: LedgerSummary, partyType: String): File? {
        return PdfGenerator.generateLedgerPdf(context, profile, summary, partyType == "FARMER")
    }
}
