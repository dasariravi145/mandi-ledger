package com.dasariravi145.agrolynch.util

import android.graphics.*
import timber.log.Timber

object ImagePreprocessor {

    data class QualityResult(
        val isClear: Boolean,
        val message: String? = null,
        val brightness: Float = 0f,
        val blurScore: Float = 0f
    )

    fun preprocess(bitmap: Bitmap, rotationDegrees: Int = 0): Bitmap {
        Timber.d("PREPROCESS: Original size: ${bitmap.width}x${bitmap.height}, rotation: $rotationDegrees")
        
        var processed = rotateBitmap(bitmap, rotationDegrees.toFloat())
        
        // Enhance for OCR
        processed = adjustContrastAndBrightness(processed, 1.3f, 10f)
        
        // Resize to a standard width for consistency while maintaining aspect ratio
        processed = resizeToWidth(processed, 1200)
        
        Timber.d("PREPROCESS: Final size: ${processed.width}x${processed.height}")
        return processed
    }

    fun checkQuality(bitmap: Bitmap): QualityResult {
        val brightness = calculateBrightness(bitmap)
        val blurScore = calculateBlurScore(bitmap)
        
        Timber.d("QUALITY_CHECK: Brightness: $brightness, Blur: $blurScore")

        return when {
            brightness < 40 -> QualityResult(false, "Image is too dark. Please use better lighting.", brightness, blurScore)
            brightness > 240 -> QualityResult(false, "Image is too bright/washed out.", brightness, blurScore)
            blurScore < 5.0 -> QualityResult(false, "Image is blurry. Please hold the phone steady.", brightness, blurScore)
            else -> QualityResult(true, null, brightness, blurScore)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun adjustContrastAndBrightness(bitmap: Bitmap, contrast: Float, brightness: Float): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        val ret = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(ret)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return ret
    }

    private fun resizeToWidth(bitmap: Bitmap, targetWidth: Int): Bitmap {
        if (bitmap.width <= targetWidth) return bitmap
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (targetWidth * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun calculateBrightness(bitmap: Bitmap): Float {
        // Sample pixels for performance
        var totalBrightness = 0f
        val step = 10
        var count = 0
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Standard formula for perceived brightness
                totalBrightness += (0.299f * r + 0.587f * g + 0.114f * b)
                count++
            }
        }
        return if (count > 0) totalBrightness / count else 0f
    }

    private fun calculateBlurScore(bitmap: Bitmap): Float {
        // Simplified Laplacian variance equivalent for blur detection
        // We look at the difference between adjacent pixels
        var totalDiff = 0f
        val step = 10
        var count = 0
        for (y in 0 until bitmap.height - step step step) {
            for (x in 0 until bitmap.width - step step step) {
                val p1 = bitmap.getPixel(x, y)
                val p2 = bitmap.getPixel(x + step, y)
                val lum1 = (Color.red(p1) + Color.green(p1) + Color.blue(p1)) / 3f
                val lum2 = (Color.red(p2) + Color.green(p2) + Color.blue(p2)) / 3f
                totalDiff += Math.abs(lum1 - lum2)
                count++
            }
        }
        return if (count > 0) totalDiff / count else 0f
    }
}
