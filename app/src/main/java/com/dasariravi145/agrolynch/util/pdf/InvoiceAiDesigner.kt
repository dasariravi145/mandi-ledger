package com.dasariravi145.agrolynch.util.pdf

import com.dasariravi145.agrolynch.domain.model.ElementLayout
import com.dasariravi145.agrolynch.domain.model.InvoiceWizardConfig
import com.dasariravi145.agrolynch.util.GeminiService
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

object InvoiceAiDesigner {

    // Preset Design Commands
    fun applyPreset(currentConfig: InvoiceWizardConfig, presetName: String): InvoiceWizardConfig {
        val template = currentConfig.template
        val newLayouts = currentConfig.layouts.toMutableMap()

        when (presetName.uppercase()) {
            "PROFESSIONAL" -> {
                newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] = (newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] ?: ElementLayout()).copy(
                    fontWeight = "BOLD", alignment = "CENTER", textColorOption = "TEMPLATE"
                )
                newLayouts[InvoiceWizardConfig.KEY_ADDRESS] = (newLayouts[InvoiceWizardConfig.KEY_ADDRESS] ?: ElementLayout()).copy(
                    alignment = "LEFT", fontSize = 12f
                )
                newLayouts[InvoiceWizardConfig.KEY_PHONE] = (newLayouts[InvoiceWizardConfig.KEY_PHONE] ?: ElementLayout()).copy(
                    alignment = "LEFT", fontSize = 12f
                )
                return currentConfig.copy(
                    layouts = newLayouts,
                    pageBackgroundOption = "DEFAULT",
                    pageBorderThickness = "THIN",
                    qrSizeOption = "MEDIUM",
                    showQrLabel = true,
                    qrLabelStyle = "BOLD"
                )
            }
            "MINIMAL" -> {
                newLayouts[InvoiceWizardConfig.KEY_GOD_IMAGE] = (newLayouts[InvoiceWizardConfig.KEY_GOD_IMAGE] ?: ElementLayout()).copy(visible = false)
                newLayouts[InvoiceWizardConfig.KEY_WATERMARK] = (newLayouts[InvoiceWizardConfig.KEY_WATERMARK] ?: ElementLayout()).copy(visible = false)
                newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] = (newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] ?: ElementLayout()).copy(
                    fontWeight = "BOLD", alignment = "LEFT"
                )
                return currentConfig.copy(
                    layouts = newLayouts,
                    pageBackgroundOption = "WHITE",
                    pageBorderThickness = "NONE",
                    qrSizeOption = "SMALL",
                    showQrLabel = false
                )
            }
            "PREMIUM" -> {
                newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] = (newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] ?: ElementLayout()).copy(
                    fontWeight = "BOLD", alignment = "CENTER", fontSize = 48f, textColorOption = "TEMPLATE"
                )
                newLayouts[InvoiceWizardConfig.KEY_LOGO] = (newLayouts[InvoiceWizardConfig.KEY_LOGO] ?: ElementLayout()).copy(
                    shape = "ROUNDED", visible = true
                )
                return currentConfig.copy(
                    layouts = newLayouts,
                    pageBackgroundOption = "SOFT_TINT",
                    pageBorderThickness = "MEDIUM",
                    pageCornerStyle = "SLIGHTLY_ROUNDED",
                    qrSizeOption = "LARGE",
                    showQrLabel = true,
                    qrLabelStyle = "BOLD",
                    qrLabelColorOption = "TEMPLATE"
                )
            }
            "TRADITIONAL MANDI" -> {
                newLayouts[InvoiceWizardConfig.KEY_GOD_IMAGE] = (newLayouts[InvoiceWizardConfig.KEY_GOD_IMAGE] ?: ElementLayout()).copy(
                    visible = true, shape = "CIRCLE"
                )
                newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] = (newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] ?: ElementLayout()).copy(
                    alignment = "CENTER", fontWeight = "BOLD"
                )
                return currentConfig.copy(
                    layouts = newLayouts,
                    pageBackgroundOption = "DEFAULT",
                    pageBorderThickness = "MEDIUM",
                    showQrLabel = true
                )
            }
            "COMPACT" -> {
                newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] = (newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] ?: ElementLayout()).copy(
                    fontSize = 36f, alignment = "LEFT"
                )
                return currentConfig.copy(
                    layouts = newLayouts,
                    qrSizeOption = "SMALL",
                    pageBorderThickness = "NONE"
                )
            }
            else -> return currentConfig
        }
    }

    suspend fun processUserRequest(
        currentConfig: InvoiceWizardConfig,
        userPrompt: String
    ): Pair<InvoiceWizardConfig, String> {
        val trimmedPrompt = userPrompt.trim()

        // Local Preset Match Check First (Cost & Speed Optimization)
        listOf("PROFESSIONAL", "MINIMAL", "PREMIUM", "TRADITIONAL MANDI", "COMPACT").forEach { preset ->
            if (trimmedPrompt.contains(preset, ignoreCase = true)) {
                val updated = applyPreset(currentConfig, preset)
                return Pair(updated, "Applied $preset design style to your invoice layout.")
            }
        }

        // Fast Local Intent Rules for simple commands
        val lowerPrompt = trimmedPrompt.lowercase()
        val localResult = tryLocalRuleMatch(currentConfig, lowerPrompt)
        if (localResult != null) {
            return localResult
        }

        // Call Gemini AI for complex natural language requests
        return try {
            val aiModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = "YOUR_GEMINI_API_KEY_HERE"
            )

            val systemPrompt = """
                You are an expert Mandi Ledger Invoice Designer AI assistant.
                The user wants to customize their invoice layout via natural language (English, Hindi, or Telugu-English).
                You MUST return ONLY a JSON object with structured commands. DO NOT RETURN ANY MARKDOWN OR TEXT OUTSIDE THE JSON.

                Available Element Keys:
                - SHOP_NAME
                - TAGLINE
                - ADDRESS
                - PHONE
                - GSTIN
                - LOGO
                - GOD_IMAGE
                - QR_CODE
                - SIGNATURE_BLOCK
                - STAMP
                - PRODUCT_TABLE
                - TOTALS_BOX
                - THANK_YOU
                - FOOTER
                - WATERMARK

                Allowed Command Types:
                1. "MOVE_ELEMENT": {"action": "MOVE_ELEMENT", "key": "QR_CODE", "xPercent": 70, "yPercent": 80}
                2. "RESIZE_ELEMENT": {"action": "RESIZE_ELEMENT", "key": "LOGO", "widthPercent": 25, "heightPercent": 15}
                3. "SET_VISIBILITY": {"action": "SET_VISIBILITY", "key": "GOD_IMAGE", "visible": false}
                4. "SET_STYLE": {"action": "SET_STYLE", "key": "SHOP_NAME", "fontWeight": "BOLD", "textColorOption": "TEMPLATE", "alignment": "CENTER"}
                5. "SET_QR_SETTING": {"action": "SET_QR_SETTING", "qrSizeOption": "LARGE", "showQrLabel": true}
                6. "SET_PAGE_STYLE": {"action": "SET_PAGE_STYLE", "pageBackgroundOption": "SOFT_TINT", "pageBorderThickness": "MEDIUM"}

                Respond with JSON format:
                {
                    "reply": "Short friendly explanation of changes made.",
                    "commands": [ ... list of commands ... ]
                }
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                aiModel.generateContent(
                    content {
                        text(systemPrompt)
                        text("Current Config Template: ${currentConfig.template}")
                        text("User Prompt: $userPrompt")
                    }
                )
            }

            val rawText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: ""
            Timber.d("AI_DESIGNER_RESPONSE: $rawText")

            if (rawText.isBlank()) {
                return Pair(currentConfig, "AI could not process this design request. No changes were made.")
            }

            val json = JSONObject(rawText)
            val reply = json.optString("reply", "Applied your requested design changes.")
            val commands = json.optJSONArray("commands") ?: JSONArray()

            var updatedConfig = currentConfig
            for (i in 0 until commands.length()) {
                val cmd = commands.getJSONObject(i)
                updatedConfig = applyCommand(updatedConfig, cmd)
            }

            Pair(updatedConfig, reply)
        } catch (e: Exception) {
            Timber.e(e, "AI_DESIGNER_ERROR")
            Pair(currentConfig, "AI design service is currently offline. Your design remains unchanged.")
        }
    }

    private fun tryLocalRuleMatch(config: InvoiceWizardConfig, prompt: String): Pair<InvoiceWizardConfig, String>? {
        val newLayouts = config.layouts.toMutableMap()

        if (prompt.contains("move qr") || prompt.contains("qr ni") || prompt.contains("qr bottom")) {
            val qr = config.getLayout(InvoiceWizardConfig.KEY_QR_CODE).copy(
                xPercent = if (prompt.contains("right") || prompt.contains("lo")) 75f else 5f,
                yPercent = 75f,
                visible = true
            )
            newLayouts[InvoiceWizardConfig.KEY_QR_CODE] = qr
            return Pair(config.copy(layouts = newLayouts), "Moved QR code to requested position.")
        }

        if (prompt.contains("bigger shop name") || prompt.contains("pedda") || prompt.contains("large title")) {
            val shop = config.getLayout(InvoiceWizardConfig.KEY_SHOP_NAME).copy(
                fontSize = 50f, fontWeight = "BOLD"
            )
            newLayouts[InvoiceWizardConfig.KEY_SHOP_NAME] = shop
            return Pair(config.copy(layouts = newLayouts), "Increased shop name font size.")
        }

        if (prompt.contains("signature bottom") || prompt.contains("signature total")) {
            val sig = config.getLayout(InvoiceWizardConfig.KEY_SIGNATURE_BLOCK).copy(
                xPercent = 70f, yPercent = 88f, visible = true
            )
            newLayouts[InvoiceWizardConfig.KEY_SIGNATURE_BLOCK] = sig
            return Pair(config.copy(layouts = newLayouts), "Adjusted signature position below totals.")
        }

        if (prompt.contains("hide god") || prompt.contains("god image hide") || prompt.contains("remove god")) {
            val god = config.getLayout(InvoiceWizardConfig.KEY_GOD_IMAGE).copy(visible = false)
            newLayouts[InvoiceWizardConfig.KEY_GOD_IMAGE] = god
            return Pair(config.copy(layouts = newLayouts), "Hidden god image from header.")
        }

        return null
    }

    private fun applyCommand(config: InvoiceWizardConfig, cmd: JSONObject): InvoiceWizardConfig {
        val action = cmd.optString("action", "")
        val key = cmd.optString("key", "")
        val newLayouts = config.layouts.toMutableMap()
        val currentLayout = if (key.isNotEmpty()) config.getLayout(key) else ElementLayout()

        when (action) {
            "MOVE_ELEMENT" -> {
                val x = cmd.optDouble("xPercent", currentLayout.xPercent.toDouble()).toFloat().coerceIn(0f, 95f)
                val y = cmd.optDouble("yPercent", currentLayout.yPercent.toDouble()).toFloat().coerceIn(0f, 95f)
                newLayouts[key] = currentLayout.copy(xPercent = x, yPercent = y)
                return config.copy(layouts = newLayouts)
            }
            "RESIZE_ELEMENT" -> {
                val w = cmd.optDouble("widthPercent", currentLayout.widthPercent.toDouble()).toFloat().coerceIn(5f, 100f)
                val h = cmd.optDouble("heightPercent", currentLayout.heightPercent.toDouble()).toFloat().coerceIn(2f, 100f)
                newLayouts[key] = currentLayout.copy(widthPercent = w, heightPercent = h)
                return config.copy(layouts = newLayouts)
            }
            "SET_VISIBILITY" -> {
                val vis = cmd.optBoolean("visible", true)
                newLayouts[key] = currentLayout.copy(visible = vis)
                return config.copy(layouts = newLayouts)
            }
            "SET_STYLE" -> {
                val fw = cmd.optString("fontWeight", currentLayout.fontWeight)
                val align = cmd.optString("alignment", currentLayout.alignment)
                val tc = cmd.optString("textColorOption", currentLayout.textColorOption)
                val fs = cmd.optDouble("fontSize", currentLayout.fontSize.toDouble()).toFloat()
                newLayouts[key] = currentLayout.copy(
                    fontWeight = fw, alignment = align, textColorOption = tc, fontSize = fs
                )
                return config.copy(layouts = newLayouts)
            }
            "SET_QR_SETTING" -> {
                val size = cmd.optString("qrSizeOption", config.qrSizeOption)
                val showLabel = cmd.optBoolean("showQrLabel", config.showQrLabel)
                return config.copy(qrSizeOption = size, showQrLabel = showLabel)
            }
            "SET_PAGE_STYLE" -> {
                val bg = cmd.optString("pageBackgroundOption", config.pageBackgroundOption)
                val border = cmd.optString("pageBorderThickness", config.pageBorderThickness)
                return config.copy(pageBackgroundOption = bg, pageBorderThickness = border)
            }
            else -> return config
        }
    }
}
