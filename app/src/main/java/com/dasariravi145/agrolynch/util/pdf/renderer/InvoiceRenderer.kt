package com.dasariravi145.agrolynch.util.pdf.renderer

import android.graphics.pdf.PdfDocument

interface InvoiceRenderer {
    fun render(
        pdfDocument: PdfDocument,
        profile: BusinessProfile,
        invoice: InvoiceData
    )
}
