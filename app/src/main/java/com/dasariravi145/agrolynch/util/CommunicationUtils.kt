package com.dasariravi145.agrolynch.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object CommunicationUtils {

    fun makeCall(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Remove spaces, special characters, and + if they exist
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        // If number is 10 digits, add India country code 91
        val finalNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
        
        val url = "https://wa.me/$finalNumber"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        
        // 1. Try normal WhatsApp
        try {
            val waIntent = Intent(intent).apply { setPackage("com.whatsapp") }
            context.startActivity(waIntent)
            return
        } catch (e: Exception) {
            // com.whatsapp not found
        }
        
        // 2. Try WhatsApp Business
        try {
            val w4bIntent = Intent(intent).apply { setPackage("com.whatsapp.w4b") }
            context.startActivity(w4bIntent)
            return
        } catch (e: Exception) {
            // com.whatsapp.w4b not found
        }
        
        // 3. Fallback to Browser / any other app that can handle wa.me
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open WhatsApp.", Toast.LENGTH_SHORT).show()
        }
    }
}
