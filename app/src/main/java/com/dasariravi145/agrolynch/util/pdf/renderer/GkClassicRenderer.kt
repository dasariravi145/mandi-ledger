package com.dasariravi145.agrolynch.util.pdf.renderer

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.dasariravi145.agrolynch.util.Formatter

class GkClassicRenderer : BaseInvoiceRenderer() {
    override fun render(pdfDocument: PdfDocument, profile: BusinessProfile, invoice: InvoiceData) {
        val page = startPage(pdfDocument)
        val canvas = page.canvas
        
        // 1. Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(10f, 10f, PAGE_WIDTH - 10f, PAGE_HEIGHT - 10f, borderPaint)

        // 2. Header Decorative
        val headerBg = Paint().apply { color = Color.parseColor("#E8F5E9") }
        canvas.drawRect(10f, 10f, PAGE_WIDTH - 10f, 120f, headerBg)

        // 3. Logo & God Image
        drawBitmapSafe(canvas, profile.logoPath, RectF(30f, 30f, 90f, 90f))
        drawBitmapSafe(canvas, profile.godImagePath, RectF(PAGE_WIDTH - 90f, 30f, PAGE_WIDTH - 30f, 90f))

        // 4. Company Info
        val titlePaint = Paint(headerPaint).apply { color = Color.parseColor("#1B5E20") }
        drawTextCentered(canvas, profile.companyName.uppercase(), 60f, titlePaint)
        
        val subPaint = Paint(normalPaint).apply { textSize = 9f; textAlign = Paint.Align.CENTER }
        canvas.drawText(profile.address, PAGE_WIDTH / 2f, 80f, subPaint)
        canvas.drawText("Mob: ${profile.mobile} | GST: ${profile.gstNumber}", PAGE_WIDTH / 2f, 95f, subPaint)
        
        if (profile.tagline.isNotEmpty()) {
            val tagPaint = Paint(normalPaint).apply { textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); textAlign = Paint.Align.CENTER }
            canvas.drawText("\"${profile.tagline}\"", PAGE_WIDTH / 2f, 110f, tagPaint)
        }

        // 5. Customer Section
        canvas.drawLine(10f, 120f, PAGE_WIDTH - 10f, 120f, borderPaint)
        drawField(canvas, "Bill No", invoice.billNumber, 30f, 145f)
        drawField(canvas, "Date", formatDate(invoice.date), 30f, 165f)
        
        drawField(canvas, "Customer", invoice.customerName, 350f, 145f)
        drawField(canvas, "Mobile", invoice.customerMobile, 350f, 165f)

        // 6. Item Table
        renderTable(canvas, invoice)

        // 7. Totals & Branding Bottom
        val totalY = 650f
        canvas.drawLine(350f, totalY, PAGE_WIDTH - 10f, totalY, borderPaint)
        
        val rightAlign = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        val labelX = 450f
        val valueX = PAGE_WIDTH - 30f
        
        var currentY = totalY + 20f
        fun drawTotalLine(label: String, value: Double) {
            canvas.drawText(label, labelX, currentY, normalPaint)
            canvas.drawText("₹${Formatter.formatCurrency(value)}", valueX, currentY, rightAlign)
            currentY += 20f
        }

        drawTotalLine("Sub Total:", invoice.subtotal)
        drawTotalLine("Commission:", invoice.commission)
        drawTotalLine("Transport:", invoice.transport)
        
        currentY += 10f
        val grandPaint = Paint(headerPaint).apply { textSize = 16f; color = Color.parseColor("#1B5E20"); textAlign = Paint.Align.RIGHT }
        canvas.drawText("GRAND TOTAL:", labelX, currentY, grandPaint)
        canvas.drawText("₹${Formatter.formatCurrency(invoice.grandTotal)}", valueX, currentY, grandPaint)

        // QR & Signature
        drawBitmapSafe(canvas, profile.qrPath, RectF(30f, 660f, 110f, 740f))
        canvas.drawText("SCAN TO PAY", 30f, 755f, Paint(boldPaint).apply { textSize = 8f })
        
        drawBitmapSafe(canvas, profile.signaturePath, RectF(PAGE_WIDTH - 150f, 740f, PAGE_WIDTH - 30f, 790f))
        canvas.drawText("Authorized Signature", PAGE_WIDTH - 150f, 805f, Paint(normalPaint).apply { textSize = 8f })

        pdfDocument.finishPage(page)
    }

    private fun renderTable(canvas: Canvas, invoice: InvoiceData) {
        val startY = 200f
        val rowHeight = 25f
        val tablePaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.LTGRAY }
        
        // Header
        val headerBg = Paint().apply { color = Color.parseColor("#F1F8E9") }
        canvas.drawRect(10f, startY, PAGE_WIDTH - 10f, startY + rowHeight, headerBg)
        
        val hPaint = Paint(boldPaint).apply { textSize = 10f }
        canvas.drawText("Sl", 20f, startY + 17f, hPaint)
        canvas.drawText("Description", 50f, startY + 17f, hPaint)
        canvas.drawText("Qty", 350f, startY + 17f, hPaint)
        canvas.drawText("Rate", 430f, startY + 17f, hPaint)
        canvas.drawText("Amount", 510f, startY + 17f, hPaint)

        var currentY = startY + rowHeight
        invoice.products.forEachIndexed { index, product ->
            canvas.drawText((index + 1).toString(), 20f, currentY + 17f, normalPaint)
            canvas.drawText("${product.name} (${product.grade})", 50f, currentY + 17f, normalPaint)
            canvas.drawText(Formatter.formatWeight(product.quantity), 350f, currentY + 17f, normalPaint)
            canvas.drawText(Formatter.formatWeight(product.rate), 430f, currentY + 17f, normalPaint)
            canvas.drawText(Formatter.formatCurrency(product.amount), 510f, currentY + 17f, boldPaint)
            currentY += rowHeight
            canvas.drawLine(10f, currentY, PAGE_WIDTH - 10f, currentY, tablePaint)
        }
    }
}
