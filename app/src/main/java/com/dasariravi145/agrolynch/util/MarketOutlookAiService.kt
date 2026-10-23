package com.dasariravi145.agrolynch.util

import com.dasariravi145.agrolynch.domain.model.ForecastDirection
import com.dasariravi145.agrolynch.domain.model.MarketForecastResult
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

object MarketOutlookAiService {

    private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"

    suspend fun generateExplanation(forecast: MarketForecastResult): String {
        if (forecast.direction == ForecastDirection.INSUFFICIENT_DATA) {
            return "Insufficient historical market observations available to generate an AI market summary."
        }

        val fallbackProse = buildRuleBasedExplanation(forecast)

        if (API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            return fallbackProse
        }

        return try {
            val model = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = API_KEY
            )

            val prompt = """
                You are an expert Mandi Market Analyst providing a short, clear, 3-sentence summary for a mandi business user.
                Convert the following VERIFIED statistical market analysis into a friendly, professional explanation.

                Context:
                - Product: ${forecast.productName} (${forecast.varietyName} ${forecast.gradeName})
                - Market: ${forecast.marketName}
                - Current Price: ₹${String.format(Locale.getDefault(), "%.1f", forecast.currentReferencePrice)}/KG
                - Forecast Direction: ${forecast.direction}
                - Expected Range Tomorrow: ₹${String.format(Locale.getDefault(), "%.1f", forecast.expectedLowTomorrow)} – ₹${String.format(Locale.getDefault(), "%.1f", forecast.expectedHighTomorrow)}/KG
                - Observed Signals: ${forecast.observedSignals.joinToString("; ")}
                - Advice: ${forecast.bestSellingWindowAdvice}

                RULES:
                1. DO NOT invent prices, dates, arrivals, or buyers. Use ONLY the numbers provided above.
                2. Keep the response to 3 short sentences.
                3. Be clear, practical, and helpful for mandi trade decisions.
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                model.generateContent(
                    content {
                        text(prompt)
                    }
                )
            }

            val text = response.text?.trim()
            if (!text.isNullOrBlank()) text else fallbackProse
        } catch (e: Exception) {
            Timber.e(e, "MARKET_OUTLOOK_AI_FAILED")
            fallbackProse
        }
    }

    private fun buildRuleBasedExplanation(forecast: MarketForecastResult): String {
        val directionText = when (forecast.direction) {
            ForecastDirection.LIKELY_UP -> "is showing an upward short-term signal"
            ForecastDirection.LIKELY_DOWN -> "is experiencing downward short-term price pressure"
            ForecastDirection.LIKELY_STABLE -> "is expected to remain steady"
            ForecastDirection.INSUFFICIENT_DATA -> "has insufficient market observations"
        }

        val signalsSummary = if (forecast.observedSignals.isNotEmpty()) {
            " Key observed factors include: " + forecast.observedSignals.joinToString(", ") + "."
        } else ""

        return "${forecast.productName} (${forecast.varietyName} ${forecast.gradeName}) in ${forecast.marketName} $directionText with an expected range of ₹${String.format(Locale.getDefault(), "%.1f", forecast.expectedLowTomorrow)}–₹${String.format(Locale.getDefault(), "%.1f", forecast.expectedHighTomorrow)}/kg tomorrow.$signalsSummary ${forecast.bestSellingWindowAdvice}"
    }
}
