package com.dasariravi145.agrolynch.util.pdf.renderer

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.dasariravi145.agrolynch.util.Formatter

class ExecutiveGlassRenderer : BaseInvoiceRenderer() {
    override fun render(pdfDocument: PdfDocument, profile: BusinessProfile, invoice: InvoiceData) {
        val page = startPage(pdfDocument)
        val canvas = page.canvas
        
        val tealColor = Color.parseColor("#00897B")

        // 1. Modern Minimalist Header
        val headerPaintTitle = Paint(headerPaint).apply { color = tealColor; textSize = 30f }
        canvas.drawText(profile.companyName, 40f, 60f, headerPaintTitle)
        
        val subPaint = Paint(normalPaint).apply { color = Color.DKGRAY; textSize = 10f }
        canvas.drawText("${profile.address} | Mob: ${profile.mobile}", 40f, 85f, subPaint)
        
        canvas.drawLine(40f, 100f, PAGE_WIDTH - 40f, 100f, Paint().apply { color = tealColor; strokeWidth = 3f })

        // 2. Info Grid
        val infoY = 140f
        drawField(canvas, "INVOICE #", invoice.billNumber, 40f, infoY)
        drawField(canvas, "DATE", formatDate(invoice.date), 40f, infoY + 20f)
        
        val rightPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("CUSTOMER", PAGE_WIDTH - 40f, infoY, rightPaint)
        canvas.drawText(invoice.customerName.uppercase(), PAGE_WIDTH - 40f, infoY + 20f, rightPaint)

        // 3. Transparent/Glass Style Table
        renderGlassTable(canvas, invoice, tealColor)

        // 4. Compact Footer
        val footY = 750f
        drawBitmapSafe(canvas, profile.qrPath, RectF(40f, footY, 110f, footY + 70f))
        
        drawBitmapSafe(canvas, profile.signaturePath, RectF(PAGE_WIDTH - 150f, footY, PAGE_WIDTH - 40f, footY + 50f))
        
        pdfDocument.finishPage(page)
    }

    private fun renderGlassTable(canvas: Canvas, invoice: InvoiceData, accent: Int) {
        val startY = 220f
        val rowHeight = 30f
        
        val headPaint = Paint().apply { color = accent; style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRect(40f, startY, PAGE_WIDTH - 40f, startY + rowHeight, headPaint)
        
        val hText = Paint(boldPaint).apply { color = accent; textSize = 10f }
        canvas.drawText("PARTICULARS", 50f, startY + 20f, hText)
        canvas.drawText("TOTAL", PAGE_WIDTH - 100f, startY + 20f, hText)

        var currentY = startY + rowHeight
        invoice.products.forEach { product ->
            canvas.drawText("${product.name} - ${Formatter.formatWeight(product.quantity)} KG @ ₹${product.rate}", 50f, currentY + 20f, normalPaint)
            canvas.drawText("₹${Formatter.formatCurrency(product.amount)}", PAGE_WIDTH - 50f, currentY + 20f, Paint(normalPaint).apply { textAlign = Paint.Align.RIGHT })
            currentY += rowHeight
        }
        
        val grandY = 650f
        canvas.drawRect(40f, grandY, PAGE_WIDTH - 40f, grandY + 40f, Paint().apply { color = accent; alpha = 15 })
        val grandPaint = Paint(headerPaint).apply { color = accent; textSize = 18f; textAlign = Paint.Align.RIGHT }
        canvas.drawText("NET PAYABLE: ₹${Formatter.formatCurrency(invoice.grandTotal)}", PAGE_WIDTH - 60f, grandY + 28f, grandPaint)
    }
}
