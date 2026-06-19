package com.dasariravi145.agrolynch.util.pdf.renderer

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.dasariravi145.agrolynch.util.Formatter
import java.text.SimpleDateFormat
import java.util.*

abstract class BaseInvoiceRenderer : InvoiceRenderer {
    protected val PAGE_WIDTH = 595 // A4 at 72 DPI
    protected val PAGE_HEIGHT = 842
    protected val MARGIN = 40f
    
    protected val normalPaint = Paint().apply { textSize = 10f; color = Color.BLACK; isAntiAlias = true }
    protected val boldPaint = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK; isAntiAlias = true }
    protected val headerPaint = Paint().apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK; isAntiAlias = true }

    protected fun formatDate(t: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t))

    protected fun drawBitmapSafe(canvas: Canvas, path: String?, rect: RectF) {
        if (path == null) return
        try {
            val bmp = BitmapFactory.decodeFile(path)
            if (bmp != null) {
                canvas.drawBitmap(bmp, null, rect, Paint().apply { isFilterBitmap = true })
            }
        } catch (e: Exception) {}
    }

    protected fun drawTextCentered(canvas: Canvas, text: String, y: Float, paint: Paint) {
        val x = (PAGE_WIDTH - paint.measureText(text)) / 2
        canvas.drawText(text, x, y, paint)
    }

    protected fun drawField(canvas: Canvas, label: String, value: String, x: Float, y: Float) {
        canvas.drawText("$label: ", x, y, boldPaint)
        canvas.drawText(value, x + boldPaint.measureText("$label: "), y, normalPaint)
    }

    protected fun startPage(pdfDocument: PdfDocument): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        return pdfDocument.startPage(pageInfo)
    }
}
