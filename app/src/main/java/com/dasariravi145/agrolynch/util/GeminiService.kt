package com.dasariravi145.agrolynch.util

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

object GeminiService {
    // Replace with your actual API key or fetch from a secure location
    private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"
    
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY
    )

    suspend fun extractBillData(bitmap: Bitmap, target: String, mlKitData: ExtractedData? = null): ExtractedData? {
        if (API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            Timber.e("AI_VISION: API Key not set")
            return null
        }

        val prompt = getPromptForTarget(target)
        
        return try {
            val response = withContext(Dispatchers.IO) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }
            
            val jsonText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: ""
            Timber.d("AI_VISION: Raw Response: $jsonText")
            val parsed = parseGeminiJson(jsonText, target)
            
            // Carry over detected numbers/strings from ML Kit if available
            if (mlKitData != null) {
                parsed.copy(
                    detectedNumbers = mlKitData.detectedNumbers,
                    detectedStrings = mlKitData.detectedStrings
                )
            } else {
                parsed
            }
        } catch (e: Exception) {
            Timber.e(e, "AI_VISION: Gemini extraction failed")
            null
        }
    }

    private fun getPromptForTarget(target: String): String {
        val basePrompt = "Extract Mandi Ledger bill details from this image into structured JSON. This is often handwritten. If a field is handwritten or unclear, mark its key in the 'low_confidence_fields' list. Return ONLY JSON. "
        
        return when (target) {
            "STOCK_ENTRY" -> basePrompt + """
                Fields for Farmer Stock Bill:
                {
                    "billNumber": "string", "date": "DD/MM/YYYY", "farmerName": "string", "productName": "string",
                    "category": "Fruit/Vegetable", "grade": "string", "quantity": 0.0, "damageOrSoot": 0.0,
                    "rate": 0.0, // Rate per KG. If bill has Rate per Ton, divide by 1000.
                    "grossAmount": 0.0, "netAmount": 0.0, "low_confidence_fields": [], "confidence": 0.8
                }
                Focus on: Farmer Name, Product, Quantity, and Rate.
            """.trimIndent()
            "SALE_ENTRY" -> basePrompt + """
                Fields for Buyer Sale Bill:
                {
                    "billNumber": "string", "date": "DD/MM/YYYY", "buyerName": "string", "productName": "string",
                    "quantity": 0.0, "saleRate": 0.0, // Sale Rate per KG. If bill has Rate per Ton, divide by 1000.
                    "amount": 0.0, "low_confidence_fields": [], "confidence": 0.8
                }
                Focus on: Buyer Name, Product, Quantity, and Sale Rate.
            """.trimIndent()
            "PAYMENT" -> basePrompt + """
                Fields for Payment Receipt:
                {
                    "date": "DD/MM/YYYY", "partyName": "string", "amount": 0.0, "paymentMode": "CASH/UPI/CHEQUE",
                    "referenceNumber": "string", "low_confidence_fields": [], "confidence": 0.8
                }
            """.trimIndent()
            else -> basePrompt + "Extract all transactional details into a flat JSON object."
        }
    }

    private fun parseGeminiJson(jsonStr: String, target: String): ExtractedData {
        return try {
            val json = JSONObject(jsonStr)
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            
            val dateStr = json.optString("date", "")
            val dateLong = try { sdf.parse(dateStr)?.time ?: System.currentTimeMillis() } catch(e: Exception) { System.currentTimeMillis() }

            val lowConfList = json.optJSONArray("low_confidence_fields")
            val lowConfSet = mutableSetOf<String>()
            if (lowConfList != null) {
                for (i in 0 until lowConfList.length()) {
                    lowConfSet.add(lowConfList.getString(i))
                }
            }

            ExtractedData(
                billNumber = json.optString("billNumber", ""),
                date = dateLong,
                farmerName = json.optString("farmerName", ""),
                buyerName = json.optString("buyerName", ""),
                partyName = json.optString("partyName", json.optString("farmerName", json.optString("buyerName", ""))),
                productName = json.optString("productName", ""),
                category = json.optString("category", "General"),
                grade = json.optString("grade", ""),
                quantity = json.optDouble("quantity", 0.0),
                damageOrSoot = json.optDouble("damageOrSoot", 0.0),
                rate = json.optDouble("rate", json.optDouble("saleRate", 0.0)),
                grossAmount = json.optDouble("grossAmount", json.optDouble("amount", 0.0)),
                amount = json.optDouble("amount", 0.0),
                netAmount = json.optDouble("netAmount", 0.0),
                paymentMode = json.optString("paymentMode", ""),
                referenceNumber = json.optString("referenceNumber", ""),
                confidenceScore = json.optDouble("confidence", 0.0).toFloat() * 100f,
                lowConfidenceFields = lowConfSet,
                ocrText = "AI Response: " + json.toString(2)
            )
        } catch (e: Exception) {
            Timber.e(e, "AI_VISION: JSON parsing error")
            ExtractedData(ocrText = "Parsing Failed: $jsonStr")
        }
    }
}
