package com.dasariravi145.agrolynch.ui.screens.receipt

import android.content.Context
import androidx.lifecycle.ViewModel
import com.dasariravi145.agrolynch.domain.model.ReceiptData
import com.dasariravi145.agrolynch.util.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReceiptViewModel @Inject constructor() : ViewModel() {

    fun generateAndShare(context: Context, data: ReceiptData) {
        val file = PdfGenerator.generateReceiptPdf(context, data)
        file?.let {
            PdfGenerator.sharePdf(context, it)
        }
    }
}
