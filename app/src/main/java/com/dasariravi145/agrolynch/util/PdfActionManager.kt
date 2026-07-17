package com.dasariravi145.agrolynch.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import timber.log.Timber

object PdfActionManager {

    private var isPdfActionRunning = false
    private var selectedPdfUri: Uri? = null

    fun resetPdfActionState() {
        isPdfActionRunning = false
        selectedPdfUri = null
        Timber.d("PDF_ACTION_RESET")
    }

    fun openPdf(context: Context, pdfUri: Uri?) {
        Timber.d("PDF_ACTION_CLICKED")
        if (pdfUri == null) {
            Timber.e("PDF_ACTION_FAILED: URI NULL")
            Toast.makeText(context, "PDF not found. Please generate again.", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            selectedPdfUri = pdfUri
            Timber.d("PDF_URI_READY: $pdfUri")
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Timber.d("PDF_OPEN_LAUNCHED")
            val activity = context.findActivity()
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Timber.e(e, "PDF_ACTION_FAILED")
            Toast.makeText(context, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            resetPdfActionState()
        }
    }

    fun printPdf(activity: Activity, pdfUri: Uri?) {
        if (pdfUri == null) {
            Timber.e("PRINT_FAILED: URI NULL")
            Toast.makeText(activity, "PDF not found. Please generate again.", Toast.LENGTH_SHORT).show()
            return
        }
        PdfPrintHelper.print(activity, pdfUri)
    }

    fun sharePdf(context: Context, pdfUri: Uri?) {
        Timber.d("PDF_ACTION_CLICKED")
        if (pdfUri == null) {
            Timber.e("PDF_ACTION_FAILED: URI NULL")
            Toast.makeText(context, "PDF not found. Please generate again.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            selectedPdfUri = pdfUri
            Timber.d("PDF_URI_READY: $pdfUri")
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Timber.d("PDF_SHARE_LAUNCHED")
            val chooserIntent = Intent.createChooser(intent, "Share Bill")
            
            val activity = context.findActivity()
            if (activity != null) {
                activity.startActivity(chooserIntent)
            } else {
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            Timber.e(e, "PDF_ACTION_FAILED")
            Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            resetPdfActionState()
        }
    }
}
