package com.dasariravi145.agrolynch.util

import android.graphics.RectF

data class TemplateLayoutSpec(
    val pageWidth: Float = 595f,
    val pageHeight: Float = 842f,
    val margin: Float = 18f,
    val headerRect: RectF = RectF(),
    val billInfoRect: RectF = RectF(),
    val tableRect: RectF = RectF(),
    val totalsRect: RectF = RectF(),
    val qrRect: RectF = RectF(),
    val signatureRect: RectF = RectF(),
    val footerRect: RectF = RectF(),
    val watermarkRect: RectF = RectF()
)
