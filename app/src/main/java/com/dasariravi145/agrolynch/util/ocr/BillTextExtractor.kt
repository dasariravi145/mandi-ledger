package com.dasariravi145.agrolynch.util.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object BillTextExtractor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(bitmap: Bitmap): Text? {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            recognizer.process(image).await()
        } catch (e: Exception) {
            null
        }
    }
}
