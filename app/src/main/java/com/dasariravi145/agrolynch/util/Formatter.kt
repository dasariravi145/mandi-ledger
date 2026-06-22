package com.dasariravi145.agrolynch.util

import java.text.DecimalFormat
import java.util.*

object Formatter {
    private val weightFormat = DecimalFormat("0.###")
    private val currencyWholeFormat = DecimalFormat("#,##,##0")
    private val currencyDecimalFormat = DecimalFormat("#,##,##0.00")

    /**
     * Formats weight/quantity/percentage:
     * 500.0 -> 500
     * 500.25 -> 500.25
     * 1.960 -> 1.96
     */
    fun formatWeight(value: Double): String {
        val formatted = weightFormat.format(value)
        return if (formatted.contains(".")) {
            formatted.replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")
        } else {
            formatted
        }
    }

    /**
     * Formats quantity with unit:
     * 500, KG -> 500 KG
     * 1.96, Ton -> 1.96 Ton
     */
    fun formatQuantityDisplay(enteredQuantity: Double, selectedUnit: String): String {
        return "${formatWeight(enteredQuantity)} $selectedUnit"
    }

    /**
     * Formats net weight in KG
     */
    fun formatNetWeight(netWeightKg: Double): String {
        return "${formatWeight(netWeightKg)} KG"
    }

    /**
     * Formats rate per unit/KG
     */
    fun formatRate(rate: Double, unit: String = "KG"): String {
        return "₹${formatWeight(rate)}/$unit"
    }

    /**
     * Formats currency for summary/display:
     * 12500.0 -> 12,500
     * 12500.5 -> 12,500.50
     */
    fun formatCurrency(value: Double): String {
        return if (value % 1.0 == 0.0) {
            currencyWholeFormat.format(value)
        } else {
            currencyDecimalFormat.format(value)
        }
    }

    /**
     * Alias for formatCurrency as per requirements
     */
    fun formatAmount(amount: Double): String {
        return "₹${formatCurrency(amount)}"
    }

    /**
     * Formats currency strictly with 2 decimal places
     */
    fun formatCurrencyStrict(value: Double): String {
        return currencyDecimalFormat.format(value)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun numberToWords(n: Long): String {
        val units = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

        if (n == 0L) return "Zero"

        fun convert(number: Long): String {
            if (number < 20) return units[number.toInt()]
            if (number < 100) return tens[(number / 10).toInt()] + (if (number % 10 != 0L) " " + units[(number % 10).toInt()] else "")
            if (number < 1000) return units[(number / 100).toInt()] + " Hundred" + (if (number % 100 != 0L) " " + convert(number % 100) else "")
            if (number < 100000) return convert(number / 1000) + " Thousand" + (if (number % 1000 != 0L) " " + convert(number % 1000) else "")
            if (number < 10000000) return convert(number / 100000) + " Lakh" + (if (number % 100000 != 0L) " " + convert(number % 100000) else "")
            return convert(number / 10000000) + " Crore" + (if (number % 10000000 != 0L) " " + convert(number % 10000000) else "")
        }

        return convert(n) + " Only"
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
