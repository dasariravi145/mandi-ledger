package com.dasariravi145.agrolynch.ui.screens.scanner

import android.graphics.*

object OcrImageProcessor {

    fun preprocessImage(bitmap: Bitmap): Bitmap {
        // 1. Crop document (In a real app, we'd use contour detection. 
        // For this task, we'll assume the user aligned it or do a simple auto-crop if possible.)
        
        // 2. Increase Contrast and Brightness (Remove shadows)
        val contrast = 1.5f
        val brightness = 10f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        
        val enhancedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhancedBitmap)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        // 3. Grayscale (Improves OCR for handwritten text)
        return toGrayscale(enhancedBitmap)
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
    
    // Straighten image would involve Hough Transform, usually done via OpenCV. 
    // Since we don't have OpenCV, we rely on standard android.graphics for basic enhancements.
}
