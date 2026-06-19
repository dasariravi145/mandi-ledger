package com.dasariravi145.agrolynch.util.pdf.renderer

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.dasariravi145.agrolynch.util.Formatter

class CompactPrintRenderer : BaseInvoiceRenderer() {
    override fun render(pdfDocument: PdfDocument, profile: BusinessProfile, invoice: InvoiceData) {
        val pageInfo = PdfDocument.PageInfo.Builder(400, 800, 1).create() // Custom width for compact
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val margin = 20f
        val width = 400f
        
        // 1. Center Header
        val namePaint = Paint(headerPaint).apply { textSize = 16f; textAlign = Paint.Align.CENTER }
        canvas.drawText(profile.companyName.uppercase(), width / 2, 40f, namePaint)
        
        val subPaint = Paint(normalPaint).apply { textSize = 8f; textAlign = Paint.Align.CENTER }
        canvas.drawText(profile.address, width / 2, 55f, subPaint)
        canvas.drawText("Mob: ${profile.mobile}", width / 2, 70f, subPaint)
        
        canvas.drawLine(margin, 80f, width - margin, 80f, Paint())

        // 2. Compact Info
        val infoY = 100f
        canvas.drawText("Bill: ${invoice.billNumber}", margin, infoY, boldPaint)
        canvas.drawText("Date: ${formatDate(invoice.date)}", width - margin, infoY, Paint(normalPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("To: ${invoice.customerName}", margin, infoY + 15f, normalPaint)

        // 3. Compact List
        var currentY = 140f
        invoice.products.forEach { product ->
            canvas.drawText("${product.name} x ${product.quantity}", margin, currentY, normalPaint)
            canvas.drawText("₹${Formatter.formatCurrency(product.amount)}", width - margin, currentY, Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT })
            currentY += 15f
        }
        
        canvas.drawLine(margin, currentY + 10f, width - margin, currentY + 10f, Paint())
        
        currentY += 30f
        val grandPaint = Paint(boldPaint).apply { textSize = 14f; textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL: ₹${Formatter.formatCurrency(invoice.grandTotal)}", width - margin, currentY, grandPaint)

        // 4. Compact Assets
        drawBitmapSafe(canvas, profile.qrPath, RectF(width / 2 - 30f, currentY + 20f, width / 2 + 30f, currentY + 80f))

        pdfDocument.finishPage(page)
    }
}
