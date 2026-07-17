package com.dasariravi145.agrolynch.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dasariravi145.agrolynch.util.findActivity

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
            val activity = context.findActivity()
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
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
        
        val activity = context.findActivity()

        // 1. Try normal WhatsApp
        try {
            val waIntent = Intent(intent).apply { setPackage("com.whatsapp") }
            if (activity != null) {
                activity.startActivity(waIntent)
            } else {
                waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(waIntent)
            }
            return
        } catch (e: Exception) {
            // com.whatsapp not found
        }
        
        // 2. Try WhatsApp Business
        try {
            val w4bIntent = Intent(intent).apply { setPackage("com.whatsapp.w4b") }
            if (activity != null) {
                activity.startActivity(w4bIntent)
            } else {
                w4bIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(w4bIntent)
            }
            return
        } catch (e: Exception) {
            // com.whatsapp.w4b not found
        }
        
        // 3. Fallback to Browser / any other app that can handle wa.me
        try {
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open WhatsApp.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFileToWhatsApp(context: Context, fileUri: Uri, phoneNumber: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (!phoneNumber.isNullOrBlank()) {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
            val finalNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
            intent.putExtra("jid", "$finalNumber@s.whatsapp.net")
        }

        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        var started = false
        for (pkg in packages) {
            try {
                val pkgIntent = Intent(intent).apply { setPackage(pkg) }
                pkgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(pkgIntent)
                started = true
                break
            } catch (e: Exception) {
                // Try next package
            }
        }

        if (!started) {
            Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show()
        }
    }
}
