package com.dasariravi145.agrolynch.util.pdf.renderer

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.dasariravi145.agrolynch.util.Formatter

class PremiumFruitRenderer : BaseInvoiceRenderer() {
    override fun render(pdfDocument: PdfDocument, profile: BusinessProfile, invoice: InvoiceData) {
        val page = startPage(pdfDocument)
        val canvas = page.canvas
        
        val purpleColor = Color.parseColor("#6A1B9A")
        val greenColor = Color.parseColor("#2E7D32")

        // 1. Floral/Fruit Decorative Border (Simplified)
        val borderPaint = Paint().apply {
            color = greenColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(20f, 20f, PAGE_WIDTH - 20f, PAGE_HEIGHT - 20f, borderPaint)

        // 2. Artistic Header
        val headerPaintTitle = Paint(headerPaint).apply { color = purpleColor; textSize = 28f }
        drawTextCentered(canvas, profile.companyName.uppercase(), 70f, headerPaintTitle)
        
        val subTextPaint = Paint(normalPaint).apply { textAlign = Paint.Align.CENTER; textSize = 10f }
        canvas.drawText(profile.address, PAGE_WIDTH / 2f, 95f, subTextPaint)
        canvas.drawText("Mob: ${profile.mobile} | GST: ${profile.gstNumber}", PAGE_WIDTH / 2f, 115f, subTextPaint)

        // Decorative Icons (Placeholders for fruit icons)
        drawBitmapSafe(canvas, profile.logoPath, RectF(40f, 40f, 100f, 100f))
        drawBitmapSafe(canvas, profile.godImagePath, RectF(PAGE_WIDTH - 100f, 40f, PAGE_WIDTH - 40f, 100f))

        // 3. Customer Info Section
        val infoY = 160f
        canvas.drawLine(40f, infoY, PAGE_WIDTH - 40f, infoY, borderPaint)
        
        drawField(canvas, "INVOICE", invoice.billNumber, 50f, infoY + 30f)
        drawField(canvas, "DATE", formatDate(invoice.date), 50f, infoY + 55f)
        
        val custPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT; color = purpleColor }
        canvas.drawText("CUSTOMER: ${invoice.customerName}", PAGE_WIDTH - 50f, infoY + 30f, custPaint)
        canvas.drawText("MOB: ${invoice.customerMobile}", PAGE_WIDTH - 50f, infoY + 55f, Paint(normalPaint).apply { textAlign = Paint.Align.RIGHT })

        // 4. Elegant Table
        renderPremiumTable(canvas, invoice, purpleColor)

        // 5. Bottom
        val footY = 740f
        drawBitmapSafe(canvas, profile.qrPath, RectF(PAGE_WIDTH / 2f - 40f, footY, PAGE_WIDTH / 2f + 40f, footY + 80f))
        
        pdfDocument.finishPage(page)
    }

    private fun renderPremiumTable(canvas: Canvas, invoice: InvoiceData, accent: Int) {
        val startY = 250f
        val rowHeight = 35f
        
        val headPaint = Paint().apply { color = accent; alpha = 30 }
        canvas.drawRoundRect(40f, startY, PAGE_WIDTH - 40f, startY + rowHeight, 15f, 15f, headPaint)
        
        val headText = Paint(boldPaint).apply { color = accent; textSize = 11f }
        canvas.drawText("ITEM DESCRIPTION", 60f, startY + 22f, headText)
        canvas.drawText("AMOUNT", PAGE_WIDTH - 120f, startY + 22f, headText)

        var currentY = startY + rowHeight + 10f
        invoice.products.forEach { product ->
            canvas.drawText("${product.name} (${product.grade})", 60f, currentY + 20f, normalPaint)
            canvas.drawText("₹${Formatter.formatCurrency(product.amount)}", PAGE_WIDTH - 60f, currentY + 20f, Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT })
            currentY += rowHeight
        }

        val grandY = 700f
        val grandPaint = Paint(headerPaint).apply { color = accent; textSize = 22f; textAlign = Paint.Align.RIGHT }
        canvas.drawText("Total Payable: ₹${Formatter.formatCurrency(invoice.grandTotal)}", PAGE_WIDTH - 40f, grandY, grandPaint)
    }
}
