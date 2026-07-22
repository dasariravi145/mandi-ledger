package com.dasariravi145.agrolynch.util

object PhoneNumberUtils {
    /**
     * Normalizes a phone number by removing all non-digit characters.
     * If the number has 10 digits, it's considered a standard local number.
     * If it has more, it might include a country code.
     */
    fun normalize(phoneNumber: String?): String {
        if (phoneNumber == null) return ""
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        // If it starts with 91 and has 12 digits, we might want to keep it that way or strip it.
        // For simple local matching, let's keep the last 10 digits if it's longer than 10.
        return if (digitsOnly.length >= 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }

    /**
     * Checks if two phone numbers are the same after normalization.
     * Returns false if both are empty.
     */
    fun isSame(phone1: String?, phone2: String?): Boolean {
        val n1 = normalize(phone1)
        val n2 = normalize(phone2)
        if (n1.isEmpty() || n2.isEmpty()) return false
        return n1 == n2
    }
}
