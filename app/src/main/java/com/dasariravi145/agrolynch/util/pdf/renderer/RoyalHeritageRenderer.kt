package com.dasariravi145.agrolynch.util.pdf.renderer

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.dasariravi145.agrolynch.util.Formatter

class RoyalHeritageRenderer : BaseInvoiceRenderer() {
    override fun render(pdfDocument: PdfDocument, profile: BusinessProfile, invoice: InvoiceData) {
        val page = startPage(pdfDocument)
        val canvas = page.canvas
        
        val goldColor = Color.parseColor("#D4AF37")
        val maroonColor = Color.parseColor("#800000")

        // 1. Border (Ornate)
        val borderPaint = Paint().apply {
            color = goldColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(10f, 10f, PAGE_WIDTH - 10f, PAGE_HEIGHT - 10f, borderPaint)
        
        val innerBorder = Paint(borderPaint).apply { strokeWidth = 1f }
        canvas.drawRect(18f, 18f, PAGE_WIDTH - 18f, PAGE_HEIGHT - 18f, innerBorder)

        // 2. Header
        val headerBg = Paint().apply { color = maroonColor }
        canvas.drawRect(10f, 10f, PAGE_WIDTH - 10f, 130f, headerBg)

        // 3. Company Info
        val titlePaint = Paint(headerPaint).apply { color = goldColor }
        drawTextCentered(canvas, profile.companyName.uppercase(), 65f, titlePaint)
        
        val subPaint = Paint(normalPaint).apply { textSize = 10f; textAlign = Paint.Align.CENTER; color = Color.WHITE }
        canvas.drawText(profile.address, PAGE_WIDTH / 2f, 90f, subPaint)
        canvas.drawText("Mob: ${profile.mobile} | GST: ${profile.gstNumber}", PAGE_WIDTH / 2f, 110f, subPaint)

        // 4. Logo (Centered below header or in header)
        drawBitmapSafe(canvas, profile.logoPath, RectF(PAGE_WIDTH / 2f - 30f, 140f, PAGE_WIDTH / 2f + 30f, 200f))

        // 5. Customer Section
        val contentY = 230f
        val marPaint = Paint(boldPaint).apply { color = maroonColor }
        canvas.drawText("BILL NO: ${invoice.billNumber}", 40f, contentY, marPaint)
        canvas.drawText("DATE: ${formatDate(invoice.date)}", 40f, contentY + 20f, normalPaint)
        
        val rightPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT; color = maroonColor }
        canvas.drawText("TO: ${invoice.customerName.uppercase()}", PAGE_WIDTH - 40f, contentY, rightPaint)
        canvas.drawText("MOB: ${invoice.customerMobile}", PAGE_WIDTH - 40f, contentY + 20f, Paint(normalPaint).apply { textAlign = Paint.Align.RIGHT })

        // 6. Table
        renderRoyalTable(canvas, invoice, maroonColor, goldColor)

        // 7. Footer branding
        val footY = 750f
        drawBitmapSafe(canvas, profile.qrPath, RectF(PAGE_WIDTH / 2f - 40f, footY, PAGE_WIDTH / 2f + 40f, footY + 80f))
        
        pdfDocument.finishPage(page)
    }

    private fun renderRoyalTable(canvas: Canvas, invoice: InvoiceData, maroon: Int, gold: Int) {
        val startY = 300f
        val rowHeight = 30f
        
        val headPaint = Paint().apply { color = maroon }
        canvas.drawRect(40f, startY, PAGE_WIDTH - 40f, startY + rowHeight, headPaint)
        
        val whiteHead = Paint(boldPaint).apply { color = Color.WHITE; textSize = 11f }
        canvas.drawText("PRODUCT DESCRIPTION", 50f, startY + 20f, whiteHead)
        canvas.drawText("AMOUNT", PAGE_WIDTH - 120f, startY + 20f, whiteHead)

        var currentY = startY + rowHeight
        val linePaint = Paint().apply { color = gold; strokeWidth = 0.5f }
        
        invoice.products.forEach { product ->
            canvas.drawText("${product.name} - ${product.quantity} ${product.grade}", 50f, currentY + 20f, normalPaint)
            canvas.drawText("₹${Formatter.formatCurrency(product.amount)}", PAGE_WIDTH - 50f, currentY + 20f, Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT })
            currentY += rowHeight
            canvas.drawLine(40f, currentY, PAGE_WIDTH - 40f, currentY, linePaint)
        }
        
        currentY += 20f
        val grandPaint = Paint(headerPaint).apply { textSize = 18f; color = maroon; textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL: ₹${Formatter.formatCurrency(invoice.grandTotal)}", PAGE_WIDTH - 40f, currentY + 30f, grandPaint)
    }
}
