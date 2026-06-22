package com.dasariravi145.agrolynch.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object PdfPrintHelper {
    fun print(activity: Activity, uri: Uri) {
        Log.d("PRINT_DEBUG", "activity=${activity::class.java.name}, uri=$uri")
        
        try {
            val printManager =
                activity.getSystemService(Context.PRINT_SERVICE) as PrintManager

            printManager.print(
                "Mandi Ledger Bill",
                PdfDocumentAdapter(activity, uri),
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
            )
            Log.i("PRINT_DEBUG", "PRINT_DIALOG_OPENED")
        } catch (e: Exception) {
            Log.e("PRINT_DEBUG", "PRINT_FAILED", e)
        }
    }
}
