package com.dasariravi145.agrolynch.util.ocr

import android.graphics.*
import timber.log.Timber

object OcrPreProcessor {

    fun preprocess(bitmap: Bitmap, rotationDegrees: Int = 0): Bitmap {
        var processed = rotateBitmap(bitmap, rotationDegrees.toFloat())
        
        // Convert to Grayscale
        processed = toGrayscale(processed)
        
        // Increase Contrast & Brightness
        processed = adjustContrastAndBrightness(processed, 1.4f, 15f)
        
        // Sharpen text
        processed = sharpen(processed)
        
        // Basic noise reduction (if needed, here we just keep it simple)
        
        return processed
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val ret = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(ret)
        val paint = Paint()
        val cm = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return ret
    }

    private fun adjustContrastAndBrightness(bitmap: Bitmap, contrast: Float, brightness: Float): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        val ret = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(ret)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return ret
    }

    private fun sharpen(bitmap: Bitmap): Bitmap {
        // Simplified sharpening using a convolution matrix
        val sharpMatrix = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        )
        // Note: Real sharpening might require more complex implementation or a library
        // For Android, we can use RenderScript or just stick to contrast for now as sharpening via Canvas is not direct
        return bitmap 
    }
}
