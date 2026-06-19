package com.dasariravi145.agrolynch.util.pdf.renderer

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.dasariravi145.agrolynch.util.Formatter

class DiamondEliteRenderer : BaseInvoiceRenderer() {
    override fun render(pdfDocument: PdfDocument, profile: BusinessProfile, invoice: InvoiceData) {
        val page = startPage(pdfDocument)
        val canvas = page.canvas
        
        val deepBlue = Color.parseColor("#1A237E")
        val accentGold = Color.parseColor("#FFD700")

        // 1. Sleek Background
        canvas.drawColor(Color.WHITE)
        
        // 2. Diamond Header
        val headerPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(PAGE_WIDTH.toFloat(), 0f)
            lineTo(PAGE_WIDTH.toFloat(), 150f)
            lineTo(0f, 180f)
            close()
        }
        canvas.drawPath(headerPath, Paint().apply { color = deepBlue })

        // 3. Company Branding
        val titlePaint = Paint(headerPaint).apply { color = Color.WHITE; textSize = 26f }
        canvas.drawText(profile.companyName.uppercase(), 40f, 60f, titlePaint)
        
        val subPaint = Paint(normalPaint).apply { color = Color.LTGRAY; textSize = 10f }
        canvas.drawText(profile.address, 40f, 85f, subPaint)
        canvas.drawText("Mob: ${profile.mobile} | GST: ${profile.gstNumber}", 40f, 105f, subPaint)

        drawBitmapSafe(canvas, profile.logoPath, RectF(PAGE_WIDTH - 120f, 30f, PAGE_WIDTH - 40f, 110f))

        // 4. Invoice Info Box
        val infoPaint = Paint().apply { color = Color.parseColor("#F5F5F5") }
        canvas.drawRoundRect(40f, 200f, PAGE_WIDTH - 40f, 270f, 10f, 10f, infoPaint)
        
        drawField(canvas, "BILL NO", invoice.billNumber, 60f, 230f)
        drawField(canvas, "DATE", formatDate(invoice.date), 60f, 250f)
        
        val rightPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT; color = deepBlue }
        canvas.drawText("FOR: ${invoice.customerName.uppercase()}", PAGE_WIDTH - 60f, 230f, rightPaint)
        canvas.drawText("MOB: ${invoice.customerMobile}", PAGE_WIDTH - 60f, 250f, Paint(normalPaint).apply { textAlign = Paint.Align.RIGHT })

        // 5. Modern Table
        renderDiamondTable(canvas, invoice, deepBlue)

        // 6. Modern Footer
        val footY = 750f
        drawBitmapSafe(canvas, profile.qrPath, RectF(40f, footY, 120f, footY + 80f))
        
        val signRect = RectF(PAGE_WIDTH - 160f, footY, PAGE_WIDTH - 40f, footY + 50f)
        drawBitmapSafe(canvas, profile.signaturePath, signRect)
        canvas.drawText("Authorized Signatory", PAGE_WIDTH - 160f, footY + 65f, Paint(normalPaint).apply { textSize = 9f })

        pdfDocument.finishPage(page)
    }

    private fun renderDiamondTable(canvas: Canvas, invoice: InvoiceData, primary: Int) {
        val startY = 300f
        val rowHeight = 30f
        
        val headPaint = Paint().apply { color = primary }
        canvas.drawRect(40f, startY, PAGE_WIDTH - 40f, startY + rowHeight, headPaint)
        
        val whiteHead = Paint(boldPaint).apply { color = Color.WHITE; textSize = 10f }
        canvas.drawText("ITEM DESCRIPTION", 55f, startY + 20f, whiteHead)
        canvas.drawText("QTY", 300f, startY + 20f, whiteHead)
        canvas.drawText("RATE", 400f, startY + 20f, whiteHead)
        canvas.drawText("AMOUNT", PAGE_WIDTH - 100f, startY + 20f, whiteHead)

        var currentY = startY + rowHeight
        val linePaint = Paint().apply { color = Color.parseColor("#EEEEEE"); strokeWidth = 1f }
        
        invoice.products.forEach { product ->
            canvas.drawText("${product.name} (${product.grade})", 55f, currentY + 20f, normalPaint)
            canvas.drawText(Formatter.formatWeight(product.quantity), 300f, currentY + 20f, normalPaint)
            canvas.drawText(Formatter.formatWeight(product.rate), 400f, currentY + 20f, normalPaint)
            canvas.drawText(Formatter.formatCurrency(product.amount), PAGE_WIDTH - 50f, currentY + 20f, Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT })
            currentY += rowHeight
            canvas.drawLine(40f, currentY, PAGE_WIDTH - 40f, currentY, linePaint)
        }
        
        currentY += 40f
        val grandPaint = Paint(headerPaint).apply { textSize = 20f; color = primary; textAlign = Paint.Align.RIGHT }
        canvas.drawText("GRAND TOTAL", PAGE_WIDTH - 150f, currentY, Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 12f })
        canvas.drawText("₹${Formatter.formatCurrency(invoice.grandTotal)}", PAGE_WIDTH - 40f, currentY, grandPaint)
    }
}
